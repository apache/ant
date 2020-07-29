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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExtensionPoint;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.URLProvider;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.apache.tools.zip.ZipFile;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Sax2 based project reader
 *
 */
public class ProjectHelper2 extends ProjectHelper {

    /** Reference holding the (ordered) target Vector */
    public static final String REFID_TARGETS = "ant.targets";

    /* Stateless */

    // singletons - since all state is in the context
    private static AntHandler elementHandler = new ElementHandler();
    private static AntHandler targetHandler = new TargetHandler();
    private static AntHandler mainHandler = new MainHandler();
    private static AntHandler projectHandler = new ProjectHandler();

    /** Specific to ProjectHelper2 so not a true Ant "magic name:" */
    private static final String REFID_CONTEXT = "ant.parsing.context";

    /**
     * helper for path -> URI and URI -> path conversions.
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Whether this instance of ProjectHelper can parse an Antlib
     * descriptor given by the URL and return its content as an
     * UnknownElement ready to be turned into an Antlib task.
     *
     * <p>This implementation returns true.</p>
     *
     * @since Ant 1.8.0
     */
    @Override
    public boolean canParseAntlibDescriptor(Resource resource) {
        return true;
    }

    /**
     * Parse the given URL as an antlib descriptor and return the
     * content as something that can be turned into an Antlib task.
     *
     * <p>simply delegates to {@link #parseUnknownElement
     * parseUnknownElement} if the resource provides an URL and throws
     * an exception otherwise.</p>
     *
     * @since Ant 1.8.0
     */
    @Override
    public UnknownElement parseAntlibDescriptor(Project containingProject,
                                                Resource resource) {
        URLProvider up = resource.as(URLProvider.class);
        if (up == null) {
            throw new BuildException("Unsupported resource type: " + resource);
        }
        return parseUnknownElement(containingProject, up.getURL());
    }

    /**
     * Parse an unknown element from a url
     *
     * @param project the current project
     * @param source  the url containing the task
     * @return a configured task
     * @exception BuildException if an error occurs
     */
    public UnknownElement parseUnknownElement(Project project, URL source)
        throws BuildException {
        Target dummyTarget = new Target();
        dummyTarget.setProject(project);

        AntXMLContext context = new AntXMLContext(project);
        context.addTarget(dummyTarget);
        context.setImplicitTarget(dummyTarget);

        parse(context.getProject(), source, new RootHandler(context, elementHandler));
        Task[] tasks = dummyTarget.getTasks();
        if (tasks.length != 1) {
            throw new BuildException("No tasks defined");
        }
        return (UnknownElement) tasks[0];
    }

    /**
     * Parse a source xml input.
     *
     * @param project the current project
     * @param source  the xml source
     * @exception BuildException if an error occurs
     */
    @Override
    public void parse(Project project, Object source) throws BuildException {
        getImportStack().addElement(source);
        AntXMLContext context = null;
        context = project.getReference(REFID_CONTEXT);
        if (context == null) {
            context = new AntXMLContext(project);
            project.addReference(REFID_CONTEXT, context);
            project.addReference(REFID_TARGETS, context.getTargets());
        }
        if (getImportStack().size() > 1) {
            // we are in an imported file.
            context.setIgnoreProjectTag(true);
            Target currentTarget = context.getCurrentTarget();
            Target currentImplicit = context.getImplicitTarget();
            Map<String, Target>    currentTargets = context.getCurrentTargets();
            try {
                Target newCurrent = new Target();
                newCurrent.setProject(project);
                newCurrent.setName("");
                context.setCurrentTarget(newCurrent);
                context.setCurrentTargets(new HashMap<>());
                context.setImplicitTarget(newCurrent);
                parse(project, source, new RootHandler(context, mainHandler));
                newCurrent.execute();
            } finally {
                context.setCurrentTarget(currentTarget);
                context.setImplicitTarget(currentImplicit);
                context.setCurrentTargets(currentTargets);
            }
        } else {
            // top level file
            context.setCurrentTargets(new HashMap<>());
            parse(project, source, new RootHandler(context, mainHandler));
            // Execute the top-level target
            context.getImplicitTarget().execute();

            // resolve extensionOf attributes
            resolveExtensionOfAttributes(project);
        }
    }

