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
 * 4. The names "Ant" and "Apache Software
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.DocumentHandler;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.XMLReaderAdapter;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.util.JAXPUtils;

/**
 * Original helper.
 *
 * @author duncan@x180.com
 */
public class ProjectHelperImpl extends ProjectHelper {

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
     * Parses the project file, configuring the project as it goes.
     *
     * @param project project instance to be configured.
     * @param source the source from which the project is read.
     * @exception BuildException if the configuration is invalid or cannot
     *                           be read.
     */
    public void parse(Project project, Object source) throws BuildException {
        if (!(source instanceof File)) {
            throw new BuildException("Only File source supported by default plugin");
        }
        File buildFile = (File) source;
        FileInputStream inputStream = null;
        InputSource inputSource = null;

        this.project = project;
        this.buildFile = new File(buildFile.getAbsolutePath());
        buildFileParent = new File(this.buildFile.getParent());

        try {
            try {
                parser = JAXPUtils.getParser();
            } catch (BuildException e) {
                parser = new XMLReaderAdapter(JAXPUtils.getXMLReader());
            }


            String uri = "file:" + buildFile.getAbsolutePath().replace('\\', '/');
            for (int index = uri.indexOf('#'); index != -1; index = uri.indexOf('#')) {
                uri = uri.substring(0, index) + "%23" + uri.substring(index + 1);
            }

            inputStream = new FileInputStream(buildFile);
            inputSource = new InputSource(inputStream);
            inputSource.setSystemId(uri);
            project.log("parsing buildfile " + buildFile + " with URI = "
                + uri, Project.MSG_VERBOSE);
            HandlerBase hb = new RootHandler(this);
            parser.setDocumentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
            parser.parse(inputSource);
        } catch (SAXParseException exc) {
            Location location =
                new Location(exc.getSystemId(), exc.getLineNumber(),
                    exc.getColumnNumber());

            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                BuildException be = (BuildException) t;
                if (be.getLocation() == Location.UNKNOWN_LOCATION) {
                    be.setLocation(location);
                }
                throw be;
            }

            throw new BuildException(exc.getMessage(), t, location);
        } catch (SAXException exc) {
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(exc.getMessage(), t);
        } catch (FileNotFoundException exc) {
            throw new BuildException(exc);
        } catch (UnsupportedEncodingException exc) {
            throw new BuildException("Encoding of project file is invalid.",
                                     exc);
        } catch (IOException exc) {
            throw new BuildException("Error reading project file: "
                                     + exc.getMessage(), exc);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
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
    static class AbstractHandler extends HandlerBase {

        /**
         * Previous handler for the document.
         * When the next element is finished, control returns
         * to this handler.
         */
        protected DocumentHandler parentHandler;

        /** Helper impl. With non-static internal classes, the compiler will generate
            this automatically - but this will fail with some compilers ( reporting
            "Expecting to find object/array on stack" ). If we pass it
            explicitely it'll work with more compilers.
        */
        ProjectHelperImpl helperImpl;

        /**
         * Creates a handler and sets the parser to use it
         * for the current element.
         *
         * @param helperImpl the ProjectHelperImpl instance associated
         *                   with this handler.
         *
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         */
        public AbstractHandler(ProjectHelperImpl helperImpl, DocumentHandler parentHandler) {
            this.parentHandler = parentHandler;
            this.helperImpl = helperImpl;

            // Start handling SAX events
            helperImpl.parser.setDocumentHandler(this);
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
            throw new SAXParseException("Unexpected element \"" + tag + "\"", helperImpl.locator);
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
                throw new SAXParseException("Unexpected text \"" + s + "\"", helperImpl.locator);
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
            helperImpl.parser.setDocumentHandler(parentHandler);
        }
    }

    /**
     * Handler for the root element. Its only child must be the "project" element.
     */
    static class RootHandler extends HandlerBase {
        ProjectHelperImpl helperImpl;

        public RootHandler(ProjectHelperImpl helperImpl) {
            this.helperImpl = helperImpl;
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

            helperImpl.project.log("resolving systemId: " + systemId, Project.MSG_VERBOSE);

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
                    file = new File(helperImpl.buildFileParent, path);
                }

                try {
                    InputSource inputSource = new InputSource(new FileInputStream(file));
                    inputSource.setSystemId("file:" + entitySystemId);
                    return inputSource;
                } catch (FileNotFoundException fne) {
                    helperImpl.project.log(file.getAbsolutePath() + " could not be found",
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
                new ProjectHandler(helperImpl, this).init(tag, attrs);
            } else {
                throw new SAXParseException("Config file is not of expected XML type", helperImpl.locator);
            }
        }

        /**
         * Sets the locator in the project helper for future reference.
         *
         * @param locator The locator used by the parser.
         *                Will not be <code>null</code>.
         */
        public void setDocumentLocator(Locator locator) {
            helperImpl.locator = locator;
        }
    }

    /**
     * Handler for the top level "project" element.
     */
    static class ProjectHandler extends AbstractHandler {

        /**
         * Constructor which just delegates to the superconstructor.
         *
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         */
        public ProjectHandler(ProjectHelperImpl helperImpl, DocumentHandler parentHandler) {
            super(helperImpl, parentHandler);
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
                    throw new SAXParseException("Unexpected attribute \"" + attrs.getName(i) + "\"",
                                                helperImpl.locator);
                }
            }

            if (def == null) {
                throw new SAXParseException("The default attribute of project "
                                            + "is required", 
                                            helperImpl.locator);
            } else {
                helperImpl.project.setDefaultTarget(def);
            }

            if (name != null) {
                helperImpl.project.setName(name);
                helperImpl.project.addReference(name, helperImpl.project);
            }

            if (id != null) {
              helperImpl.project.addReference(id, helperImpl.project);
            }

            if (helperImpl.project.getProperty("basedir") != null) {
                helperImpl.project.setBasedir(helperImpl.project.getProperty("basedir"));
            } else {
                if (baseDir == null) {
                    helperImpl.project.setBasedir(helperImpl.buildFileParent.getAbsolutePath());
                } else {
                    // check whether the user has specified an absolute path
                    if ((new File(baseDir)).isAbsolute()) {
                        helperImpl.project.setBasedir(baseDir);
                    } else {
                        helperImpl.project.setBaseDir(helperImpl.project.resolveFile(baseDir,
                                                                                     helperImpl.buildFileParent));
                    }
                }
            }

        }

