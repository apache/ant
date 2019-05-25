/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.TypeAdapter;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.xml.sax.AttributeList;
import org.xml.sax.DocumentHandler;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.XMLReaderAdapter;

/**
 * Original helper.
 *
 */
public class ProjectHelperImpl extends ProjectHelper {

    /**
     * helper for path -> URI and URI -> path conversions.
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * SAX 1 style parser used to parse the given file. This may
     * in fact be a SAX 2 XMLReader wrapped in an XMLReaderAdapter.
     */
    private Parser parser;

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
     * Target that all other targets will depend upon implicitly.
     *
     * <p>This holds all tasks and data type definitions that have
     * been placed outside of targets.</p>
     */
    private Target implicitTarget = new Target();

    /**
     * default constructor
     */
    public ProjectHelperImpl() {
        implicitTarget.setName("");
    }

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
            throw new BuildException("Only File source supported by "
                + "default plugin");
        }
        File bFile = (File) source;

        this.project = project;
        this.buildFile = new File(bFile.getAbsolutePath());
        buildFileParent = new File(this.buildFile.getParent());

        try {
            parser = JAXPUtils.getParser();
        } catch (BuildException e) {
            parser = new XMLReaderAdapter(JAXPUtils.getXMLReader());
        }

        try (InputStream inputStream = Files.newInputStream(bFile.toPath())) {
            String uri = FILE_UTILS.toURI(bFile.getAbsolutePath());
            InputSource inputSource = new InputSource(inputStream);
            inputSource.setSystemId(uri);
            project.log("parsing buildfile " + bFile + " with URI = " + uri, Project.MSG_VERBOSE);
            HandlerBase hb = new RootHandler(this);
            parser.setDocumentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
            parser.parse(inputSource);
        } catch (SAXParseException exc) {
            Location location = new Location(exc.getSystemId(), exc.getLineNumber(),
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
            throw new BuildException("Encoding of project file is invalid.", exc);
        } catch (IOException exc) {
            throw new BuildException("Error reading project file: " + exc.getMessage(), exc);
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
        // CheckStyle:VisibilityModifier OFF - bc

        /**
         * Previous handler for the document.
         * When the next element is finished, control returns
         * to this handler.
         */
        protected DocumentHandler parentHandler;

        /** Helper impl. With non-static internal classes, the compiler will generate
            this automatically - but this will fail with some compilers (reporting
            "Expecting to find object/array on stack"). If we pass it
            explicitly it'll work with more compilers.
        */
        ProjectHelperImpl helperImpl;
        // CheckStyle:VisibilityModifier ON

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

            if (!s.isEmpty()) {
                throw new SAXParseException("Unexpected text \"" + s + "\"", helperImpl.locator);
            }
        }

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
         */
        public void endElement(String name) throws SAXException {
            // Let parent resume handling SAX events
            helperImpl.parser.setDocumentHandler(parentHandler);
        }
    }

    /**
     * Handler for the root element. Its only child must be the "project" element.
     */
    static class RootHandler extends HandlerBase {
        // CheckStyle:VisibilityModifier OFF - bc
        ProjectHelperImpl helperImpl;
        // CheckStyle:VisibilityModifier ON

        public RootHandler(ProjectHelperImpl helperImpl) {
            this.helperImpl = helperImpl;
        }

        /**
         * Resolves file: URIs relative to the build file.
         *
         * @param publicId The public identifier, or <code>null</code>
         *                 if none is available. Ignored in this
         *                 implementation.
         * @param systemId The system identifier provided in the XML
         *                 document. Will not be <code>null</code>.
         */
        public InputSource resolveEntity(String publicId, String systemId) {

            helperImpl.project.log("resolving systemId: " + systemId, Project.MSG_VERBOSE);

            if (systemId.startsWith("file:")) {
                String path = FILE_UTILS.fromURI(systemId);

                File file = new File(path);
                if (!file.isAbsolute()) {
                    file = FILE_UTILS.resolveFile(helperImpl.buildFileParent, path);
                    helperImpl.project.log("Warning: '" + systemId + "' in " + helperImpl.buildFile
                            + " should be expressed simply as '" + path.replace('\\', '/')
                            + "' for compliance with other XML tools", Project.MSG_WARN);
                }
                try {
                    InputSource inputSource = new InputSource(Files.newInputStream(file.toPath()));
                    inputSource.setSystemId(FILE_UTILS.toURI(file.getAbsolutePath()));
                    return inputSource;
                } catch (IOException fne) {
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
            if ("project".equals(tag)) {
                new ProjectHandler(helperImpl, this).init(tag, attrs);
            } else {
                throw new SAXParseException("Config file is not of expected " + "XML type",
                        helperImpl.locator);
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
                switch (key) {
                    case "default":
                        def = value;
                        break;
                    case "name":
                        name = value;
                        break;
                    case "id":
                        id = value;
                        break;
                    case "basedir":
                        baseDir = value;
                        break;
                    default:
                        throw new SAXParseException("Unexpected attribute \"" + key + "\"",
                                helperImpl.locator);
                }
            }

            if (def != null && !def.isEmpty()) {
                helperImpl.project.setDefault(def);
            } else {
                throw new BuildException("The default attribute is required");
            }

            if (name != null) {
                helperImpl.project.setName(name);
                helperImpl.project.addReference(name, helperImpl.project);
            }

            if (id != null) {
                helperImpl.project.addReference(id, helperImpl.project);
            }

            if (helperImpl.project.getProperty(MagicNames.PROJECT_BASEDIR) != null) {
                helperImpl.project.setBasedir(helperImpl.project.getProperty(MagicNames.PROJECT_BASEDIR));
            } else {
                if (baseDir == null) {
                    helperImpl.project.setBasedir(helperImpl.buildFileParent.getAbsolutePath());
                } else {
                    // check whether the user has specified an absolute path
                    if ((new File(baseDir)).isAbsolute()) {
                        helperImpl.project.setBasedir(baseDir);
                    } else {
                        File resolvedBaseDir = FILE_UTILS.resolveFile(helperImpl.buildFileParent,
                                baseDir);
                        helperImpl.project.setBaseDir(resolvedBaseDir);
                    }
                }
            }

            helperImpl.project.addTarget("", helperImpl.implicitTarget);
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
            if ("target".equals(name)) {
                handleTarget(name, attrs);
            } else {
                handleElement(helperImpl, this, helperImpl.implicitTarget, name, attrs);
            }
        }

        /**
         * Handles a target definition element by creating a target handler
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
                switch (key) {
                    case "name":
                        name = value;
                        if (name.isEmpty()) {
                            throw new BuildException("name attribute must not be empty",
                                    new Location(helperImpl.locator));
                        }
                        break;
                    case "depends":
                        depends = value;
                        break;
                    case "if":
                        ifCond = value;
                        break;
                    case "unless":
                        unlessCond = value;
                        break;
                    case "id":
                        id = value;
                        break;
                    case "description":
                        description = value;
                        break;
                    default:
                        throw new SAXParseException("Unexpected attribute \"" + key + "\"",
                                helperImpl.locator);
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without a name attribute",
                        helperImpl.locator);
            }

            target = new Target();

            // implicit target must be first on dependency list
            target.addDependency("");

            target.setName(name);
            target.setIf(ifCond);
            target.setUnless(unlessCond);
            target.setDescription(description);
            helperImpl.project.addTarget(name, target);

            if (id != null && !id.isEmpty()) {
                helperImpl.project.addReference(id, target);
            }

            // take care of dependencies

            if (!depends.isEmpty()) {
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
            handleElement(helperImpl, this, target, name, attrs);
        }
    }

    /**
     * Start a new DataTypeHandler if element is known to be a
     * data-type and a TaskHandler otherwise.
     *
     * <p>Factored out of TargetHandler.</p>
     *
     * @since Ant 1.6
     */
    private static void handleElement(ProjectHelperImpl helperImpl, DocumentHandler parent,
            Target target, String elementName, AttributeList attrs) throws SAXParseException {
        if ("description".equals(elementName)) {
            // created for side effect
            new DescriptionHandler(helperImpl, parent); //NOSONAR
        } else if (helperImpl.project.getDataTypeDefinitions().get(elementName) != null) {
            new DataTypeHandler(helperImpl, parent, target).init(elementName, attrs);
        } else {
            new TaskHandler(helperImpl, parent, target, null, target).init(elementName, attrs);
        }
    }

    /**
     * Handler for "description" elements.
     */
    static class DescriptionHandler extends AbstractHandler {

        /**
         * Constructor which just delegates to the superconstructor.
         *
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         */
        public DescriptionHandler(ProjectHelperImpl helperImpl,
                                  DocumentHandler parentHandler) {
            super(helperImpl, parentHandler);
        }

        /**
         * Adds the text as description to the project.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         */
        public void characters(char[] buf, int start, int count) {
            String text = new String(buf, start, count);
            String currentDescription = helperImpl.project.getDescription();
            if (currentDescription == null) {
                helperImpl.project.setDescription(text);
            } else {
                helperImpl.project.setDescription(currentDescription + text);
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
         *                      Must not be <code>null</code>.
         *
         * @param parentWrapper Wrapper for the parent element, if any.
         *                      May be <code>null</code>.
         *
         * @param target        Target this element is part of.
         *                      Must not be <code>null</code>.
         */
        public TaskHandler(ProjectHelperImpl helperImpl, DocumentHandler parentHandler,
                           TaskContainer container,
                           RuntimeConfigurable parentWrapper, Target target) {
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
                //TODO task.setTaskType(tag);
                task.setTaskName(tag);
            }
            task.setLocation(new Location(helperImpl.locator));
            helperImpl.configureId(task, attrs);

            task.setOwningTarget(target);
            container.addTask(task);
            task.init();
            wrapper = task.getRuntimeConfigurableWrapper();
            wrapper.setAttributes(attrs);
            if (parentWrapper != null) {
                parentWrapper.addChild(wrapper);
            }
        }

        /**
         * Adds text to the task, using the wrapper.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         */
        public void characters(char[] buf, int start, int count) {
            wrapper.addText(buf, start, count);
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
                new TaskHandler(helperImpl, this, (TaskContainer) task, wrapper, target).init(name,
                        attrs);
            } else {
                new NestedElementHandler(helperImpl, this, task, wrapper, target).init(name, attrs);
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
         *                      Must not be <code>null</code>.
         *
         * @param target        Target this element is part of.
         *                      Must not be <code>null</code>.
         */
        public NestedElementHandler(ProjectHelperImpl helperImpl,
                                    DocumentHandler parentHandler,
                                    Object parent,
                                    RuntimeConfigurable parentWrapper,
                                    Target target) {
            super(helperImpl, parentHandler);

            if (parent instanceof TypeAdapter) {
                this.parent = ((TypeAdapter) parent).getProxy();
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
            Class<?> parentClass = parent.getClass();
            IntrospectionHelper ih = IntrospectionHelper.getHelper(helperImpl.project, parentClass);

            try {
                String elementName = propType.toLowerCase(Locale.ENGLISH);
                if (parent instanceof UnknownElement) {
                    UnknownElement uc = new UnknownElement(elementName);
                    uc.setProject(helperImpl.project);
                    ((UnknownElement) parent).addChild(uc);
                    child = uc;
                } else {
                    child = ih.createElement(helperImpl.project, parent, elementName);
                }
                helperImpl.configureId(child, attrs);

                childWrapper = new RuntimeConfigurable(child, propType);
                childWrapper.setAttributes(attrs);
                parentWrapper.addChild(childWrapper);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), helperImpl.locator, exc);
            }
        }

        /**
         * Adds text to the element, using the wrapper.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         */
        public void characters(char[] buf, int start, int count) {
            childWrapper.addText(buf, start, count);
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
                new TaskHandler(helperImpl, this, (TaskContainer) child, childWrapper, target)
                        .init(name, attrs);
            } else {
                new NestedElementHandler(helperImpl, this, child, childWrapper, target).init(name,
                        attrs);
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
         * Constructor with a target specified.
         *
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         *
         * @param target The parent target of this element.
         *               Must not be <code>null</code>.
         */
        public DataTypeHandler(ProjectHelperImpl helperImpl, DocumentHandler parentHandler,
                Target target) {
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
                wrapper = new RuntimeConfigurable(element, propType);
                wrapper.setAttributes(attrs);
                target.addDataType(wrapper);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), helperImpl.locator, exc);
            }
        }

        /**
         * Adds text to the using the wrapper.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         *
         * @see ProjectHelper#addText(Project,Object,char[],int,int)
         */
        public void characters(char[] buf, int start, int count) {
            wrapper.addText(buf, start, count);
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
