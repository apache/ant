/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Locale;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.DocumentHandler;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.XMLReaderAdapter;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Configures a project (complete with targets and tasks) based on
 * an XML build file.
 *
 * @author duncan@x180.com
 */

public class ProjectHelper {

    /** 
     * Parser factory to use to create parsers.
     * @see #getParserFactory
     */
    private static SAXParserFactory parserFactory = null;

    /**
     * SAX 1 style parser used to parse the given file. This may 
     * in fact be a SAX 2 XMLReader wrapped in an XMLReaderAdapter.
     */
    private org.xml.sax.Parser parser;
    
    /** The project to configure. */
    private Project project;
    /** The configuration file to parse. */
    private File buildFile;
    /** 
     * Parent directory of the build file. Used for resolving entities
     * and setting the project's base directory.
     */
    private File buildFileParent;
    /** 
     * Locator for the configuration file parser. 
     * Used for giving locations of errors etc.
     */
    private Locator locator;

    /**
     * Configures the project with the contents of the specified XML file.
     * 
     * @param project The project to configure. Must not be <code>null</code>.
     * @param buildFile An XML file giving the project's configuration.
     *                  Must not be <code>null</code>.
     * 
     * @exception BuildException if the configuration is invalid or cannot 
     *                           be read
     */
    public static void configureProject(Project project, File buildFile) throws BuildException {
        new ProjectHelper(project, buildFile).parse();
    }

    /**
     * Constructs a new helper for the specified project and XML file.
     * 
     * @param project The project for the resulting ProjectHelper to configure. 
     *                Must not be <code>null</code>.
     * @param buildFile An XML file giving the project's configuration.
     *                  Must not be <code>null</code>.
     */
    private ProjectHelper(Project project, File buildFile) {
        this.project = project;
        this.buildFile = new File(buildFile.getAbsolutePath());
        buildFileParent = new File(this.buildFile.getParent());
    }

