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
package org.apache.tools.ant;

import java.io.File;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.LoaderUtils;
import org.xml.sax.AttributeList;

/**
 * Configures a Project (complete with Targets and Tasks) based on
 * a build file. It'll rely on a plugin to do the actual processing
 * of the file.
 * <p>
 * This class also provide static wrappers for common introspection.
 */
public class ProjectHelper {
    /** The URI for ant name space */
    public static final String ANT_CORE_URI = MagicNames.ANTLIB_PREFIX
            + MagicNames.ANT_CORE_PACKAGE;

    /** The URI for antlib current definitions */
    public static final String ANT_CURRENT_URI      = "ant:current";

    /** The URI for ant specific attributes
     * @since Ant 1.9.1
     */
    public static final String ANT_ATTRIBUTE_URI      = "ant:attribute";

    /**
     * The URI for defined types/tasks - the format is antlib:&lt;package&gt;
     * @deprecated use MagicNames.ANTLIB_PREFIX
     */
    @Deprecated
    public static final String ANTLIB_URI = MagicNames.ANTLIB_PREFIX;

    /** Polymorphic attribute  */
    public static final String ANT_TYPE = "ant-type";

    /**
     * Name of JVM system property which provides the name of the
     * ProjectHelper class to use.
     * @deprecated use MagicNames.PROJECT_HELPER_CLASS
     */
    @Deprecated
    public static final String HELPER_PROPERTY = MagicNames.PROJECT_HELPER_CLASS;

    /**
     * The service identifier in jars which provide Project Helper
     * implementations.
     * @deprecated use MagicNames.PROJECT_HELPER_SERVICE
     */
    @Deprecated
    public static final String SERVICE_ID = MagicNames.PROJECT_HELPER_SERVICE;

    /**
     * name of project helper reference that we add to a project
     * @deprecated use MagicNames.REFID_PROJECT_HELPER
     */
    @Deprecated
    public static final String PROJECTHELPER_REFERENCE = MagicNames.REFID_PROJECT_HELPER;

    /**
     * constant to denote use project name as target prefix
     * @since Ant 1.9.1
     */
    public static final String USE_PROJECT_NAME_AS_TARGET_PREFIX = "USE_PROJECT_NAME_AS_TARGET_PREFIX";

    /**
     * Configures the project with the contents of the specified build file.
     *
     * @param project The project to configure. Must not be <code>null</code>.
     * @param buildFile A build file giving the project's configuration.
     *                  Must not be <code>null</code>.
     *
     * @exception BuildException if the configuration is invalid or cannot be read
     */
    public static void configureProject(Project project, File buildFile) throws BuildException {
        FileResource resource = new FileResource(buildFile);
        ProjectHelper helper = ProjectHelperRepository.getInstance().getProjectHelperForBuildFile(resource);
        project.addReference(MagicNames.REFID_PROJECT_HELPER, helper);
        helper.parse(project, buildFile);
    }

    /**
     * Possible value for target's onMissingExtensionPoint attribute. It determines how to deal with
     * targets that want to extend missing extension-points.
     * <p>
     * This class behaves like a Java 1.5 Enum class.
     *
     * @since 1.8.2
     */
    public static final class OnMissingExtensionPoint {

        /** fail if the extension-point is not defined */
        public static final OnMissingExtensionPoint FAIL = new OnMissingExtensionPoint(
                "fail");

        /** warn if the extension-point is not defined */
        public static final OnMissingExtensionPoint WARN = new OnMissingExtensionPoint(
                "warn");

        /** ignore the extensionOf attribute if the extension-point is not defined */
        public static final OnMissingExtensionPoint IGNORE = new OnMissingExtensionPoint(
                "ignore");

        private static final OnMissingExtensionPoint[] values = new OnMissingExtensionPoint[] {
                                FAIL, WARN, IGNORE };

        private final String name;

