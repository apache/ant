/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 1999,2003 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.xml.sax.*;
import javax.xml.parsers.*;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;

/**
 * Make available the tasks and types from an Ant library. <pre>
 * &lt;antlib library="libname.jar" &gt;
 *   &lt;alias name="nameOnLib" as="newName" /&gt;
 * &lt;/antlib&gt;
 *
 * &lt;antlib file="libname.jar" override="true" /&gt;
 * </pre>
 *
 * @author minor changes by steve loughran, steve_l@iseran.com
 * @author <a href="j_a_fernandez@yahoo.com">Jose Alberto Fernandez</a>
 * @since ant1.5
 */
public class Antlib extends Task {

    /**
     * Location of descriptor in library
     */
    public static final String ANT_DESCRIPTOR = "META-INF/antlib.xml";

    /**
     * The named classloader to use.
     * Defaults to the default classLoader.
     */
    private String loaderId = "";

    /**
     * file attribute
     */
    private File file = null;
    /**
     * override attribute
     */
    private boolean override = false;
    /**
     * attribute to control failure when loading
     */
    private FailureAction onerror = new FailureAction();

    /**
     * classpath to build up
     */
    private Path classpath = null;


    /**
     * our little xml parse
     */
    private SAXParserFactory saxFactory;

    /**
     * table of aliases
     */
    private Vector aliases = new Vector();

    /**
     * Some internal constants.
     */
    private static final int FAIL = 0, REPORT = 1, IGNORE = 2;