    /**
     * Parses the project file, configuring the project as it goes.
     * 
     * @exception BuildException if the configuration is invalid or cannot 
     *                           be read
     */
    private void parse() throws BuildException {
        FileInputStream inputStream = null;
        InputSource inputSource = null;
        
        try {
            SAXParser saxParser = getParserFactory().newSAXParser();
            try {
                parser = saxParser.getParser();
            } catch (SAXException exc) {
                parser = new XMLReaderAdapter(saxParser.getXMLReader());
            }

            String uri = "file:" + buildFile.getAbsolutePath().replace('\\', '/');
            for (int index = uri.indexOf('#'); index != -1; index = uri.indexOf('#')) {
                uri = uri.substring(0, index) + "%23" + uri.substring(index+1);
            }
            
            inputStream = new FileInputStream(buildFile);
            inputSource = new InputSource(inputStream);
            inputSource.setSystemId(uri);
            project.log("parsing buildfile " + buildFile + " with URI = " + uri, Project.MSG_VERBOSE);
            HandlerBase hb = new RootHandler();
            parser.setDocumentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
            parser.parse(inputSource);
        }
        catch(ParserConfigurationException exc) {
            throw new BuildException("Parser has not been configured correctly", exc);
        }
        catch(SAXParseException exc) {
            Location location =
                new Location(buildFile.toString(), exc.getLineNumber(), exc.getColumnNumber());

            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                BuildException be = (BuildException) t;
                if (be.getLocation() == Location.UNKNOWN_LOCATION) {
                    be.setLocation(location);
                }
                throw be;
            }
            
            throw new BuildException(exc.getMessage(), t, location);
        }
        catch(SAXException exc) {
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(exc.getMessage(), t);
        }
        catch(FileNotFoundException exc) {
            throw new BuildException(exc);
        }
        catch(IOException exc) {
            throw new BuildException("Error reading project file", exc);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException ioe) {
                    // ignore this
                }
            }
        }
    }

    /**
     * The common superclass for all SAX event handlers used to parse
     * the configuration file. Each method just throws an exception, 
     * so subclasses should override what they can handle.
     *
     * Each type of XML element (task, target, etc.) in Ant has
     * a specific subclass.
     *
     * In the constructor, this class takes over the handling of SAX
     * events from the parent handler and returns
     * control back to the parent in the endElement method.
     */
    private class AbstractHandler extends HandlerBase {
        
        /** 
         * Previous handler for the document. 
         * When the next element is finished, control returns
         * to this handler.
         */
        protected DocumentHandler parentHandler;
        
        /**
         * Creates a handler and sets the parser to use it
         * for the current element.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public AbstractHandler(DocumentHandler parentHandler) {
            this.parentHandler = parentHandler;

            // Start handling SAX events
            parser.setDocumentHandler(this);
        }
        
        /**
         * Handles the start of an element. This base implementation just
         * throws an exception.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void startElement(String tag, AttributeList attrs) throws SAXParseException {
            throw new SAXParseException("Unexpected element \"" + tag + "\"", locator);
        }

        /**
         * Handles text within an element. This base implementation just
         * throws an exception.
         * 
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         * 
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void characters(char[] buf, int start, int count) throws SAXParseException {
            String s = new String(buf, start, count).trim();

            if (s.length() > 0) {
                throw new SAXParseException("Unexpected text \"" + s + "\"", locator);
            }
        }

        /**
         * Called when this element and all elements nested into it have been
         * handled.
         */
        protected void finished() {}

        /**
         * Handles the end of an element. Any required clean-up is performed
         * by the finished() method and then the original handler is restored to
         * the parser.
         * 
         * @param name The name of the element which is ending.
         *             Will not be <code>null</code>.
         * 
         * @exception SAXException in case of error (not thrown in 
         *                         this implementation)
         * 
         * @see #finished()
         */
        public void endElement(String name) throws SAXException {

            finished();
            // Let parent resume handling SAX events
            parser.setDocumentHandler(parentHandler);
        }
    }

    /**
     * Handler for the root element. Its only child must be the "project" element.
     */
    private class RootHandler extends HandlerBase {

        /**
         * Resolves file: URIs relative to the build file.
         * 
         * @param publicId The public identifer, or <code>null</code>
         *                 if none is available. Ignored in this 
         *                 implementation.
         * @param systemId The system identifier provided in the XML 
         *                 document. Will not be <code>null</code>.
         */
        public InputSource resolveEntity(String publicId,
                                         String systemId) {
        
            project.log("resolving systemId: " + systemId, Project.MSG_VERBOSE);
        
            if (systemId.startsWith("file:")) {
                String path = systemId.substring(5);
                int index = path.indexOf("file:");
                
                // we only have to handle these for backward compatibility
                // since they are in the FAQ.
                while (index != -1) {
                    path = path.substring(0, index) + path.substring(index + 5);
                    index = path.indexOf("file:");
                }
                
                String entitySystemId = path;
                index = path.indexOf("%23");
                // convert these to #
                while (index != -1) {
                    path = path.substring(0, index) + "#" + path.substring(index + 3);
                    index = path.indexOf("%23");
                }

                File file = new File(path);
                if (!file.isAbsolute()) {
                    file = new File(buildFileParent, path);
                }
                
                try {
                    InputSource inputSource = new InputSource(new FileInputStream(file));
                    inputSource.setSystemId("file:" + entitySystemId);
                    return inputSource;
                } catch (FileNotFoundException fne) {
                    project.log(file.getAbsolutePath()+" could not be found", 
                                Project.MSG_WARN);
                }
            }
            // use default if not file or file not found
            return null;
        }

        /**
         * Handles the start of a project element. A project handler is created
         * and initialised with the element name and attributes.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if the tag given is not 
         *                              <code>"project"</code>
         */
        public void startElement(String tag, AttributeList attrs) throws SAXParseException {
            if (tag.equals("project")) {
                new ProjectHandler(this).init(tag, attrs);
            } else {
                throw new SAXParseException("Config file is not of expected XML type", locator);
            }
        }

        /**
         * Sets the locator in the project helper for future reference.
         * 
         * @param locator The locator used by the parser.
         *                Will not be <code>null</code>.
         */
        public void setDocumentLocator(Locator locator) {
            ProjectHelper.this.locator = locator;
        }
    }

    /**
     * Handler for the top level "project" element.
     */
    private class ProjectHandler extends AbstractHandler {
        
        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public ProjectHandler(DocumentHandler parentHandler) {
            super(parentHandler);
        }
        
        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. The attributes which
         * this handler can deal with are: <code>"default"</code>,
         * <code>"name"</code>, <code>"id"</code> and <code>"basedir"</code>.
         * 
         * @param tag Name of the element which caused this handler
         *            to be created. Should not be <code>null</code>.
         *            Ignored in this implementation.
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * 
         * @exception SAXParseException if an unexpected attribute is 
         *            encountered or if the <code>"default"</code> attribute
         *            is missing.
         */
        public void init(String tag, AttributeList attrs) throws SAXParseException {
            String def = null;
            String name = null;
            String id = null;
            String baseDir = null;

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getName(i);
                String value = attrs.getValue(i);

                if (key.equals("default")) {
                    def = value;
                } else if (key.equals("name")) {
                    name = value;
                } else if (key.equals("id")) {
                    id = value;
                } else if (key.equals("basedir")) {
                    baseDir = value;
                } else {
                    throw new SAXParseException("Unexpected attribute \"" + attrs.getName(i) + "\"", locator);
                }
            }

            if (def == null) {
                throw new SAXParseException("The default attribute of project is required", 
                                            locator);
            }
            

            project.setDefaultTarget(def);

            if (name != null) {
                project.setName(name);
                project.addReference(name, project);
            }

            if (id != null) {
              project.addReference(id, project);
            }

            if (project.getProperty("basedir") != null) {
                project.setBasedir(project.getProperty("basedir"));
            } else {
                if (baseDir == null) {
                    project.setBasedir(buildFileParent.getAbsolutePath());
                } else {
                    // check whether the user has specified an absolute path
                    if ((new File(baseDir)).isAbsolute()) {
                        project.setBasedir(baseDir);
                    } else {
                        project.setBaseDir(project.resolveFile(baseDir, buildFileParent));
                    }
                }
            }

        }

        /**
         * Handles the start of a top-level element within the project. An
         * appropriate handler is created and initialised with the details
         * of the element.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if the tag given is not 
         *            <code>"taskdef"</code>, <code>"typedef"</code>,
         *            <code>"property"</code>, <code>"target"</code>
         *            or a data type definition
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (name.equals("taskdef")) {
                handleTaskdef(name, attrs);
            } else if (name.equals("typedef")) {
                handleTypedef(name, attrs);
            } else if (name.equals("property")) {
                handleProperty(name, attrs);
            } else if (name.equals("target")) {
                handleTarget(name, attrs);
            } else if (project.getDataTypeDefinitions().get(name) != null) {
                handleDataType(name, attrs);
            } else {
                throw new SAXParseException("Unexpected element \"" + name + "\"", locator);
            }
        }
        
        /**
         * Handles a task defintion element by creating a task handler
         * and initialising is with the details of the element.
         * 
         * @param tag The name of the element to be handled.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element to be handled.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs when initialising 
         *                              the task handler
         *                          
         */
        private void handleTaskdef(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(this, null, null, null)).init(name, attrs);
        }

        /**
         * Handles a type defintion element by creating a task handler
         * and initialising is with the details of the element.
         * 
         * @param tag The name of the element to be handled.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element to be handled.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs initialising the 
         *                              handler
         */
        private void handleTypedef(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(this, null, null, null)).init(name, attrs);
        }

        /**
         * Handles a property defintion element by creating a task handler
         * and initialising is with the details of the element.
         * 
         * @param tag The name of the element to be handled.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element to be handled.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs initialising 
         *                              the handler
         */
        private void handleProperty(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(this, null, null, null)).init(name, attrs);
        }

        /**
         * Handles a target defintion element by creating a target handler
         * and initialising is with the details of the element.
         * 
         * @param tag The name of the element to be handled.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element to be handled.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs initialising 
         *                              the handler
         */
        private void handleTarget(String tag, AttributeList attrs) throws SAXParseException {
            new TargetHandler(this).init(tag, attrs);
        }
        /**
         * Handles a data type defintion element by creating a data type 
         * handler and initialising is with the details of the element.
         * 
         * @param tag The name of the element to be handled.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element to be handled.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs initialising 
         *                              the handler
         */
        private void handleDataType(String name, AttributeList attrs) throws SAXParseException {
            new DataTypeHandler(this).init(name, attrs);
        }

    }

    /**
     * Handler for "target" elements.
     */
    private class TargetHandler extends AbstractHandler {
        private Target target;

        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public TargetHandler(DocumentHandler parentHandler) {
            super(parentHandler);
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. The attributes which
         * this handler can deal with are: <code>"name"</code>,
         * <code>"depends"</code>, <code>"if"</code>,
         * <code>"unless"</code>, <code>"id"</code> and 
         * <code>"description"</code>.
         * 
         * @param tag Name of the element which caused this handler
         *            to be created. Should not be <code>null</code>.
         *            Ignored in this implementation.
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * 
         * @exception SAXParseException if an unexpected attribute is encountered
         *            or if the <code>"name"</code> attribute is missing.
         */
        public void init(String tag, AttributeList attrs) throws SAXParseException {
            String name = null;
            String depends = "";
            String ifCond = null;
            String unlessCond = null;
            String id = null;
            String description = null;

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getName(i);
                String value = attrs.getValue(i);

                if (key.equals("name")) {
                    name = value;
                } else if (key.equals("depends")) {
                    depends = value;
                } else if (key.equals("if")) {
                    ifCond = value;
                } else if (key.equals("unless")) {
                    unlessCond = value;
                } else if (key.equals("id")) {
                    id = value;
                } else if (key.equals("description")) {
                    description = value;
                } else {
                    throw new SAXParseException("Unexpected attribute \"" + key + "\"", locator);
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without a name attribute", locator);
            }

            target = new Target();
            target.setName(name);
            target.setIf(ifCond);
            target.setUnless(unlessCond);
            target.setDescription(description);
            project.addTarget(name, target);

            if (id != null && !id.equals("")) {
                project.addReference(id, target);
            }

            // take care of dependencies

            if (depends.length() > 0) {
                target.setDepends(depends);
            }
        }

        /**
         * Handles the start of an element within a target.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (project.getDataTypeDefinitions().get(name) != null) {
                new DataTypeHandler(this, target).init(name, attrs);
            } else {
                new TaskHandler(this, target, null, target).init(name, attrs);
            }
        }
    }

    /**
     * Handler for all task elements.
     */
    private class TaskHandler extends AbstractHandler {
        /** Containing target, if any. */
        private Target target;
        /** 
         * Container for the task, if any. If target is 
         * non-<code>null</code>, this must be too.
         */
        private TaskContainer container;
        /**
         * Task created by this handler.
         */
        private Task task;
        /**
         * Wrapper for the parent element, if any. The wrapper for this 
         * element will be added to this wrapper as a child.
         */
        private RuntimeConfigurable parentWrapper;
        /**
         * Wrapper for this element which takes care of actually configuring
         * the element, if this element is contained within a target. 
         * Otherwise the configuration is performed with the configure method.
         * @see ProjectHelper#configure(Object,AttributeList,Project)
         */
        private RuntimeConfigurable wrapper = null;
        
        /**
         * Constructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         * 
         * @param container     Container for the element. 
         *                      May be <code>null</code> if the target is 
         *                      <code>null</code> as well. If the
         *                      target is <code>null</code>, this parameter
         *                      is effectively ignored.
         * 
         * @param parentWrapper Wrapper for the parent element, if any.
         *                      May be <code>null</code>. If the
         *                      target is <code>null</code>, this parameter
         *                      is effectively ignored.
         * 
         * @param target        Target this element is part of.
         *                      May be <code>null</code>.
         */
        public TaskHandler(DocumentHandler parentHandler, TaskContainer container, RuntimeConfigurable parentWrapper, Target target) {
            super(parentHandler);
            this.container = container;
            this.parentWrapper = parentWrapper;
            this.target = target;
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         * 
         * @param tag Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         *            
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * 
         * @exception SAXParseException in case of error (not thrown in
         *                              this implementation)
         */
        public void init(String tag, AttributeList attrs) throws SAXParseException {
            try {
                task = project.createTask(tag);
            } catch (BuildException e) {
                // swallow here, will be thrown again in 
                // UnknownElement.maybeConfigure if the problem persists.
            }

            if (task == null) {
                task = new UnknownElement(tag);
                task.setProject(project);
                task.setTaskType(tag);
                task.setTaskName(tag);
            }

            task.setLocation(new Location(buildFile.toString(), locator.getLineNumber(), locator.getColumnNumber()));
            configureId(task, attrs);

            // Top level tasks don't have associated targets
            if (target != null) {
                task.setOwningTarget(target);
                container.addTask(task);
                task.init();
                wrapper = task.getRuntimeConfigurableWrapper();
                wrapper.setAttributes(attrs);
                if (parentWrapper != null) {
                    parentWrapper.addChild(wrapper);
                }
            } else {
                task.init();
                configure(task, attrs, project);
            }
        }

        /**
         * Executes the task if it is a top-level one.
         */
        protected void finished() {
            if (task != null && target == null) {
                task.execute();
            }
        }

        /**
         * Adds text to the task, using the wrapper if one is
         * available (in other words if the task is within a target) 
         * or using addText otherwise.
         * 
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         * 
         * @exception SAXParseException if the element doesn't support text
         * 
         * @see ProjectHelper#addText(Project,Object,char[],int,int)
         */
        public void characters(char[] buf, int start, int count) throws SAXParseException {
            if (wrapper == null) {
                try {
                    addText(project, task, buf, start, count);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), locator, exc);
                }
            } else {
                wrapper.addText(buf, start, count);
            }
        }
        
        /**
         * Handles the start of an element within a target. Task containers
         * will always use another task handler, and all other tasks
         * will always use a nested element handler.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (task instanceof TaskContainer) {
                // task can contain other tasks - no other nested elements possible
                new TaskHandler(this, (TaskContainer)task, wrapper, target).init(name, attrs);
            }
            else {
                new NestedElementHandler(this, task, wrapper, target).init(name, attrs);
            }
        }
    }

    /**
     * Handler for all nested properties.
     */
    private class NestedElementHandler extends AbstractHandler {
        /** Parent object (task/data type/etc). */
        private Object parent;
        /** The nested element itself. */
        private Object child;
        /**
         * Wrapper for the parent element, if any. The wrapper for this 
         * element will be added to this wrapper as a child.
         */
        private RuntimeConfigurable parentWrapper;
        /**
         * Wrapper for this element which takes care of actually configuring
         * the element, if a parent wrapper is provided.
         * Otherwise the configuration is performed with the configure method.
         * @see ProjectHelper#configure(Object,AttributeList,Project)
         */
        private RuntimeConfigurable childWrapper = null;
        /** Target this element is part of, if any. */
        private Target target;

        /**
         * Constructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         * 
         * @param parent        Parent of this element (task/data type/etc).
         *                      Must not be <code>null</code>.
         * 
         * @param parentWrapper Wrapper for the parent element, if any.
         *                      May be <code>null</code>.
         * 
         * @param target        Target this element is part of.
         *                      May be <code>null</code>.
         */
        public NestedElementHandler(DocumentHandler parentHandler, 
                                    Object parent,
                                    RuntimeConfigurable parentWrapper,
                                    Target target) {
            super(parentHandler);

            if (parent instanceof TaskAdapter) {
                this.parent = ((TaskAdapter) parent).getProxy();
            } else {
                this.parent = parent;
            }
            this.parentWrapper = parentWrapper;
            this.target = target;
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         * 
         * @param tag Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         *            
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * 
         * @exception SAXParseException in case of error, such as a 
         *            BuildException being thrown during configuration.
         */
        public void init(String propType, AttributeList attrs) throws SAXParseException {
            Class parentClass = parent.getClass();
            IntrospectionHelper ih = 
                IntrospectionHelper.getHelper(parentClass);

            try {
                String elementName = propType.toLowerCase(Locale.US);
                if (parent instanceof UnknownElement) {
                    UnknownElement uc = new UnknownElement(elementName);
                    uc.setProject(project);
                    ((UnknownElement) parent).addChild(uc);
                    child = uc;
                } else {
                    child = ih.createElement(project, parent, elementName);
                }

                configureId(child, attrs);

                if (parentWrapper != null) {
                    childWrapper = new RuntimeConfigurable(child, propType);
                    childWrapper.setAttributes(attrs);
                    parentWrapper.addChild(childWrapper);
                } else {
                    configure(child, attrs, project);
                    ih.storeElement(project, parent, child, elementName);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        /**
         * Adds text to the element, using the wrapper if one is
         * available or using addText otherwise.
         * 
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         * 
         * @exception SAXParseException if the element doesn't support text
         * 
         * @see ProjectHelper#addText(Project,Object,char[],int,int)
         */
        public void characters(char[] buf, int start, int count) throws SAXParseException {
            if (parentWrapper == null) {
                try {
                    addText(project, child, buf, start, count);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), locator, exc);
                }
            } else {
                childWrapper.addText(buf, start, count);
            }
        }

        /**
         * Handles the start of an element within this one. Task containers
         * will always use a task handler, and all other elements
         * will always use another nested element handler.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (child instanceof TaskContainer) {
                // taskcontainer nested element can contain other tasks - no other 
                // nested elements possible
                new TaskHandler(this, (TaskContainer)child, childWrapper, target).init(name, attrs);
            }
            else {
                new NestedElementHandler(this, child, childWrapper, target).init(name, attrs);
            }
        }
    }

    /**
     * Handler for all data types directly subordinate to project or target.
     */
    private class DataTypeHandler extends AbstractHandler {
        /** Parent target, if any. */
        private Target target;
        /** The element being configured. */
        private Object element;
        /** Wrapper for this element, if it's part of a target. */
        private RuntimeConfigurable wrapper = null;
        
        /**
         * Constructor with no target specified.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public DataTypeHandler(DocumentHandler parentHandler) {
            this(parentHandler, null);
        }

        /**
         * Constructor with a target specified.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         * 
         * @param target The parent target of this element.
         *               May be <code>null</code>.
         */
        public DataTypeHandler(DocumentHandler parentHandler, Target target) {
            super(parentHandler);
            this.target = target;
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         * 
         * @param tag Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         *            
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * 
         * @exception SAXParseException in case of error, such as a 
         *            BuildException being thrown during configuration.
         */
        public void init(String propType, AttributeList attrs) throws SAXParseException {
            try {
                element = project.createDataType(propType);
                if (element == null) {
                    throw new BuildException("Unknown data type "+propType);
                }
                
                if (target != null) {
                    wrapper = new RuntimeConfigurable(element, propType);
                    wrapper.setAttributes(attrs);
                    target.addDataType(wrapper);
                } else {
                    configure(element, attrs, project);
                    configureId(element, attrs);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        // XXX: (Jon Skeet) Any reason why this doesn't use the wrapper
        // if one is available, whereas NestedElementHandler.characters does?
        /**
         * Adds text to the element.
         * 
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         * 
         * @exception SAXParseException if the element doesn't support text
         * 
         * @see ProjectHelper#addText(Project,Object,char[],int,int)
         */
        public void characters(char[] buf, int start, int count) throws SAXParseException {
            try {
                addText(project, element, buf, start, count);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        /**
         * Handles the start of an element within this one.
         * This will always use a nested element handler.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs when initialising
         *                              the child handler
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new NestedElementHandler(this, element, wrapper, target).init(name, attrs);
        }
    }

    /**
     * Configures an object using an introspection handler.
     * 
     * @param target The target object to be configured.
     *               Must not be <code>null</code>.
     * @param attrs  A list of attributes to configure within the target.
     *               Must not be <code>null</code>.
     * @param project The project containing the target. 
     *                Must not be <code>null</code>.
     * 
     * @exception BuildException if any of the attributes can't be handled by
     *                           the target
     */
    public static void configure(Object target, AttributeList attrs, 
                                 Project project) throws BuildException {
        if( target instanceof TaskAdapter ) {
            target=((TaskAdapter)target).getProxy();
        }

        IntrospectionHelper ih = 
            IntrospectionHelper.getHelper(target.getClass());

        project.addBuildListener(ih);

        for (int i = 0; i < attrs.getLength(); i++) {
            // reflect these into the target
            String value=replaceProperties(project, attrs.getValue(i), 
                                           project.getProperties() );
            try {
                ih.setAttribute(project, target, 
                                attrs.getName(i).toLowerCase(Locale.US), value);

            } catch (BuildException be) {
                // id attribute must be set externally
                if (!attrs.getName(i).equals("id")) {
                    throw be;
                }
            }
        }
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     * 
     * @param project The project containing the target. 
     *                Must not be <code>null</code>.
     * @param target  The target object to be configured.
     *                Must not be <code>null</code>.
     * @param buf A character array of the text within the element.
     *            Will not be <code>null</code>.
     * @param start The start element in the array.
     * @param count The number of characters to read from the array.
     * 
     * @exception BuildException if the target object doesn't accept text
     */
    public static void addText(Project project, Object target, char[] buf, int start, int count)
        throws BuildException {
        addText(project, target, new String(buf, start, count));
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     * 
     * @param project The project containing the target. 
     *                Must not be <code>null</code>.
     * @param target  The target object to be configured.
     *                Must not be <code>null</code>.
     * @param text    Text to add to the target.
     *                May be <code>null</code>, in which case this
     *                method call is a no-op.
     * 
     * @exception BuildException if the target object doesn't accept text
     */
    public static void addText(Project project, Object target, String text)
        throws BuildException {

        if (text == null ) {
            return;
        }

        if(target instanceof TaskAdapter) {
            target = ((TaskAdapter) target).getProxy();
        }

        IntrospectionHelper.getHelper(target.getClass()).addText(project, target, text);
    }

    /**
     * Stores a configured child element within its parent object.
     * 
     * @param project Project containing the objects.
     *                May be <code>null</code>.
     * @param parent  Parent object to add child to.
     *                Must not be <code>null</code>.
     * @param child   Child object to store in parent.
     *                Should not be <code>null</code>.
     * @param tag     Name of element which generated the child.
     *                May be <code>null</code>, in which case
     *                the child is not stored.
     */
    public static void storeChild(Project project, Object parent, Object child, String tag) {
        IntrospectionHelper ih = IntrospectionHelper.getHelper(parent.getClass());
        ih.storeElement(project, parent, child, tag);
    }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value with 
     * the string value of the corresponding properties.
     *
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>.
     *
     * @exception BuildException if the string contains an opening 
     *                           <code>${</code> without a closing 
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     * 
     * @since 1.5
     */
     public static String replaceProperties(Project project, String value)
            throws BuildException {
         return project.replaceProperties(value);
     }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value 
     * with the string value of the corresponding data types.
     *
     * @param project The container project. This is used solely for
     *                logging purposes. Must not be <code>null</code>.
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>, in which case this
     *              method returns immediately with no effect.
     * @param keys  Mapping (String to String) of property names to their 
     *              values. Must not be <code>null</code>.
     * 
     * @exception BuildException if the string contains an opening 
     *                           <code>${</code> without a closing 
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     */
     public static String replaceProperties(Project project, String value, Hashtable keys)
            throws BuildException {
        if (value == null) {
            return null;
        }

        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Enumeration i = fragments.elements();
        Enumeration j = propertyRefs.elements();
        while (i.hasMoreElements()) {
            String fragment = (String)i.nextElement();
            if (fragment == null) {
                String propertyName = (String)j.nextElement();
                if (!keys.containsKey(propertyName)) {
                    project.log("Property ${" + propertyName + "} has not been set", Project.MSG_VERBOSE);
                }
                fragment = (keys.containsKey(propertyName)) ? (String) keys.get(propertyName) 
                                                            : "${" + propertyName + "}"; 
            }
            sb.append(fragment);
        }                        
        
        return sb.toString();
    }

    /**
     * Parses a string containing <code>${xxx}</code> style property
     * references into two lists. The first list is a collection
     * of text fragments, while the other is a set of string property names.
     * <code>null</code> entries in the first list indicate a property 
     * reference from the second list.
     * 
     * @param value     Text to parse. Must not be <code>null</code>.
     * @param fragments List to add text fragments to. 
     *                  Must not be <code>null</code>.
     * @param propertyRefs List to add property names to.
     *                     Must not be <code>null</code>.
     * 
     * @exception BuildException if the string contains an opening 
     *                           <code>${</code> without a closing 
     *                           <code>}</code>
     */
    public static void parsePropertyString(String value, Vector fragments, Vector propertyRefs) 
        throws BuildException {
        int prev = 0;
        int pos;
        while ((pos = value.indexOf("$", prev)) >= 0) {
            if (pos > 0) {
                fragments.addElement(value.substring(prev, pos));
            }

            if( pos == (value.length() - 1)) {
                fragments.addElement("$");
                prev = pos + 1;
            }
            else if (value.charAt(pos + 1) != '{' ) {
                fragments.addElement(value.substring(pos + 1, pos + 2));
                prev = pos + 2;
            } else {
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new BuildException("Syntax error in property: " 
                                                 + value );
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.addElement(null);
                propertyRefs.addElement(propertyName);
                prev = endName + 1;
            }
        }

        if (prev < value.length()) {
            fragments.addElement(value.substring(prev));
        }
    }

    /**
     * Returns the parser factory to use. Only one parser
     * factory is ever created by this method (multi-threading 
     * issues aside) and is then cached for future use.
     * 
     * @return a SAXParserFactory to use within this class
     */
    private static SAXParserFactory getParserFactory() {
        if (parserFactory == null) {
            parserFactory = SAXParserFactory.newInstance();
        }

        return parserFactory;
    }

    /**
     * Scans an attribute list for the <code>id</code> attribute and 
     * stores a reference to the target object in the project if an
     * id is found.
     * <p>
     * This method was moved out of the configure method to allow
     * it to be executed at parse time.
     * 
     * @see #configure(Object,AttributeList,Project)
     */
    private void configureId(Object target, AttributeList attr) {
        String id = attr.getValue("id");
        if (id != null) {
            project.addReference(id, target);
        }
    }
}