        /**
         * Handles the start of a top-level element within the project. An
         * appropriate handler is created and initialised with the details
         * of the element.
         *
         * @param name The name of the element being started.
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
            } else if (helperImpl.project.getDataTypeDefinitions().get(name) != null) {
                handleDataType(name, attrs);
            } else {
                throw new SAXParseException("Unexpected element \"" + name + "\"", helperImpl.locator);
            }
        }

        /**
         * Handles a task defintion element by creating a task handler
         * and initialising is with the details of the element.
         *
         * @param name The name of the element to be handled.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element to be handled.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the task handler
         *
         */
        private void handleTaskdef(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(helperImpl, this, null, null, null)).init(name, attrs);
        }

        /**
         * Handles a type defintion element by creating a task handler
         * and initialising is with the details of the element.
         *
         * @param name The name of the element to be handled.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element to be handled.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs initialising the
         *                              handler
         */
        private void handleTypedef(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(helperImpl, this, null, null, null)).init(name, attrs);
        }

        /**
         * Handles a property defintion element by creating a task handler
         * and initialising is with the details of the element.
         *
         * @param name The name of the element to be handled.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element to be handled.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs initialising
         *                              the handler
         */
        private void handleProperty(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(helperImpl, this, null, null, null)).init(name, attrs);
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
            new TargetHandler(helperImpl, this).init(tag, attrs);
        }
        /**
         * Handles a data type defintion element by creating a data type
         * handler and initialising is with the details of the element.
         *
         * @param name The name of the element to be handled.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element to be handled.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs initialising
         *                              the handler
         */
        private void handleDataType(String name, AttributeList attrs) throws SAXParseException {
            new DataTypeHandler(helperImpl, this).init(name, attrs);
        }

    }

    /**
     * Handler for "target" elements.
     */
    static class TargetHandler extends AbstractHandler {
        private Target target;

        /**
         * Constructor which just delegates to the superconstructor.
         *
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         */
        public TargetHandler(ProjectHelperImpl helperImpl, DocumentHandler parentHandler) {
            super(helperImpl, parentHandler);
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
                    throw new SAXParseException("Unexpected attribute \"" + key + "\"", helperImpl.locator);
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without a name attribute",
                                            helperImpl.locator);
            }

            target = new Target();
            target.setName(name);
            target.setIf(ifCond);
            target.setUnless(unlessCond);
            target.setDescription(description);
            helperImpl.project.addTarget(name, target);

            if (id != null && !id.equals("")) {
                helperImpl.project.addReference(id, target);
            }

            // take care of dependencies

            if (depends.length() > 0) {
                target.setDepends(depends);
            }
        }

        /**
         * Handles the start of an element within a target.
         *
         * @param name The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (helperImpl.project.getDataTypeDefinitions().get(name) != null) {
                new DataTypeHandler(helperImpl, this, target).init(name, attrs);
            } else {
                new TaskHandler(helperImpl, this, target, null, target).init(name, attrs);
            }
        }
    }

    /**
     * Handler for all task elements.
     */
    static class TaskHandler extends AbstractHandler {
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
        public TaskHandler(ProjectHelperImpl helperImpl, DocumentHandler parentHandler,
                           TaskContainer container, RuntimeConfigurable parentWrapper, Target target) {
            super(helperImpl, parentHandler);
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
                task = helperImpl.project.createTask(tag);
            } catch (BuildException e) {
                // swallow here, will be thrown again in
                // UnknownElement.maybeConfigure if the problem persists.
            }

            if (task == null) {
                task = new UnknownElement(tag);
                task.setProject(helperImpl.project);
                //XXX task.setTaskType(tag);
                task.setTaskName(tag);
            }

            task.setLocation(new Location(helperImpl.locator.getSystemId(), helperImpl.locator.getLineNumber(),
                                          helperImpl.locator.getColumnNumber()));
            helperImpl.configureId(task, attrs);

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
                configure(task, attrs, helperImpl.project);
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
                    ProjectHelper.addText(helperImpl.project, task, buf, start, count);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), helperImpl.locator, exc);
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
         * @param name The name of the element being started.
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
                new TaskHandler(helperImpl, this, (TaskContainer) task,
                    wrapper, target).init(name, attrs);
            } else {
                new NestedElementHandler(helperImpl, this, task,
                    wrapper, target).init(name, attrs);
            }
        }
    }

    /**
     * Handler for all nested properties.
     */
    static class NestedElementHandler extends AbstractHandler {
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
        public NestedElementHandler(ProjectHelperImpl helperImpl,
                                    DocumentHandler parentHandler,
                                    Object parent,
                                    RuntimeConfigurable parentWrapper,
                                    Target target) {
            super(helperImpl, parentHandler);

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
         * @param propType Name of the element which caused this handler
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
                    uc.setProject(helperImpl.project);
                    ((UnknownElement) parent).addChild(uc);
                    child = uc;
                } else {
                    child = ih.createElement(helperImpl.project, parent, elementName);
                }

                helperImpl.configureId(child, attrs);

                if (parentWrapper != null) {
                    childWrapper = new RuntimeConfigurable(child, propType);
                    childWrapper.setAttributes(attrs);
                    parentWrapper.addChild(childWrapper);
                } else {
                    configure(child, attrs, helperImpl.project);
                    ih.storeElement(helperImpl.project, parent, child, elementName);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), helperImpl.locator, exc);
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
                    ProjectHelper.addText(helperImpl.project, child, buf, start, count);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), helperImpl.locator, exc);
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
         * @param name The name of the element being started.
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
                new TaskHandler(helperImpl, this, (TaskContainer) child,
                    childWrapper, target).init(name, attrs);
            } else {
                new NestedElementHandler(helperImpl, this, child,
                    childWrapper, target).init(name, attrs);
            }
        }
    }

    /**
     * Handler for all data types directly subordinate to project or target.
     */
    static class DataTypeHandler extends AbstractHandler {
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
        public DataTypeHandler(ProjectHelperImpl helperImpl, DocumentHandler parentHandler) {
            this(helperImpl, parentHandler, null);
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
        public DataTypeHandler(ProjectHelperImpl helperImpl, DocumentHandler parentHandler, Target target) {
            super(helperImpl, parentHandler);
            this.target = target;
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         *
         * @param propType Name of the element which caused this handler
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
                element = helperImpl.project.createDataType(propType);
                if (element == null) {
                    throw new BuildException("Unknown data type " + propType);
                }

                if (target != null) {
                    wrapper = new RuntimeConfigurable(element, propType);
                    wrapper.setAttributes(attrs);
                    target.addDataType(wrapper);
                } else {
                    configure(element, attrs, helperImpl.project);
                    helperImpl.configureId(element, attrs);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), helperImpl.locator, exc);
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
                ProjectHelper.addText(helperImpl.project, element, buf, start, count);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), helperImpl.locator, exc);
            }
        }

        /**
         * Handles the start of an element within this one.
         * This will always use a nested element handler.
         *
         * @param name The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the child handler
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new NestedElementHandler(helperImpl, this, element, wrapper, target).init(name, attrs);
        }
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