        private OnMissingExtensionPoint(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public String toString() {
            return name;
        }

        public static OnMissingExtensionPoint valueOf(String name) {
            if (name == null) {
                throw new NullPointerException();
            }
            for (OnMissingExtensionPoint value : values) {
                if (name.equals(value.name())) {
                    return value;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown onMissingExtensionPoint " + name);
        }
    }

    // -------------------- Common properties  --------------------
    // The following properties are required by import (and other tasks
    // that read build files using ProjectHelper).

    private Vector<Object> importStack = new Vector<>();
    private List<String[]> extensionStack = new LinkedList<>();

    /**
     *  Import stack.
     *  Used to keep track of imported files. Error reporting should
     *  display the import path.
     *
     * @return the stack of import source objects.
     */
    public Vector<Object> getImportStack() {
        return importStack;
    }

    /**
     * Extension stack.
     * Used to keep track of targets that extend extension points.
     *
     * @return a list of three element string arrays where the first
     * element is the name of the extensionpoint, the second the name
     * of the target and the third the name of the enum like class
     * {@link OnMissingExtensionPoint}.
     */
    public List<String[]> getExtensionStack() {
        return extensionStack;
    }

    private static final ThreadLocal<String> targetPrefix = new ThreadLocal<>();

    /**
     * The prefix to prepend to imported target names.
     *
     * <p>May be set by &lt;import&gt;'s as attribute.</p>
     *
     * @return the configured prefix or null
     *
     * @since Ant 1.8.0
     */
    public static String getCurrentTargetPrefix() {
        return targetPrefix.get();
    }

    /**
     * Sets the prefix to prepend to imported target names.
     *
     * @param prefix String
     * @since Ant 1.8.0
     */
    public static void setCurrentTargetPrefix(String prefix) {
        targetPrefix.set(prefix);
    }

    private static final ThreadLocal<String> prefixSeparator = ThreadLocal.withInitial(() -> ".");

    /**
     * The separator between the prefix and the target name.
     *
     * <p>May be set by &lt;import&gt;'s prefixSeparator attribute.</p>
     *
     * @return String
     * @since Ant 1.8.0
     */
    public static String getCurrentPrefixSeparator() {
        return prefixSeparator.get();
    }

    /**
     * Sets the separator between the prefix and the target name.
     *
     * @param sep String
     * @since Ant 1.8.0
     */
    public static void setCurrentPrefixSeparator(String sep) {
        prefixSeparator.set(sep);
    }

    private static final ThreadLocal<Boolean> inIncludeMode = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Whether the current file should be read in include as opposed
     * to import mode.
     *
     * <p>In include mode included targets are only known by their
     * prefixed names and their depends lists get rewritten so that
     * all dependencies get the prefix as well.</p>
     *
     * <p>In import mode imported targets are known by an adorned as
     * well as a prefixed name and the unadorned target may be
     * overwritten in the importing build file.  The depends list of
     * the imported targets is not modified at all.</p>
     *
     * @return boolean
     * @since Ant 1.8.0
     */
    public static boolean isInIncludeMode() {
        return Boolean.TRUE.equals(inIncludeMode.get());
    }

    /**
     * Sets whether the current file should be read in include as
     * opposed to import mode.
     *
     * @param includeMode boolean
     * @since Ant 1.8.0
     */
    public static void setInIncludeMode(boolean includeMode) {
        inIncludeMode.set(includeMode);
    }

    // --------------------  Parse method  --------------------
    /**
     * Parses the project file, configuring the project as it goes.
     *
     * @param project The project for the resulting ProjectHelper to configure.
     *                Must not be <code>null</code>.
     * @param source The source for XML configuration. A helper must support
     *               at least File, for backward compatibility. Helpers may
     *               support URL, InputStream, etc or specialized types.
     *
     * @since Ant1.5
     * @exception BuildException if the configuration is invalid or cannot
     *                           be read
     */
    public void parse(Project project, Object source) throws BuildException {
        throw new BuildException("ProjectHelper.parse() must be implemented "
            + "in a helper plugin " + this.getClass().getName());
    }

    /**
     * Get the first project helper found in the classpath
     *
     * @return an project helper, never <code>null</code>
     * @see org.apache.tools.ant.ProjectHelperRepository#getHelpers()
     */
    public static ProjectHelper getProjectHelper() {
        return ProjectHelperRepository.getInstance().getHelpers().next();
    }

    /**
     * JDK1.1 compatible access to the context class loader. Cut &amp; paste from JAXP.
     *
     * @deprecated since 1.6.x.
     *             Use LoaderUtils.getContextClassLoader()
     *
     * @return the current context class loader, or <code>null</code>
     * if the context class loader is unavailable.
     */
    @Deprecated
    public static ClassLoader getContextClassLoader() {
        return LoaderUtils.isContextLoaderAvailable() ? LoaderUtils.getContextClassLoader() : null;
    }

    // -------------------- Static utils, used by most helpers ----------------

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
     * @deprecated since 1.6.x.
     *             Use IntrospectionHelper for each property.
     *
     * @exception BuildException if any of the attributes can't be handled by
     *                           the target
     */
    @Deprecated
    public static void configure(Object target, AttributeList attrs,
                                 Project project) throws BuildException {
        if (target instanceof TypeAdapter) {
            target = ((TypeAdapter) target).getProxy();
        }
        IntrospectionHelper ih = IntrospectionHelper.getHelper(project, target.getClass());

        for (int i = 0, length = attrs.getLength(); i < length; i++) {
            // reflect these into the target
            String value = replaceProperties(project, attrs.getValue(i), project.getProperties());
            try {
                ih.setAttribute(project, target, attrs.getName(i).toLowerCase(Locale.ENGLISH), value);
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
    public static void addText(Project project, Object target, char[] buf,
        int start, int count) throws BuildException {
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

        if (text == null) {
            return;
        }
        if (target instanceof TypeAdapter) {
            target = ((TypeAdapter) target).getProxy();
        }
        IntrospectionHelper.getHelper(project, target.getClass()).addText(project, target, text);
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
        IntrospectionHelper ih = IntrospectionHelper.getHelper(project, parent.getClass());
        ih.storeElement(project, parent, child, tag);
    }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value with
     * the string value of the corresponding properties.
     *
     * @param project The project containing the properties to replace.
     *                Must not be <code>null</code>.
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
     * @deprecated since 1.6.x.
     *             Use project.replaceProperties().
     * @since 1.5
     */
    @Deprecated
     public static String replaceProperties(Project project, String value) throws BuildException {
        // needed since project properties are not accessible
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
     * @param keys  Mapping (String to Object) of property names to their
     *              values. Must not be <code>null</code>.
     *
     * @exception BuildException if the string contains an opening
     *                           <code>${</code> without a closing
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     * @deprecated since 1.6.x.
     *             Use PropertyHelper.
     */
    @Deprecated
     public static String replaceProperties(Project project, String value, Hashtable<String, Object> keys)
             throws BuildException {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(project);
        return ph.replaceProperties(null, value, keys);
    }

    /**
     * Parses a string containing <code>${xxx}</code> style property
     * references into two lists. The first list is a collection
     * of text fragments, while the other is a set of string property names.
     * <code>null</code> entries in the first list indicate a property
     * reference from the second list.
     *
     * <p>As of Ant 1.8.0 this method is never invoked by any code
     * inside of Ant itself.</p>
     *
     * @param value     Text to parse. Must not be <code>null</code>.
     * @param fragments List to add text fragments to.
     *                  Must not be <code>null</code>.
     * @param propertyRefs List to add property names to.
     *                     Must not be <code>null</code>.
     *
     * @deprecated since 1.6.x.
     *             Use PropertyHelper.
     * @exception BuildException if the string contains an opening
     *                           <code>${</code> without a closing <code>}</code>
     */
    @Deprecated
    public static void parsePropertyString(String value, Vector<String> fragments, Vector<String> propertyRefs)
            throws BuildException {
        PropertyHelper.parsePropertyStringDefault(value, fragments, propertyRefs);
    }

    /**
     * Map a namespaced {uri,name} to an internal string format.
     * For BC purposes the names from the ant core uri will be
     * mapped to "name", other names will be mapped to
     * uri + ":" + name.
     * @param uri   The namespace URI
     * @param name  The localname
     * @return      The stringified form of the ns name
     */
    public static String genComponentName(String uri, String name) {
        if (uri == null || uri.isEmpty() || uri.equals(ANT_CORE_URI)) {
            return name;
        }
        return uri + ":" + name;
    }

    /**
     * extract a uri from a component name
     *
     * @param componentName  The stringified form for {uri, name}
     * @return               The uri or "" if not present
     */
    public static String extractUriFromComponentName(String componentName) {
        if (componentName == null) {
            return "";
        }
        int index = componentName.lastIndexOf(':');
        if (index == -1) {
            return "";
        }
        return componentName.substring(0, index);
    }

    /**
     * extract the element name from a component name
     *
     * @param componentName  The stringified form for {uri, name}
     * @return               The element name of the component
     */
    public static String extractNameFromComponentName(String componentName) {
        int index = componentName.lastIndexOf(':');
        if (index == -1) {
            return componentName;
        }
        return componentName.substring(index + 1);
    }

    /**
     * Convert an attribute namespace to a "component name".
     * @param ns the xml namespace uri.
     * @return the converted value.
     * @since Ant 1.9.1
     */
    public static String nsToComponentName(String ns) {
        return "attribute namespace:" + ns;
    }

    /**
     * Add location to build exception.
     * @param ex the build exception, if the build exception
     *           does not include
     * @param newLocation the location of the calling task (may be null)
     * @return a new build exception based in the build exception with
     *         location set to newLocation. If the original exception
     *         did not have a location, just return the build exception
     */
    public static BuildException addLocationToBuildException(
            BuildException ex, Location newLocation) {
        if (ex.getLocation() == null || ex.getMessage() == null) {
            return ex;
        }
        String errorMessage
            = String.format("The following error occurred while executing this line:%n%s%s",
                ex.getLocation().toString(), ex.getMessage());
        if (ex instanceof ExitStatusException) {
            int exitStatus = ((ExitStatusException) ex).getStatus();
            if (newLocation == null) {
                return new ExitStatusException(errorMessage, exitStatus);
            }
            return new ExitStatusException(errorMessage, exitStatus, newLocation);
        }
        if (newLocation == null) {
            return new BuildException(errorMessage, ex);
        }
        return new BuildException(errorMessage, ex, newLocation);
    }

    /**
     * Whether this instance of ProjectHelper can parse an Antlib
     * descriptor given by the URL and return its content as an
     * UnknownElement ready to be turned into an Antlib task.
     *
     * <p>This method should not try to parse the content of the
     * descriptor, the URL is only given as an argument to allow
     * subclasses to decide whether they can support a given URL
     * scheme or not.</p>
     *
     * <p>Subclasses that return true in this method must also
     * override {@link #parseAntlibDescriptor
     * parseAntlibDescriptor}.</p>
     *
     * <p>This implementation returns false.</p>
     *
     * @param r Resource
     * @return boolean
     * @since Ant 1.8.0
     */
    public boolean canParseAntlibDescriptor(Resource r) {
        return false;
    }

    /**
     * Parse the given URL as an antlib descriptor and return the
     * content as something that can be turned into an Antlib task.
     *
     * @param containingProject Project
     * @param source Resource
     * @return UnknownElement
     * @since ant 1.8.0
     */
    public UnknownElement parseAntlibDescriptor(Project containingProject,
                                                Resource source) {
        throw new BuildException("can't parse antlib descriptors");
    }

    /**
     * Check if the helper supports the kind of file. Some basic check on the
     * extension's file should be done here.
     *
     * @param buildFile
     *            the file expected to be parsed (never <code>null</code>)
     * @return true if the helper supports it
     * @since Ant 1.8.0
     */
    public boolean canParseBuildFile(Resource buildFile) {
        return true;
    }

    /**
     * The file name of the build script to be parsed if none specified on the command line
     *
     * @return the name of the default file (never <code>null</code>)
     * @since Ant 1.8.0
     */
    public String getDefaultBuildFile() {
        return Main.DEFAULT_BUILD_FILENAME;
    }

    /**
     * Check extensionStack and inject all targets having extensionOf attributes
     * into extensionPoint.
     * <p>
     * This method allow you to defer injection and have a powerful control of
     * extensionPoint wiring.
     * </p>
     * <p>
     * This should be invoked by each concrete implementation of ProjectHelper
     * when the root "buildfile" and all imported/included buildfile are loaded.
     * </p>
     *
     * @param project The project containing the target. Must not be
     *            <code>null</code>.
     * @exception BuildException if OnMissingExtensionPoint.FAIL and
     *                extensionPoint does not exist
     * @see OnMissingExtensionPoint
     * @since 1.9
     */
    public void resolveExtensionOfAttributes(Project project)
            throws BuildException {
        for (String[] extensionInfo : getExtensionStack()) {
            String extPointName = extensionInfo[0];
            String targetName = extensionInfo[1];
            OnMissingExtensionPoint missingBehaviour = OnMissingExtensionPoint.valueOf(extensionInfo[2]);
            // if the file has been included or imported, it may have a prefix
            // we should consider when trying to resolve the target it is
            // extending
            String prefixAndSep = extensionInfo.length > 3 ? extensionInfo[3] : null;

            // find the target we're extending
            Hashtable<String, Target> projectTargets = project.getTargets();
            Target extPoint = null;
            if (prefixAndSep == null) {
                // no prefix - not from an imported/included build file
                extPoint = projectTargets.get(extPointName);
            } else {
                // we have a prefix, which means we came from an include/import

                // FIXME: here we handle no particular level of include. We try
                // the fully prefixed name, and then the non-prefixed name. But
                // there might be intermediate project in the import stack,
                // which prefix should be tested before testing the non-prefix
                // root name.

                extPoint = projectTargets.get(prefixAndSep + extPointName);
                if (extPoint == null) {
                    extPoint = projectTargets.get(extPointName);
                }
            }

            // make sure we found a point to extend on
            if (extPoint == null) {
                String message = "can't add target " + targetName
                        + " to extension-point " + extPointName
                        + " because the extension-point is unknown.";
                if (missingBehaviour == OnMissingExtensionPoint.FAIL) {
                    throw new BuildException(message);
                } else if (missingBehaviour == OnMissingExtensionPoint.WARN) {
                    Target t = projectTargets.get(targetName);
                    project.log(t, "Warning: " + message, Project.MSG_WARN);
                }
            } else {
                if (!(extPoint instanceof ExtensionPoint)) {
                    throw new BuildException("referenced target " + extPointName
                            + " is not an extension-point");
                }
                extPoint.addDependency(targetName);
            }
        }
    }
}