    /**
     * Parses the project file, configuring the project as it goes.
     *
     * @param project the current project
     * @param source  the xml source
     * @param handler the root handler to use (contains the current context)
     * @exception BuildException if the configuration is invalid or cannot
     *                           be read
     */
    public void parse(Project project, Object source, RootHandler handler) throws BuildException {

        AntXMLContext context = handler.context;

        File buildFile = null;
        URL  url = null;
        String buildFileName = null;

        if (source instanceof File) {
            buildFile = (File) source;
        } else if (source instanceof URL) {
            url = (URL) source;
        } else if (source instanceof Resource) {
            FileProvider fp = ((Resource) source).as(FileProvider.class);
            if (fp != null) {
                buildFile = fp.getFile();
            } else {
                URLProvider up = ((Resource) source).as(URLProvider.class);
                if (up != null) {
                    url = up.getURL();
                }
            }
        }
        if (buildFile != null) {
            buildFile = FILE_UTILS.normalize(buildFile.getAbsolutePath());
            context.setBuildFile(buildFile);
            buildFileName = buildFile.toString();
        } else if (url != null) {
            try {
                context.setBuildFile((File) null);
                context.setBuildFile(url);
            } catch (MalformedURLException ex) {
                throw new BuildException(ex);
            }
            buildFileName = url.toString();
        } else {
            throw new BuildException("Source " + source.getClass().getName()
                                     + " not supported by this plugin");
        }
        InputStream inputStream = null;
        InputSource inputSource = null;
        ZipFile zf = null;

        try {
            /**
             * SAX 2 style parser used to parse the given file.
             */
            XMLReader parser = JAXPUtils.getNamespaceXMLReader();

            String uri = null;
            if (buildFile != null) {
                uri = FILE_UTILS.toURI(buildFile.getAbsolutePath());
                inputStream = Files.newInputStream(buildFile.toPath());
            } else {
                uri = url.toString();
                int pling = uri.indexOf("!/");
                if (uri.startsWith("jar:file") && pling > -1) {
                    zf = new ZipFile(org.apache.tools.ant.launch.Locator
                                     .fromJarURI(uri), "UTF-8");
                    inputStream =
                        zf.getInputStream(zf.getEntry(uri.substring(pling + 2)));
                } else {
                    URLConnection conn = url.openConnection();
                    conn.setUseCaches(false);
                    inputStream = conn.getInputStream();
                }
            }

            inputSource = new InputSource(inputStream);
            if (uri != null) {
                inputSource.setSystemId(uri);
            }
            project.log("parsing buildfile " + buildFileName + " with URI = "
                        + uri + (zf != null ? " from a zip file" : ""),
                        Project.MSG_VERBOSE);

            parser.setContentHandler(handler);
            parser.setEntityResolver(handler);
            parser.setErrorHandler(handler);
            parser.setDTDHandler(handler);
            parser.parse(inputSource);
        } catch (SAXParseException exc) {
            Location location = new Location(exc.getSystemId(), exc.getLineNumber(), exc
                                             .getColumnNumber());

            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                BuildException be = (BuildException) t;
                if (be.getLocation() == Location.UNKNOWN_LOCATION) {
                    be.setLocation(location);
                }
                throw be;
            }
            throw new BuildException(exc.getMessage(), t == null ? exc : t, location);
        } catch (SAXException exc) {
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(exc.getMessage(), t == null ? exc : t);
        } catch (FileNotFoundException exc) {
            throw new BuildException(exc);
        } catch (UnsupportedEncodingException exc) {
            throw new BuildException("Encoding of project file " + buildFileName + " is invalid.",
                                     exc);
        } catch (IOException exc) {
            throw new BuildException("Error reading project file " + buildFileName + ": "
                                     + exc.getMessage(), exc);
        } finally {
            FileUtils.close(inputStream);
            ZipFile.closeQuietly(zf);
        }
    }

    /**
     * Returns main handler
     * @return main handler
     */
    protected static AntHandler getMainHandler() {
        return mainHandler;
    }

    /**
     * Sets main handler
     * @param handler  new main handler
     */
    protected static void setMainHandler(AntHandler handler) {
        mainHandler = handler;
    }

    /**
     * Returns project handler
     * @return project handler
     */
    protected static AntHandler getProjectHandler() {
        return projectHandler;
    }

    /**
     * Sets project handler
     * @param handler  new project handler
     */
    protected static void setProjectHandler(AntHandler handler) {
        projectHandler = handler;
    }

    /**
     * Returns target handler
     * @return target handler
     */
    protected static AntHandler getTargetHandler() {
        return targetHandler;
    }

    /**
     * Sets target handler
     * @param handler  new target handler
     */
    protected static void setTargetHandler(AntHandler handler) {
        targetHandler = handler;
    }

    /**
     * Returns element handler
     * @return element handler
     */
    protected static AntHandler getElementHandler() {
        return elementHandler;
    }

    /**
     * Sets element handler
     * @param handler  new element handler
     */
    protected static void setElementHandler(AntHandler handler) {
        elementHandler = handler;
    }

    /**
     * The common superclass for all SAX event handlers used to parse
     * the configuration file.
     *
     * The context will hold all state information. At each time
     * there is one active handler for the current element. It can
     * use onStartChild() to set an alternate handler for the child.
     */
    public static class AntHandler  {
        /**
         * Handles the start of an element. This base implementation does
         * nothing.
         *
         * @param uri the namespace URI for the tag
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param qname The qualified name of the element.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * @param context The context that this element is in.
         *
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void onStartElement(String uri, String tag, String qname, Attributes attrs,
                                   AntXMLContext context) throws SAXParseException {
        }

        /**
         * Handles the start of an element. This base implementation just
         * throws an exception - you must override this method if you expect
         * child elements.
         *
         * @param uri The namespace uri for this element.
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param qname The qualified name for this element.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * @param context The current context.
         * @return a handler (in the derived classes)
         *
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public AntHandler onStartChild(String uri, String tag, String qname, Attributes attrs,
                                       AntXMLContext context) throws SAXParseException {
            throw new SAXParseException("Unexpected element \"" + qname + " \"", context
                                        .getLocator());
        }

        /**
         * Handle the end of a element.
         *
         * @param uri the namespace uri of the element
         * @param tag the tag of the element
         * @param qname the qualified name of the element
         * @param context the current context
         * @exception SAXParseException if an error occurs
         */
        public void onEndChild(String uri, String tag, String qname, AntXMLContext context)
            throws SAXParseException {
        }

        /**
         * This method is called when this element and all elements nested into it have been
         * handled. I.e., this happens at the &lt;/end_tag_of_the_element&gt;.
         * @param uri the namespace uri for this element
         * @param tag the element name
         * @param context the current context
         */
        public void onEndElement(String uri, String tag, AntXMLContext context) {
        }

        /**
         * Handles text within an element. This base implementation just
         * throws an exception, you must override it if you expect content.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         * @param context The current context.
         *
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void characters(char[] buf, int start, int count, AntXMLContext context)
            throws SAXParseException {
            String s = new String(buf, start, count).trim();

            if (!s.isEmpty()) {
                throw new SAXParseException("Unexpected text \"" + s + "\"", context.getLocator());
            }
        }

        /**
         * Will be called every time a namespace is reached.
         * It'll verify if the ns was processed, and if not load the task definitions.
         * @param uri The namespace uri.
         */
        protected void checkNamespace(String uri) {
        }
    }

    /**
     * Handler for ant processing. Uses a stack of AntHandlers to
     * implement each element (the original parser used a recursive behavior,
     * with the implicit execution stack)
     */
    public static class RootHandler extends DefaultHandler {
        private Stack<AntHandler> antHandlers = new Stack<>();
        private AntHandler currentHandler = null;
        private AntXMLContext context;

        /**
         * Creates a new RootHandler instance.
         *
         * @param context The context for the handler.
         * @param rootHandler The handler for the root element.
         */
        public RootHandler(AntXMLContext context, AntHandler rootHandler) {
            currentHandler = rootHandler;
            antHandlers.push(currentHandler);
            this.context = context;
        }

        /**
         * Returns the current ant handler object.
         * @return the current ant handler.
         */
        public AntHandler getCurrentAntHandler() {
            return currentHandler;
        }

        /**
         * Resolves file: URIs relative to the build file.
         *
         * @param publicId The public identifier, or <code>null</code>
         *                 if none is available. Ignored in this
         *                 implementation.
         * @param systemId The system identifier provided in the XML
         *                 document. Will not be <code>null</code>.
         * @return an inputsource for this identifier
         */
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {

            context.getProject().log("resolving systemId: " + systemId, Project.MSG_VERBOSE);

            if (systemId.startsWith("file:")) {
                String path = FILE_UTILS.fromURI(systemId);

                File file = new File(path);
                if (!file.isAbsolute()) {
                    file = FILE_UTILS.resolveFile(context.getBuildFileParent(), path);
                    context.getProject().log(
                                             "Warning: '" + systemId + "' in " + context.getBuildFile()
                                             + " should be expressed simply as '" + path.replace('\\', '/')
                                             + "' for compliance with other XML tools", Project.MSG_WARN);
                }
                context.getProject().log("file=" + file, Project.MSG_DEBUG);
                try {
                    InputSource inputSource = new InputSource(Files.newInputStream(file.toPath()));
                    inputSource.setSystemId(FILE_UTILS.toURI(file.getAbsolutePath()));
                    return inputSource;
                } catch (IOException fne) {
                    context.getProject().log(file.getAbsolutePath() + " could not be found",
                                             Project.MSG_WARN);
                }

            }
            // use default if not file or file not found
            context.getProject().log("could not resolve systemId", Project.MSG_DEBUG);
            return null;
        }

        /**
         * Handles the start of a project element. A project handler is created
         * and initialised with the element name and attributes.
         *
         * @param uri The namespace uri for this element.
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param qname The qualified name for this element.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception org.xml.sax.SAXParseException if the tag given is not
         *                              <code>"project"</code>
         */
        @Override
        public void startElement(String uri, String tag, String qname, Attributes attrs)
            throws SAXParseException {
            AntHandler next = currentHandler.onStartChild(uri, tag, qname, attrs, context);
            antHandlers.push(currentHandler);
            currentHandler = next;
            currentHandler.onStartElement(uri, tag, qname, attrs, context);
        }

        /**
         * Sets the locator in the project helper for future reference.
         *
         * @param locator The locator used by the parser.
         *                Will not be <code>null</code>.
         */
        @Override
        public void setDocumentLocator(Locator locator) {
            context.setLocator(locator);
        }

        /**
         * Handles the end of an element. Any required clean-up is performed
         * by the onEndElement() method and then the original handler is restored to the parser.
         *
         * @param uri  The namespace URI for this element.
         * @param name The name of the element which is ending.
         *             Will not be <code>null</code>.
         * @param qName The qualified name for this element.
         *
         * @exception SAXException in case of error (not thrown in this implementation)
         */
        @Override
        public void endElement(String uri, String name, String qName) throws SAXException {
            currentHandler.onEndElement(uri, name, context);
            currentHandler = antHandlers.pop();
            if (currentHandler != null) {
                currentHandler.onEndChild(uri, name, qName, context);
            }
        }

        /**
         * Handle text within an element, calls currentHandler.characters.
         *
         * @param buf  A character array of the test.
         * @param start The start offset in the array.
         * @param count The number of characters to read.
         * @exception SAXParseException if an error occurs
         */
        @Override
        public void characters(char[] buf, int start, int count) throws SAXParseException {
            currentHandler.characters(buf, start, count, context);
        }

        /**
         * Start a namespace prefix to uri mapping
         *
         * @param prefix the namespace prefix
         * @param uri the namespace uri
         */
        @Override
        public void startPrefixMapping(String prefix, String uri) {
            context.startPrefixMapping(prefix, uri);
        }

        /**
         * End a namespace prefix to uri mapping
         *
         * @param prefix the prefix that is not mapped anymore
         */
        @Override
        public void endPrefixMapping(String prefix) {
            context.endPrefixMapping(prefix);
        }
    }

    /**
     * The main handler - it handles the &lt;project&gt; tag.
     *
     * @see org.apache.tools.ant.helper.ProjectHelper2.AntHandler
     */
    public static class MainHandler extends AntHandler {

        /**
         * Handle the project tag
         *
         * @param uri The namespace uri.
         * @param name The element tag.
         * @param qname The element qualified name.
         * @param attrs The attributes of the element.
         * @param context The current context.
         * @return The project handler that handles subelements of project
         * @exception SAXParseException if the qualified name is not "project".
         */
        @Override
        public AntHandler onStartChild(String uri, String name, String qname, Attributes attrs,
                                       AntXMLContext context) throws SAXParseException {
            if ("project".equals(name)
                && (uri.isEmpty() || uri.equals(ANT_CORE_URI))) {
                return ProjectHelper2.projectHandler;
            }
            if (name.equals(qname)) {
                throw new SAXParseException("Unexpected element \"{" + uri
                                            + "}" + name + "\" {" + ANT_CORE_URI + "}" + name, context.getLocator());
            }
            throw new SAXParseException("Unexpected element \"" + qname
                                        + "\" " + name, context.getLocator());
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
         * @param uri The namespace URI for this element.
         * @param tag Name of the element which caused this handler
         *            to be created. Should not be <code>null</code>.
         *            Ignored in this implementation.
         * @param qname The qualified name for this element.
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * @param context The current context.
         *
         * @exception SAXParseException if an unexpected attribute is
         *            encountered or if the <code>"default"</code> attribute
         *            is missing.
         */
        @Override
        public void onStartElement(String uri, String tag, String qname, Attributes attrs,
                                   AntXMLContext context) throws SAXParseException {
            String baseDir = null;
            boolean nameAttributeSet = false;

            Project project = context.getProject();
            // Set the location of the implicit target associated with the project tag
            context.getImplicitTarget().setLocation(new Location(context.getLocator()));

            /** TODO I really don't like this - the XML processor is still
             * too 'involved' in the processing. A better solution (IMO)
             * would be to create UE for Project and Target too, and
             * then process the tree and have Project/Target deal with
             * its attributes (similar with Description).
             *
             * If we eventually switch to (or add support for) DOM,
             * things will work smoothly - UE can be avoided almost completely
             * (it could still be created on demand, for backward compatibility)
             */

            for (int i = 0; i < attrs.getLength(); i++) {
                String attrUri = attrs.getURI(i);
                if (attrUri != null && !attrUri.isEmpty() && !attrUri.equals(uri)) {
                    continue; // Ignore attributes from unknown uris
                }
                String value = attrs.getValue(i);
                switch (attrs.getLocalName(i)) {
                    case "default":
                        if (value != null && !value.isEmpty()) {
                            if (!context.isIgnoringProjectTag()) {
                                project.setDefault(value);
                            }
                        }
                        break;
                    case "name":
                        if (value != null) {
                            context.setCurrentProjectName(value);
                            nameAttributeSet = true;
                            if (!context.isIgnoringProjectTag()) {
                                project.setName(value);
                                project.addReference(value, project);
                            } else if (isInIncludeMode()) {
                                if (!value.isEmpty() && getCurrentTargetPrefix() != null
                                        && getCurrentTargetPrefix().endsWith(ProjectHelper.USE_PROJECT_NAME_AS_TARGET_PREFIX)) {
                                    String newTargetPrefix = getCurrentTargetPrefix().replace(ProjectHelper.USE_PROJECT_NAME_AS_TARGET_PREFIX, value);
                                    // help nested include tasks
                                    setCurrentTargetPrefix(newTargetPrefix);
                                }
                            }
                        }
                        break;
                    case "id":
                        if (value != null) {
                            // What's the difference between id and name ?
                            if (!context.isIgnoringProjectTag()) {
                                project.addReference(value, project);
                            }
                        }
                        break;
                    case "basedir":
                        if (!context.isIgnoringProjectTag()) {
                            baseDir = value;
                        }
                        break;
                    default:
                        // TODO ignore attributes in a different NS (maybe store them ?)
                        throw new SAXParseException("Unexpected attribute \"" + attrs.getQName(i)
                                + "\"", context.getLocator());
                }
            }

            // TODO Move to Project (so it is shared by all helpers)
            String antFileProp =
                MagicNames.ANT_FILE + "." + context.getCurrentProjectName();
            String dup = project.getProperty(antFileProp);
            String typeProp =
                MagicNames.ANT_FILE_TYPE + "." + context.getCurrentProjectName();
            String dupType = project.getProperty(typeProp);
            if (dup != null && nameAttributeSet) {
                Object dupFile = null;
                Object contextFile = null;
                if (MagicNames.ANT_FILE_TYPE_URL.equals(dupType)) {
                    try {
                        dupFile = new URL(dup);
                    } catch (MalformedURLException mue) {
                        throw new BuildException("failed to parse "
                                                 + dup + " as URL while looking"
                                                 + " at a duplicate project"
                                                 + " name.", mue);
                    }
                    contextFile = context.getBuildFileURL();
                } else {
                    dupFile = new File(dup);
                    contextFile = context.getBuildFile();
                }

                if (context.isIgnoringProjectTag() && !dupFile.equals(contextFile)) {
                    project.log("Duplicated project name in import. Project "
                                + context.getCurrentProjectName() + " defined first in " + dup
                                + " and again in " + contextFile, Project.MSG_WARN);
                }
            }
            if (nameAttributeSet) {
                if (context.getBuildFile() != null) {
                    project.setUserProperty(antFileProp,
                                            context.getBuildFile().toString());
                    project.setUserProperty(typeProp,
                                            MagicNames.ANT_FILE_TYPE_FILE);
                } else if (context.getBuildFileURL() != null) {
                    project.setUserProperty(antFileProp,
                                            context.getBuildFileURL().toString());
                    project.setUserProperty(typeProp,
                                            MagicNames.ANT_FILE_TYPE_URL);
                }
            }
            if (context.isIgnoringProjectTag()) {
                // no further processing
                return;
            }
            // set explicitly before starting ?
            if (project.getProperty(MagicNames.PROJECT_BASEDIR) != null) {
                project.setBasedir(project.getProperty(MagicNames.PROJECT_BASEDIR));
            } else {
                // Default for baseDir is the location of the build file.
                if (baseDir == null) {
                    project.setBasedir(context.getBuildFileParent().getAbsolutePath());
                } else {
                    // check whether the user has specified an absolute path
                    if ((new File(baseDir)).isAbsolute()) {
                        project.setBasedir(baseDir);
                    } else {
                        project.setBaseDir(FILE_UTILS.resolveFile(context.getBuildFileParent(),
                                                                  baseDir));
                    }
                }
            }
            project.addTarget("", context.getImplicitTarget());
            context.setCurrentTarget(context.getImplicitTarget());
        }

        /**
         * Handles the start of a top-level element within the project. An
         * appropriate handler is created and initialised with the details
         * of the element.
         *
         * @param uri The namespace URI for this element.
         * @param name The name of the element being started.
         *            Will not be <code>null</code>.
         * @param qname The qualified name for this element.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * @param context The context for this element.
         * @return a target or an element handler.
         *
         * @exception org.xml.sax.SAXParseException if the tag given is not
         *            <code>"taskdef"</code>, <code>"typedef"</code>,
         *            <code>"property"</code>, <code>"target"</code>,
         *            <code>"extension-point"</code>
         *            or a data type definition
         */
        @Override
        public AntHandler onStartChild(String uri, String name, String qname, Attributes attrs,
                                       AntXMLContext context) throws SAXParseException {
            return ("target".equals(name) || "extension-point".equals(name))
                && (uri.isEmpty() || uri.equals(ANT_CORE_URI))
                ? ProjectHelper2.targetHandler : ProjectHelper2.elementHandler;
        }
    }

    /**
     * Handler for "target" and "extension-point" elements.
     */
    public static class TargetHandler extends AntHandler {

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. The attributes which
         * this handler can deal with are: <code>"name"</code>,
         * <code>"depends"</code>, <code>"if"</code>,
         * <code>"unless"</code>, <code>"id"</code> and
         * <code>"description"</code>.
         *
         * @param uri The namespace URI for this element.
         * @param tag Name of the element which caused this handler
         *            to be created. Should not be <code>null</code>.
         *            Ignored in this implementation.
         * @param qname The qualified name for this element.
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * @param context The current context.
         *
         * @exception SAXParseException if an unexpected attribute is encountered
         *            or if the <code>"name"</code> attribute is missing.
         */
        @Override
        public void onStartElement(String uri, String tag, String qname, Attributes attrs,
                                   AntXMLContext context) throws SAXParseException {
            String name = null;
            String depends = "";
            String extensionPoint = null;
            OnMissingExtensionPoint extensionPointMissing = null;

            Project project = context.getProject();
            Target target = "target".equals(tag)
                ? new Target() : new ExtensionPoint();
            target.setProject(project);
            target.setLocation(new Location(context.getLocator()));
            context.addTarget(target);

            for (int i = 0; i < attrs.getLength(); i++) {
                String attrUri = attrs.getURI(i);
                if (attrUri != null && !attrUri.isEmpty() && !attrUri.equals(uri)) {
                    continue; // Ignore attributes from unknown uris
                }
                String value = attrs.getValue(i);
                switch (attrs.getLocalName(i)) {
                    case "name":
                        name = value;
                        if (name.isEmpty()) {
                            throw new BuildException("name attribute must not be empty");
                        }
                        break;
                    case "depends":
                        depends = value;
                        break;
                    case "if":
                        target.setIf(value);
                        break;
                    case "unless":
                        target.setUnless(value);
                        break;
                    case "id":
                        if (value != null && !value.isEmpty()) {
                            context.getProject().addReference(value, target);
                        }
                        break;
                    case "description":
                        target.setDescription(value);
                        break;
                    case "extensionOf":
                        extensionPoint = value;
                        break;
                    case "onMissingExtensionPoint":
                        try {
                            extensionPointMissing = OnMissingExtensionPoint.valueOf(value);
                        } catch (IllegalArgumentException e) {
                            throw new BuildException("Invalid onMissingExtensionPoint " + value);
                        }
                        break;
                    default:
                        throw new SAXParseException("Unexpected attribute \"" + attrs.getQName(i)
                                + "\"", context.getLocator());
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without a name attribute",
                                            context.getLocator());
            }

            String prefix = null;
            boolean isInIncludeMode =
                context.isIgnoringProjectTag() && isInIncludeMode();
            String sep = getCurrentPrefixSeparator();

            if (isInIncludeMode) {
                prefix = getTargetPrefix(context);
                if (prefix == null) {
                    throw new BuildException("can't include build file "
                                             + context.getBuildFileURL()
                                             + ", no as attribute has been given"
                                             + " and the project tag doesn't"
                                             + " specify a name attribute");
                }
                name = prefix + sep + name;
            }

            // Check if this target is in the current build file
            if (context.getCurrentTargets().get(name) != null) {
                throw new BuildException("Duplicate target '" + name + "'",
                                         target.getLocation());
            }
            Hashtable<String, Target> projectTargets = project.getTargets();
            boolean usedTarget = false;
            // If the name has not already been defined define it
            if (projectTargets.containsKey(name)) {
                project.log("Already defined in main or a previous import, ignore " + name,
                            Project.MSG_VERBOSE);
            } else {
                target.setName(name);
                context.getCurrentTargets().put(name, target);
                project.addOrReplaceTarget(name, target);
                usedTarget = true;
            }

            if (!depends.isEmpty()) {
                if (!isInIncludeMode) {
                    target.setDepends(depends);
                } else {
                    for (String string : Target.parseDepends(depends, name, "depends")) {
                        target.addDependency(prefix + sep + string);
                   }
                }
            }
            if (!isInIncludeMode && context.isIgnoringProjectTag()
                && (prefix = getTargetPrefix(context)) != null) {
                // In an imported file (and not completely
                // ignoring the project tag or having a preconfigured prefix)
                String newName = prefix + sep + name;
                Target newTarget = target;
                if (usedTarget) {
                    newTarget = "target".equals(tag)
                            ? new Target(target) : new ExtensionPoint(target);
                }
                newTarget.setName(newName);
                context.getCurrentTargets().put(newName, newTarget);
                project.addOrReplaceTarget(newName, newTarget);
            }
            if (extensionPointMissing != null && extensionPoint == null) {
                throw new BuildException("onMissingExtensionPoint attribute cannot " +
                                         "be specified unless extensionOf is specified",
                                         target.getLocation());

            }
            if (extensionPoint != null) {
                ProjectHelper helper =
                        context.getProject().getReference(MagicNames.REFID_PROJECT_HELPER);
                for (String extPointName : Target.parseDepends(extensionPoint, name, "extensionOf")) {
                    if (extensionPointMissing == null) {
                        extensionPointMissing = OnMissingExtensionPoint.FAIL;
                    }
                    // defer extensionpoint resolution until the full
                    // import stack has been processed
                    if (isInIncludeMode()) {
                        // if in include mode, provide prefix we're including by
                        // so that we can try and resolve extension point from
                        // the local file first
                        helper.getExtensionStack().add(
                                new String[] {extPointName, target.getName(),
                                        extensionPointMissing.name(), prefix + sep});
                    } else {
                        helper.getExtensionStack().add(
                                new String[] {extPointName, target.getName(),
                                        extensionPointMissing.name()});
                    }
                }
            }
        }

        private String getTargetPrefix(AntXMLContext context) {
            String configuredValue = getCurrentTargetPrefix();
            if (configuredValue != null && configuredValue.isEmpty()) {
                configuredValue = null;
            }
            if (configuredValue != null) {
                return configuredValue;
            }

            String projectName = context.getCurrentProjectName();
            if (projectName != null && projectName.isEmpty()) {
                projectName = null;
            }

            return projectName;
        }

        /**
         * Handles the start of an element within a target.
         *
         * @param uri The namespace URI for this element.
         * @param name The name of the element being started.
         *            Will not be <code>null</code>.
         * @param qname The qualified name for this element.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * @param context The current context.
         * @return an element handler.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        @Override
        public AntHandler onStartChild(String uri, String name, String qname, Attributes attrs,
                                       AntXMLContext context) throws SAXParseException {
            return ProjectHelper2.elementHandler;
        }

        /**
         * Handle the end of the project, sets the current target of the
         * context to be the implicit target.
         *
         * @param uri The namespace URI of the element.
         * @param tag The name of the element.
         * @param context The current context.
         */
        @Override
        public void onEndElement(String uri, String tag, AntXMLContext context) {
            context.setCurrentTarget(context.getImplicitTarget());
        }
    }

    /**
     * Handler for all project elements (tasks, data types)
     */
    public static class ElementHandler extends AntHandler {

        /**
         * Constructor.
         */
        public ElementHandler() {
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         *
         * @param uri The namespace URI for this element.
         * @param tag Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         * @param qname The qualified name for this element.
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * @param context The current context.
         *
         * @exception SAXParseException in case of error (not thrown in
         *                              this implementation)
         */
        @Override
        public void onStartElement(String uri, String tag, String qname, Attributes attrs,
                                   AntXMLContext context) throws SAXParseException {
            RuntimeConfigurable parentWrapper = context.currentWrapper();
            Object parent = null;

            if (parentWrapper != null) {
                parent = parentWrapper.getProxy();
            }

            /* UnknownElement is used for tasks and data types - with
               delayed eval */
            UnknownElement task = new UnknownElement(tag);
            task.setProject(context.getProject());
            task.setNamespace(uri);
            task.setQName(qname);
            task.setTaskType(ProjectHelper.genComponentName(task.getNamespace(), tag));
            task.setTaskName(qname);

            Location location = new Location(context.getLocator().getSystemId(), context
                                             .getLocator().getLineNumber(), context.getLocator().getColumnNumber());
            task.setLocation(location);
            task.setOwningTarget(context.getCurrentTarget());

            if (parent != null) {
                // Nested element
                ((UnknownElement) parent).addChild(task);
            }  else {
                // Task included in a target (including the default one).
                context.getCurrentTarget().addTask(task);
            }

            context.configureId(task, attrs);

            // container.addTask(task);
            // This is a nop in UE: task.init();

            RuntimeConfigurable wrapper = new RuntimeConfigurable(task, task.getTaskName());

            for (int i = 0; i < attrs.getLength(); i++) {
                String name = attrs.getLocalName(i);
                String attrUri = attrs.getURI(i);
                if (attrUri != null && !attrUri.isEmpty() && !attrUri.equals(uri)) {
                    name = attrUri + ":" + attrs.getQName(i);
                }
                String value = attrs.getValue(i);
                // PR: Hack for ant-type value
                //  an ant-type is a component name which can
                // be namespaced, need to extract the name
                // and convert from qualified name to uri/name
                if (ANT_TYPE.equals(name)
                    || (ANT_CORE_URI.equals(attrUri)
                        && ANT_TYPE.equals(attrs.getLocalName(i)))) {
                    name = ANT_TYPE;
                    int index = value.indexOf(":");
                    if (index >= 0) {
                        String prefix = value.substring(0, index);
                        String mappedUri = context.getPrefixMapping(prefix);
                        if (mappedUri == null) {
                            throw new BuildException("Unable to find XML NS prefix \"" + prefix
                                                     + "\"");
                        }
                        value = ProjectHelper.genComponentName(mappedUri, value
                                                               .substring(index + 1));
                    }
                }
                wrapper.setAttribute(name, value);
            }
            if (parentWrapper != null) {
                parentWrapper.addChild(wrapper);
            }
            context.pushWrapper(wrapper);
        }

        /**
         * Adds text to the task, using the wrapper
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         * @param context The current context.
         *
         * @exception SAXParseException if the element doesn't support text
         *
         * @see ProjectHelper#addText(Project,java.lang.Object,char[],int,int)
         */
        @Override
        public void characters(char[] buf, int start, int count,
                               AntXMLContext context) throws SAXParseException {
            RuntimeConfigurable wrapper = context.currentWrapper();
            wrapper.addText(buf, start, count);
        }

        /**
         * Handles the start of an element within a target. Task containers
         * will always use another task handler, and all other tasks
         * will always use a nested element handler.
         *
         * @param uri The namespace URI for this element.
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param qname The qualified name for this element.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * @param context The current context.
         * @return The handler for elements.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        @Override
        public AntHandler onStartChild(String uri, String tag, String qname, Attributes attrs,
                                       AntXMLContext context) throws SAXParseException {
            return ProjectHelper2.elementHandler;
        }

        /**
         * Handles the end of the element. This pops the wrapper from
         * the context.
         *
         * @param uri The namespace URI for the element.
         * @param tag The name of the element.
         * @param context The current context.
         */
        @Override
        public void onEndElement(String uri, String tag, AntXMLContext context) {
            context.popWrapper();
        }
    }
}
