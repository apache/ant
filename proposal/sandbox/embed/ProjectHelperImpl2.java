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

package org.apache.tools.ant.helper;

import org.apache.tools.ant.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Stack;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
//import org.xml.sax.HandlerBase;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.DocumentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.XMLReaderAdapter;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributeListImpl;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.tools.ant.types.SystemPath;
/**
 * Sax2 based project reader
 *
 * @author duncan@x180.com
 * @author Costin Manolache
 */
public class ProjectHelperImpl2 extends ProjectHelper {
    /* Stateless */
    
    /** 
     * Parser factory to use to create parsers.
     * @see #getParserFactory
     */
    private static SAXParserFactory parserFactory = null;

    /**
     * Parses the project file, configuring the project as it goes.
     * 
     * @exception BuildException if the configuration is invalid or cannot 
     *                           be read
     */
    public void parse(Project project, Object source) throws BuildException {
        // Hook our one tasks.
        project.addDataTypeDefinition( "systemPath" , SystemPath.class );
        
        AntXmlContext context=new AntXmlContext();
        if(source instanceof File) {
            context.buildFile=(File)source;
//         } else if( source instanceof InputStream ) {
//         } else if( source instanceof URL ) {
//         } else if( source instanceof InputSource ) {
        } else {
            throw new BuildException( "Source " + source.getClass().getName() +
                                      " not supported by this plugin" );
        }

        FileInputStream inputStream = null;
        InputSource inputSource = null;

        context.project = project;
        context.buildFile = new File(context.buildFile.getAbsolutePath());
        context.buildFileParent = new File(context.buildFile.getParent());
        
        try {
            /**
             * SAX 2 style parser used to parse the given file. 
             */
            org.xml.sax.XMLReader parser;
    
            if (parserFactory == null) {
                parserFactory = SAXParserFactory.newInstance();
            }

            SAXParser saxParser = parserFactory.newSAXParser();
            parser =saxParser.getXMLReader();

            String uri = "file:" + context.buildFile.getAbsolutePath().replace('\\', '/');
            for (int index = uri.indexOf('#'); index != -1; index = uri.indexOf('#')) {
                uri = uri.substring(0, index) + "%23" + uri.substring(index+1);
            }
            
            inputStream = new FileInputStream(context.buildFile);
            inputSource = new InputSource(inputStream);
            inputSource.setSystemId(uri);
            project.log("parsing buildfile " + context.buildFile + " with URI = " + uri, Project.MSG_VERBOSE);

            DefaultHandler hb = new RootHandler(context);

            parser.setContentHandler(hb);
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
                new Location(context.buildFile.toString(), exc.getLineNumber(), exc.getColumnNumber());

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
    public static class AntHandler  {
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
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
            throw new SAXParseException("Unexpected element \" " + qname + "\"", context.locator);
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
        public AntHandler onStartChild(String uri, String tag, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            throw new SAXParseException("Unexpected element \"" + qname + " \"", context.locator);
        }

        /**
         * Called when this element and all elements nested into it have been
         * handled.
         */
        public void onEndElement(String uri, String tag, AntXmlContext context) {
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
        public void characters(char[] buf, int start, int count, AntXmlContext context)
            throws SAXParseException
        {
            String s = new String(buf, start, count).trim();

            if (s.length() > 0) {
                throw new SAXParseException("Unexpected text \"" + s + "\"", context.locator);
            }
        }
    }

    /** Context information for ant deserialization
     */
    public static class AntXmlContext {
        /** The project to configure. */
        Project project;
        /** The configuration file to parse. */
        File buildFile;
        /** 
         * Parent directory of the build file. Used for resolving entities
         * and setting the project's base directory.
         */
        File buildFileParent;
        /** 
         * Locator for the configuration file parser. 
         * Used for giving locations of errors etc.
         */
        Locator locator;

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
        void configureId(Object target, Attributes attr) {
            String id = attr.getValue("id");
            if (id != null) {
                project.addReference(id, target);
            }
        }

    }
    
    
    /**
     * Handler for ant processing. Uses a stack of AntHandlers to
     * implement each element ( the original parser used a recursive behavior,
     * with the implicit execution stack )
     */
    public static class RootHandler extends DefaultHandler {
        Stack antHandlers=new Stack();
        AntHandler currentHandler;
        AntXmlContext context;
        
        public RootHandler(AntXmlContext context) {
            currentHandler=new MainHandler();
            antHandlers.push( currentHandler );
            this.context=context;
        }
        
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
        
            context.project.log("resolving systemId: " + systemId, Project.MSG_VERBOSE);
        
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
                    file = new File(context.buildFileParent, path);
                }
                
                try {
                    InputSource inputSource = new InputSource(new FileInputStream(file));
                    inputSource.setSystemId("file:" + entitySystemId);
                    return inputSource;
                } catch (FileNotFoundException fne) {
                    context.project.log(file.getAbsolutePath()+" could not be found", 
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
        public void startElement(String uri, String tag, String qname, Attributes attrs)
            throws SAXParseException
        {
            AntHandler next=currentHandler.onStartChild(uri, tag, qname, attrs, context);
            antHandlers.push( currentHandler );
            //System.out.println("XXX push " + currentHandler );
            currentHandler=next;
            currentHandler.onStartElement( uri, tag, qname, attrs, context );
        }

        /**
         * Sets the locator in the project helper for future reference.
         * 
         * @param locator The locator used by the parser.
         *                Will not be <code>null</code>.
         */
        public void setDocumentLocator(Locator locator) {
            context.locator = locator;
        }

        /**
         * Handles the end of an element. Any required clean-up is performed
         * by the onEndElement() method and then the original handler is restored to
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
        public void endElement(String uri, String name, String qName) throws SAXException {
            currentHandler.onEndElement(uri, name, context);
            AntHandler prev=(AntHandler)antHandlers.pop();
            //System.out.println("XXX pop " + currentHandler + " " + prev);
            currentHandler=prev;
        }

        public void characters(char[] buf, int start, int count)
            throws SAXParseException
        {
            currentHandler.characters( buf, start, count, context );
        }
    }

    public static class MainHandler extends AntHandler {

        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
        }

        public AntHandler onStartChild(String uri, String name, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            if (qname.equals("project")) {
                return new ProjectHandler();
            } else {
                throw new SAXParseException("Unexpected element \"" + qname + "\" " + name, context.locator);
            }
        }        
    }
    
    /**
     * Handler for the top level "project" element.
     */
    public static class ProjectHandler extends AntHandler {
        
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
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
            String def = null;
            String name = null;
            String id = null;
            String baseDir = null;

            if (! qname.equals("project")) {
                throw new SAXParseException("Config file is not of expected XML type", context.locator);
            }

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getQName(i);
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
                    throw new SAXParseException("Unexpected attribute \"" + attrs.getQName(i) + "\"", context.locator);
                }
            }

            if (def == null) {
                throw new SAXParseException("The default attribute of project is required", 
                                            context.locator);
            }
            
            Project project=context.project;
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
                    project.setBasedir(context.buildFileParent.getAbsolutePath());
                } else {
                    // check whether the user has specified an absolute path
                    if ((new File(baseDir)).isAbsolute()) {
                        project.setBasedir(baseDir);
                    } else {
                        project.setBaseDir(project.resolveFile(baseDir,
                                                               context.buildFileParent));
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
        public AntHandler onStartChild(String uri, String name, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            if (qname.equals("taskdef")) {
                return new TaskHandler(null, null, null);
            } else if (qname.equals("typedef")) {
                return new TaskHandler(null, null, null);
            } else if (qname.equals("property")) {
                return new TaskHandler(null, null, null);
            } else if (qname.equals("target")) {
                return new TargetHandler();
            } else if (context.project.getDataTypeDefinitions().get(qname) != null) {
                return new DataTypeHandler(null);
            } else {
                throw new SAXParseException("Unexpected element \"" + qname + "\" " + name, context.locator);
            }
        }
    }

    /**
     * Handler for "target" elements.
     */
    public static class TargetHandler extends AntHandler {
        private Target target;

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
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
            String name = null;
            String depends = "";
            String ifCond = null;
            String unlessCond = null;
            String id = null;
            String description = null;

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getQName(i);
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
                    throw new SAXParseException("Unexpected attribute \"" + key + "\"", context.locator);
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without a name attribute",
                                            context.locator);
            }

            target = new Target();
            target.setName(name);
            target.setIf(ifCond);
            target.setUnless(unlessCond);
            target.setDescription(description);
            context.project.addTarget(name, target);

            if (id != null && !id.equals("")) {
                context.project.addReference(id, target);
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
        public AntHandler onStartChild(String uri, String name, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            if (context.project.getDataTypeDefinitions().get(qname) != null) {
                return new DataTypeHandler(target);
            } else {
                return new TaskHandler(target, null, target);
            }
        }
    }

    /**
     * Handler for all task elements.
     */
    public static class TaskHandler extends AntHandler {
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
        private RuntimeConfigurable2 parentWrapper;
        /**
         * Wrapper for this element which takes care of actually configuring
         * the element, if this element is contained within a target. 
         * Otherwise the configuration is performed with the configure method.
         * @see ProjectHelper#configure(Object,Attributes,Project)
         */
        private RuntimeConfigurable2 wrapper = null;
        
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
        public TaskHandler(TaskContainer container, RuntimeConfigurable2 parentWrapper, Target target) {
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
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
            try {
                task = context.project.createTask(qname);
            } catch (BuildException e) {
                // swallow here, will be thrown again in 
                // UnknownElement.maybeConfigure if the problem persists.
            }

            if (task == null) {
                task = new UnknownElement(qname);
                task.setProject(context.project);
                //XXX task.setTaskType(qname);
                task.setTaskName(qname);
            }

            task.setLocation(new Location(context.buildFile.toString(),
                                          context.locator.getLineNumber(),
                                          context.locator.getColumnNumber()));
            context.configureId(task, attrs);

            // Top level tasks don't have associated targets
            if (target != null) {
                task.setOwningTarget(target);
                container.addTask(task);
                task.init();
                //wrapper = task.getRuntimeConfigurableWrapper();
                wrapper=new RuntimeConfigurable2(task, task.getTaskName());
                wrapper.setAttributes2(attrs);
                if (parentWrapper != null) {
                    parentWrapper.addChild(wrapper);
                }
            } else {
                task.init();
                PropertyHelper.getPropertyHelper(context.project).configure(task, attrs, context.project);
            }
        }

        /**
         * Executes the task if it is a top-level one.
         */
        public void onEndElement(String uri, String tag, AntXmlContext context) {
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
        public void characters(char[] buf, int start, int count,
                               AntXmlContext context)
            throws SAXParseException
        {
            if (wrapper == null) {
                try {
                    ProjectHelper.addText(context.project, task, buf, start, count);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), context.locator, exc);
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
        public AntHandler onStartChild(String uri, String tag, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            if (task instanceof TaskContainer) {
                // task can contain other tasks - no other nested elements possible
                return new TaskHandler((TaskContainer)task, wrapper, target);
            }
            else {
                return new NestedElementHandler(task, wrapper, target);
            }
        }
    }

    /**
     * Handler for all nested properties.
     */
    public static class NestedElementHandler extends AntHandler {
        /** Parent object (task/data type/etc). */
        private Object parent;
        /** The nested element itself. */
        private Object child;
        /**
         * Wrapper for the parent element, if any. The wrapper for this 
         * element will be added to this wrapper as a child.
         */
        private RuntimeConfigurable2 parentWrapper;
        /**
         * Wrapper for this element which takes care of actually configuring
         * the element, if a parent wrapper is provided.
         * Otherwise the configuration is performed with the configure method.
         * @see ProjectHelper#configure(Object,Attributes,Project)
         */
        private RuntimeConfigurable2 childWrapper = null;
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
        public NestedElementHandler(Object parent,
                                    RuntimeConfigurable2 parentWrapper,
                                    Target target) {
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
        public void onStartElement(String uri, String propType, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
            Class parentClass = parent.getClass();
            IntrospectionHelper ih = 
                IntrospectionHelper.getHelper(parentClass);

            try {
                String elementName = qname.toLowerCase(Locale.US);
                if (parent instanceof UnknownElement) {
                    UnknownElement uc = new UnknownElement(elementName);
                    uc.setProject(context.project);
                    ((UnknownElement) parent).addChild(uc);
                    child = uc;
                } else {
                    child = ih.createElement(context.project, parent, elementName);
                }

                context.configureId(child, attrs);

                if (parentWrapper != null) {
                    childWrapper = new RuntimeConfigurable2(child, qname);
                    childWrapper.setAttributes2(attrs);
                    parentWrapper.addChild(childWrapper);
                } else {
                    PropertyHelper.getPropertyHelper(context.project).configure(child, attrs, context.project);
                    ih.storeElement(context.project, parent, child, elementName);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), context.locator, exc);
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
        public void characters(char[] buf, int start, int count,
                               AntXmlContext context)
            throws SAXParseException
        {
            if (parentWrapper == null) {
                try {
                    ProjectHelper.addText(context.project, child, buf, start, count);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), context.locator, exc);
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
        public AntHandler onStartChild(String uri, String tag, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            if (child instanceof TaskContainer) {
                // taskcontainer nested element can contain other tasks - no other 
                // nested elements possible
                return new TaskHandler((TaskContainer)child, childWrapper, target);
            }
            else {
                return new NestedElementHandler(child, childWrapper, target);
            }
        }
    }

    /**
     * Handler for all data types directly subordinate to project or target.
     */
    public static class DataTypeHandler extends AntHandler {
        /** Parent target, if any. */
        private Target target;
        /** The element being configured. */
        private Object element;
        /** Wrapper for this element, if it's part of a target. */
        private RuntimeConfigurable2 wrapper = null;
        
        /**
         * Constructor with a target specified.
         * 
         * @param target The parent target of this element.
         *               May be <code>null</code>.
         */
        public DataTypeHandler( Target target) {
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
        public void onStartElement(String uri, String propType, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
            try {
                element = context.project.createDataType(qname);
                if (element == null) {
                    throw new BuildException("Unknown data type "+qname);
                }
                
                if (target != null) {
                    wrapper = new RuntimeConfigurable2(element, qname);
                    wrapper.setAttributes2(attrs);
                    target.addDataType(wrapper);
                } else {
                    PropertyHelper.getPropertyHelper(context.project).configure(element, attrs, context.project);
                    context.configureId(element, attrs);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), context.locator, exc);
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
        public void characters(char[] buf, int start, int count,
                               AntXmlContext context)
            throws SAXParseException
        {
            try {
                ProjectHelper.addText(context.project, element, buf, start, count);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), context.locator, exc);
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
        public AntHandler onStartChild(String uri, String tag, String qname,
                                       Attributes attrs, AntXmlContext context)
            throws SAXParseException
        {
            return new NestedElementHandler(element, wrapper, target);
        }
    }
    
}
