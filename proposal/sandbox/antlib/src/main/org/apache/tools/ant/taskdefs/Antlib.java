/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 1999 The Apache Software Foundation.  All rights
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
     * The named classloader to use.
     * Defaults to the default classLoader.
     */
    private String loaderId = "";

    /**
     * library attribute
     */
    private String library = null;
    /**
     * file attribute
     */
    private File file = null;
    /**
     * override attribute
     */
    private boolean override = false;
    /**
     * attribute to control classloader use
     */
    private boolean useCurrentClassloader = false;
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
     * Location of descriptor in library
     */
    public static String ANT_DESCRIPTOR = "META-INF/antlib.xml";

    /**
     * Prefix name for DTD of descriptor
     */
    public static String ANTLIB_DTD_URL =
            "http://jakarta.apache.org/ant/";
    /**
     * prefix of the antlib
     */
    public static String ANTLIB_DTD_PREFIX = "Antlib-V";
    
    /**
     * version counter 
     */
    public static String ANTLIB_DTD_VERSION = "1_0";
    
    /**
     * dtd file extension
     */
    public static String ANTLIB_DTD_EXT = ".dtd";


    /**
     * constructor creates a validating sax parser
     */
    public Antlib() {
        super();
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setValidating(true);
    }


    /**
     * constructor binds to a project as well as setting up internal state
     *
     * @param p Description of Parameter
     */
    public Antlib(Project p) {
        this();
        setProject(p);
    }


    /**
     * Set name of library to load. The library is located in $ANT_HOME/lib.
     *
     * @param lib the name of library relative to $ANT_HOME/lib.
     */
    public void setLibrary(String lib) {
        this.library = lib;
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
     * Set the ClassLoader to use for this library.
     *
     * @param id the id for the ClassLoader to use, 
     *           if other than the default.
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
     * Set whether to use a new classloader or not. 
     * Default is <code>false</code>.
     * This property is mostly used by the core when loading core tasks.
     *
     * @param useCurrentClassloader if true the current classloader will
     *      be used to load the definitions.
     */
    public void setUseCurrentClassloader(boolean useCurrentClassloader) {
        this.useCurrentClassloader = useCurrentClassloader;
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
            classpath = new Path(project);
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
     * actually do the work of loading the library
     *
     * @exception BuildException Description of Exception
     * @todo maybe have failonerror support for missing file?
     */
    public void execute()
        throws BuildException {
        File realFile = file;
        if (library != null) {
            if (file != null) {
                String msg = "You cannot specify both file and library.";
                throw new BuildException(msg, location);
            }
            // For the time being libraries live in $ANT_HOME/antlib.
            // The idea being that we would not load all the jars there anymore
            String home = project.getProperty("ant.home");

            if (home == null) {
                throw new BuildException("ANT_HOME not set as required.");
            }

            realFile = new File(new File(home, "antlib"), library);
        }
        else if (file == null) {
            String msg = "Must specify either library or file attribute.";
            throw new BuildException(msg, location);
        }
        if (!realFile.exists()) {
            String msg = "Cannot find library: " + realFile;
            throw new BuildException(msg, location);
        }

        //open the descriptor
        InputStream is = getDescriptor(realFile);

        if (is == null) {
            String msg = "Missing descriptor on library: " + realFile;
            throw new BuildException(msg, location);
        }

        
        ClassLoader classloader=null;
        if (useCurrentClassloader && classpath != null) {
            log("ignoring the useCurrentClassloader option as a classpath is defined",
                    Project.MSG_WARN);
            useCurrentClassloader=false;
        }
        if (!useCurrentClassloader) {
            classloader = makeClassLoader(realFile);
        }

        //parse it and evaluate it.
        evaluateDescriptor(classloader, processAliases(), is);
    }


    /**
     * Load definitions directly from an external XML file.
     *
     * @param xmlfile XML file in the Antlib format.
     * @exception BuildException failure to open the file
     */
    public void loadDefinitions(File xmlfile)
        throws BuildException {
        try {
            InputStream is = new FileInputStream(xmlfile);
            loadDefinitions(is);
        }
        catch (IOException io) {
            throw new BuildException("Cannot read file: " + file, io);
        }
    }


    /**
     * Load definitions directly from InputStream.
     *
     * @param is InputStream for the Antlib descriptor.
     * @exception BuildException trouble
     */
    public void loadDefinitions(InputStream is)
        throws BuildException {
        evaluateDescriptor(null, processAliases(), is);
    }


    /**
     * get a descriptor from the library file
     *
     * @param file jarfile to open
     * @return input stream to the Descriptor 
     * @exception BuildException io trouble, or it isnt a zipfile
     */
    private InputStream getDescriptor(File file)
        throws BuildException {
        try {
            final ZipFile zipfile = new ZipFile(file);
            ZipEntry entry = zipfile.getEntry(ANT_DESCRIPTOR);

            if (entry == null) {
                return null;
            }

            // Guarantee that when Entry is closed so does the zipfile instance.
            return
                new FilterInputStream(zipfile.getInputStream(entry)) {
                    public void close()
                        throws IOException {
                        super.close();
                        zipfile.close();
                    }
                };
        }
        catch (ZipException ze) {
            throw new BuildException("Not a library file.", ze, location);
        }
        catch (IOException ioe) {
            throw new BuildException("Cannot read library content.",
                    ioe, location);
        }
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
     * @param file library file to use
     * @return classloader using te
     * @exception BuildException trouble creating the classloader
     */
    protected ClassLoader makeClassLoader(File file)
        throws BuildException {
        Path clspath = new Path(project);
        clspath.setLocation(file);
        //append any build supplied classpath
        if (classpath != null) {
            clspath.append(classpath);
        }
	return project.addToLoader(loaderId, clspath);
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
            Parser parser = saxParser.getParser();

            InputSource inputSource = new InputSource(is);
            //inputSource.setSystemId(uri); //URI is nasty for jar entries
            project.log("parsing descriptor for library: " + file,
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
         * @param cl optional classloader
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
		    if (project.isRoleDefined(name)) {
			String msg = "Cannot override role: " + name;
			log(msg, Project.MSG_WARN);
			return;			
		    }
		    // Defining a new role
		    project.addRoleDefinition(name, loadClass(className),
					      (adapter == null? 
					       null : loadClass(adapter))); 
		    return;
		}

		// Defining a new element kind
		//check for name alias
		String alias = aliasMap.getProperty(name);
		if (alias != null) {
		    name = alias;
		}
		//catch an attempted override of an existing name
		if (!override && project.isDefinedOnRole(tag, name)) {
		    String msg = "Cannot override " + tag + ": " + name;
		    log(msg, Project.MSG_WARN);
		    return;
		}
		project.addDefinitionOnRole(tag, name, loadClass(className));
	    }
	    catch(BuildException be) {
		throw new SAXParseException(be.getMessage(), locator, be);
	    }
        }

	public void endElement(String tag) {
	    level--;
	}

	private Class loadClass(String className)
	    throws SAXParseException {
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
		String msg = "Class " + className +
		    " cannot be found";
		throw new SAXParseException(msg, locator, cnfe);
	    }
	    catch (NoClassDefFoundError ncdfe) {
		String msg = "Class " + className +
		    " cannot be found";
		throw new SAXParseException(msg, locator);
	    }
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