    /**
     * Posible actions when classes are not found
     */
    public static class FailureAction extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{"fail", "report", "ignore"};
        }
    }

    private static class DescriptorEnumeration implements Enumeration {

        /**
         * The name of the resource being searched for.
         */
        private String resourceName;

        /**
         * The index of the next file to search.
         */
        private int index;

        /**
         * The list of files to search
         */
        private File files[];

        /**
         * The URL of the next resource to return in the enumeration. If this
         * field is <code>null</code> then the enumeration has been completed,
         * i.e., there are no more elements to return.
         */
        private URL nextDescriptor;

        /**
         * Construct a new enumeration of resources of the given name found
         * within this class loader's classpath.
         *
         * @param name the name of the resource to search for.
         */
        DescriptorEnumeration(String fileNames[], String name) {
            this.resourceName = name;
            this.index = 0;
            this.files = new File[fileNames.length];
            for (int i = 0; i < files.length; i++) {
                files[i] = new File(fileNames[i]);
            }
            findNextDescriptor();
        }

        /**
         * Indicates whether there are more elements in the enumeration to
         * return.
         *
         * @return <code>true</code> if there are more elements in the
         *         enumeration; <code>false</code> otherwise.
         */
        public boolean hasMoreElements() {
            return (this.nextDescriptor != null);
        }

        /**
         * Returns the next resource in the enumeration.
         *
         * @return the next resource in the enumeration.
         */
        public Object nextElement() {
            URL ret = this.nextDescriptor;
            findNextDescriptor();
            return ret;
        }

        /**
         * Locates the next descriptor of the correct name in the files and
         * sets <code>nextDescriptor</code> to the URL of that resource. If no
         * more resources can be found, <code>nextDescriptor</code> is set to
         * <code>null</code>.
         */
        private void findNextDescriptor() {
            URL url = null;
            while (index < files.length && url == null) {
                try {
                    url = getDescriptorURL(files[index], this.resourceName);
                    index++;
                }
                catch (BuildException e) {
                    // ignore path elements which are not valid relative to the
                    // project
                }
            }
            this.nextDescriptor = url;
        }

        /**
         * Get an URL to a given resource in the given file which may
         * either be a directory or a zip file.
         *
         * @param file the file (directory or jar) in which to search for
         *             the resource. Must not be <code>null</code>.
         * @param resourceName the name of the resource for which a URL
         *                     is required. Must not be <code>null</code>.
         *
         * @return a URL to the required resource or <code>null</code> if the
         *         resource cannot be found in the given file object
         * @todo This code is extracted from AntClassLoader.getResourceURL
         *       I hate when that happens but the code there is too tied to
         *       the ClassLoader internals. Maybe we can find a nice place
         *       to put it where both can use it.
         */
        private URL getDescriptorURL(File file, String resourceName) {
            try {
                if (!file.exists()) {
                    return null;
                }

                if (file.isDirectory()) {
                    File resource = new File(file, resourceName);

                    if (resource.exists()) {
                        try {
                            return new URL("file:"+resource.toString());
                        } catch (MalformedURLException ex) {
                            return null;
                        }
                    }
                }
                else {
                    ZipFile zipFile = new ZipFile(file);
                    try {
                        ZipEntry entry = zipFile.getEntry(resourceName);
                        if (entry != null) {
                            try {
                                return new URL("jar:file:"+file.toString()+"!/"+entry);
                            } catch (MalformedURLException ex) {
                                return null;
                            }
                        }
                    }
                    finally {
                        zipFile.close();
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

    }

    /**
     * constructor creates a validating sax parser
     */
    public Antlib() {
        super();
        // Default error action
        onerror.setValue("report");
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setValidating(false);
    }


    /**
     * constructor binds to a project and sets ignore mode on errors
     *
     * @param p Description of Parameter
     */
    public Antlib(Project p) {
        this();
        setProject(p);
    }


    /**
     * Set name of library to load. The library is located in $ANT_HOME/antlib.
     *
     * @param lib the name of library relative to $ANT_HOME/antlib.
     */
    public void setLibrary(String lib) {
        setFile(libraryFile("antlib", lib));
    }


    /**
     * Set file location of library to load.
     *
     * @param file the jar file for the library.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Set the ID of the ClassLoader to use for this library.
     *
     * @param id the id for the ClassLoader to use,
     *           <code>null</code> means use ANT's core classloader.
     */
    public void setLoaderid(String id) {
        this.loaderId = id;
    }

    /**
     * Set whether to override any existing definitions.
     *
     * @param override if true new definitions will replace existing ones.
     */
    public void setOverride(boolean override) {
        this.override = override;
    }


    /**
     * Get what to do if a definition cannot be loaded
     * This method is mostly used by the core when loading core tasks.
     *
     * @return what to do if a definition cannot be loaded
     */
    final protected FailureAction getOnerror() {
        return this.onerror;
    }


    /**
     * Set whether to fail if a definition cannot be loaded
     * Default is <code>true</code>.
     * This property is mostly used by the core when loading core tasks.
     *
     * @param onerror if true loading will stop if classes
     *                      cannot be instantiated
     */
    public void setOnerror(FailureAction onerror) {
        this.onerror = onerror;
    }


    /**
     * Create new Alias element.
     *
     * @return Description of the Returned Value
     */
    public Alias createAlias() {
        Alias als = new Alias();
        aliases.add(als);
        return als;
    }


    /**
     * Set the classpath to be used for this compilation
     *
     * @param cp The new Classpath value
     */
    public void setClasspath(Path cp) {
        if (classpath == null) {
            classpath = cp;
        }
        else {
            classpath.append(cp);
        }
    }


    /**
     * create a nested classpath element.
     *
     * @return  classpath to use
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }


    /**
     * Adds a reference to a CLASSPATH defined elsewhere
     *
     * @param r The new ClasspathRef value
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }


    /**
     * Obtain library file from ANT_HOME directory.
     *
     * @param lib the library name.
     * @return the File instance of the library
     */
    private File libraryFile(String homeSubDir, String lib) {
        // For the time being libraries live in $ANT_HOME/antlib.
        // The idea being that not to load all the jars there anymore
        String home = getProject().getProperty("ant.home");

        if (home == null) {
            throw new BuildException("ANT_HOME not set as required.");
        }

        return new File(new File(home, homeSubDir), lib);
    }

    /**
     * actually do the work of loading the library
     *
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException {
        if (file == null && classpath == null) {
            String msg =
                "Must specify either library or file attribute or classpath.";
            throw new BuildException(msg, getLocation());
        }
        if (file != null && !file.exists()) {
            String msg = "Cannot find library: " + file;
            throw new BuildException(msg, getLocation());
        }

        loadDefinitions();
    }


    /**
     * Load definitions in library and classpath
     *
     * @exception BuildException failure to access the resource
     */
    public boolean loadDefinitions() throws BuildException {
        return loadDefinitions(ANT_DESCRIPTOR);
    }

    /**
     * Load definitions from resource name in library and classpath
     *
     * @param res the name of the resources to load
     * @exception BuildException failure to access the resource
     */
    final protected boolean loadDefinitions(String res)
        throws BuildException {
        Path path = makeLoaderClasspath();
        ClassLoader cl = makeClassLoader(path);
        boolean found = false;
        try {
            for (Enumeration e = getDescriptors(path, res); e.hasMoreElements(); ) {
                URL resURL = (URL)e.nextElement();
                InputStream is = resURL.openStream();
                loadDefinitions(cl, is);
                found = true;
            }
            if (!found && onerror.getIndex() != IGNORE) {
                String sPath = path.toString();
                if ("".equals(sPath.trim())) {
                    sPath = System.getProperty("java.classpath");
                }
                String msg = "Cannot find any " + res +
                    " antlib descriptors in: " + sPath;
                switch (onerror.getIndex()) {
                case FAIL:
                    throw new BuildException(msg);
                case REPORT:
                    log(msg, Project.MSG_WARN);
                }
            }
        }
        catch (IOException io) {
            String msg = "Cannot load definitions from: " + res;
            switch (onerror.getIndex()) {
            case FAIL:
                throw new BuildException(msg, io);
            case REPORT:
                log(io.getMessage(), Project.MSG_WARN);
            }
        }
        return found;
    }


    /**
     * Load definitions directly from InputStream.
     *
     * @param is InputStream for the Antlib descriptor.
     * @exception BuildException trouble
     */
    private void loadDefinitions(ClassLoader cl, InputStream is)
        throws BuildException {
        evaluateDescriptor(cl, processAliases(), is);
    }


    /**
     * get an Enumeration of URLs for all resouces corresponding to the
     * descriptor name.
     *
     * @param res the name of the resource to collect
     * @return input stream to the Descriptor or null if none existent
     * @exception BuildException io trouble, or it isnt a zipfile
     */
    private Enumeration getDescriptors(Path path, final String res)
        throws BuildException, IOException {
        if (loaderId == null) {
            // Path cannot be added to the CoreLoader so simply
            // ask for all instances of the resource descriptors
            return getProject().getCoreLoader().getResources(res);
        }

        return new DescriptorEnumeration(path.list(), res);
    }


    /**
     * turn the alias list to a property hashtable
     *
     * @return generated property hashtable
     */
    private Properties processAliases() {
        Properties p = new Properties();

        for (Enumeration e = aliases.elements(); e.hasMoreElements(); ) {
            Alias a = (Alias) e.nextElement();
            p.put(a.name, a.as);
        }
        return p;
    }


    /**
     * create the classpath for this library from the file passed in and
     * any classpath parameters
     *
     * @param clspath library file to use
     * @return classloader using te
     * @exception BuildException trouble creating the classloader
     */
    protected ClassLoader makeClassLoader(Path clspath)
        throws BuildException {
        if (loaderId == null) {
            log("Loading definitions from CORE, <classpath> ignored",
                Project.MSG_VERBOSE);
            return getProject().getCoreLoader();
        }

        log("Using ClassLoader '" + loaderId + "' to load path: " + clspath,
            Project.MSG_VERBOSE);
        return getProject().addToLoader(loaderId, clspath);
    }


    /**
     * Constructs the Path to add to the ClassLoader
     */
    private Path makeLoaderClasspath()
    {
        Path clspath = new Path(getProject());
        if (file != null) clspath.setLocation(file);
        //append any build supplied classpath
        if (classpath != null) {
            clspath.append(classpath);
        }
        return clspath;
    }

    /**
     * parse the antlib descriptor
     *
     * @param cl optional classloader
     * @param als alias list as property hashtable
     * @param is input stream to descriptor
     * @exception BuildException trouble
     */
    protected void evaluateDescriptor(ClassLoader cl,
                                      Properties als, InputStream is)
        throws BuildException {
        try {
            SAXParser saxParser = saxFactory.newSAXParser();

            InputSource inputSource = new InputSource(is);
            //inputSource.setSystemId(uri); //URI is nasty for jar entries
            getProject().log("parsing descriptor for library: " + file,
                        Project.MSG_VERBOSE);
            saxParser.parse(inputSource, new AntLibraryHandler(cl, als));
        }
        catch (ParserConfigurationException exc) {
            throw new BuildException("Parser has not been configured correctly", exc);
        }
        catch (SAXParseException exc) {
            Location location =
                    new Location(ANT_DESCRIPTOR,
                    exc.getLineNumber(), exc.getColumnNumber());

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
        catch (SAXException exc) {
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(exc.getMessage(), t);
        }
        catch (IOException exc) {
            throw new BuildException("Error reading library descriptor", exc);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException ioe) {
                    // ignore this
                }
            }
        }
    }


    /**
     * Parses the document describing the content of the
     * library. An inner class for access to Project.log
     */
    private class AntLibraryHandler extends HandlerBase {

        /**
         * our classloader
         */
        private final ClassLoader classloader;
        /**
         * the aliases
         */
        private final Properties aliasMap;
        /**
         * doc locator
         */
        private Locator locator = null;

        private int level = 0;

        private String name = null;
        private String className = null;
        private String adapter = null;

        /**
         * Constructor for the AntLibraryHandler object
         *
         * @param classloader optional classloader
         * @param als alias list
         */
        AntLibraryHandler(ClassLoader classloader, Properties als) {
            this.classloader = classloader;
            this.aliasMap = als;
        }

        /**
         * Sets the DocumentLocator attribute of the AntLibraryHandler
         * object
         *
         * @param locator The new DocumentLocator value
         */
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        private void parseAttributes(String tag, AttributeList attrs)
            throws SAXParseException {
            name = null;
            className = null;
            adapter = null;

            for (int i = 0, last = attrs.getLength(); i < last; i++) {
                String key = attrs.getName(i);
                String value = attrs.getValue(i);

                if (key.equals("name")) {
                    name = value;
                }
                else if (key.equals("class")) {
                    className = value;
                }
                else if ("role".equals(tag) && key.equals("adapter")) {
                    adapter = value;
                }
                else {
                    throw new SAXParseException("Unexpected attribute \""
                                                + key + "\"", locator);
                }
            }
            if (name == null || className == null) {
                String msg = "Underspecified " + tag + " declaration.";
                throw new SAXParseException(msg, locator);
            }
        }

        /**
         * SAX callback handler
         *
         * @param tag XML tag
         * @param attrs attributes
         * @exception SAXParseException parse trouble
         */
        public void startElement(String tag, AttributeList attrs)
            throws SAXParseException {
            level ++;
            if ("antlib".equals(tag)) {
                if (level > 1) {
                    throw new SAXParseException("Unexpected element: " + tag,
                                                locator);
                }
                // No attributes to worry about
                return;
            }
            if (level == 1) {
                throw new SAXParseException("Missing antlib root element",
                                            locator);
            }

            // Must have the two attributes declared
            parseAttributes(tag, attrs);

            try {
                if ("role".equals(tag)) {
                    if (getProject().isRoleDefined(name)) {
                        String msg = "Cannot override role: " + name;
                        log(msg, Project.MSG_WARN);
                        return;
                    }
                    // Defining a new role
                    Class clz = loadClass(className);
                    if (clz != null) {
                        getProject().addRoleDefinition(name, clz,
                                                  (adapter == null? null :
                                                   loadClass(adapter)));
                    }
                    return;
                }

                // Defining a new element kind
                //check for name alias
                String alias = aliasMap.getProperty(name);
                if (alias != null) {
                    name = alias;
                }
                //catch an attempted override of an existing name
                if (!override && getProject().isDefinedOnRole(tag, name)) {
                    String msg = "Cannot override " + tag + ": " + name;
                    log(msg, Project.MSG_WARN);
                    return;
                }
                Class clz = loadClass(className);
                if (clz != null)
                    getProject().addDefinitionOnRole(tag, name, clz);
            }
            catch(BuildException be) {
                switch (onerror.getIndex()) {
                case FAIL:
                    throw new SAXParseException(be.getMessage(), locator, be);
                case REPORT:
                    getProject().log(be.getMessage(), Project.MSG_WARN);
                    break;
                default:
                    getProject().log(be.getMessage(), Project.MSG_DEBUG);
                }
            }
        }

        public void endElement(String tag) {
            level--;
        }

        private Class loadClass(String className)
            throws SAXParseException {
            String msg = null;
            try {
                //load the named class
                Class cls;
                if(classloader==null) {
                    cls=Class.forName(className);
                }
                else {
                    cls=classloader.loadClass(className);
                }
                return cls;
            }
            catch (ClassNotFoundException cnfe) {
                msg = "Class " + className + " cannot be found";
                if (onerror.getIndex() == FAIL)
                    throw new SAXParseException(msg, locator, cnfe);
            }
            catch (NoClassDefFoundError ncdfe) {
                msg = "Class " + className + " cannot be loaded";
                if (onerror.getIndex() == FAIL)
                    throw new SAXParseException(msg, locator);
            }

            if (onerror.getIndex() == REPORT) {
                getProject().log(msg, Project.MSG_WARN);
            }
            else {
                getProject().log(msg, Project.MSG_DEBUG);
            }
            return null;
        }

    //end inner class AntLibraryHandler
    }


    /**
     * this class is used for alias elements
     *
     * @author slo
     * @created 11 November 2001
     */
    public static class Alias {
        /**
         * Description of the Field
         */
        private String name;
        /**
         * Description of the Field
         */
        private String as;


        /**
         * Sets the Name attribute of the Alias object
         *
         * @param name The new Name value
         */
        public void setName(String name) {
            this.name = name;
        }


        /**
         * Sets the As attribute of the Alias object
         *
         * @param as The new As value
         */
        public void setAs(String as) {
            this.as = as;
        }
    //end inner class alias
    }

//end class Antlib
}


