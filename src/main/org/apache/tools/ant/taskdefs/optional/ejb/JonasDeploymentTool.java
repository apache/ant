/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.ejb;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.taskdefs.Java;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.xml.parsers.SAXParser;

/**
 * The deployment tool to add the jonas specific deployment descriptors to the
 * ejb JAR file. JONAS only requires one additional file jonas-ejb-jar.xml.
 *
 * @author <a href="mailto:cmorvan@ingenosya.com">Cyrille Morvan</a> , <a
 * href="http://www.ingenosya.com">Ingenosya France</a>, <a
 * href="mailto:mathieu.peltier@inrialpes.fr">Mathieu Peltier</a>
 * @version 1.0
 * @see EjbJar#createJonas
 */
public class JonasDeploymentTool extends GenericDeploymentTool {

    /** Public Id of the standard deployment descriptor DTD. */
    protected static final String EJB_JAR_1_1_PUBLIC_ID = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    protected static final String EJB_JAR_2_0_PUBLIC_ID = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";

    /** Public Id of the JOnAS-specific deployment descriptor DTD. */
    protected static final String JONAS_EJB_JAR_2_4_PUBLIC_ID = "-//ObjectWeb//DTD JOnAS 2.4//EN";
    protected static final String JONAS_EJB_JAR_2_5_PUBLIC_ID = "-//ObjectWeb//DTD JOnAS 2.5//EN";

    /** RMI ORB. */
    protected static final String RMI_ORB = "RMI";

    /** JEREMIE ORB. */
    protected static final String JEREMIE_ORB = "JEREMIE";

    /** DAVID ORB. */
    protected static final String DAVID_ORB = "DAVID";

    /**
     * Name of the standard deployment descriptor DTD (these files are stored in
     * the ${JONAS_ROOT}/xml directory).
     */
    protected static final String EJB_JAR_1_1_DTD = "ejb-jar_1_1.dtd";
    protected static final String EJB_JAR_2_0_DTD = "ejb-jar_2_0.dtd";

    /**
     * Name of the JOnAS-specific deployment descriptor DTD (these files are
     * stored in the ${JONAS_ROOT}/xml directory).
     */
    protected static final String JONAS_EJB_JAR_2_4_DTD = "jonas-ejb-jar_2_4.dtd";
    protected static final String JONAS_EJB_JAR_2_5_DTD = "jonas-ejb-jar_2_5.dtd";

    /** Default JOnAS deployment descriptor name. */
    protected static final String JONAS_DD = "jonas-ejb-jar.xml";

    /** GenIC class name (JOnAS 2.5) */
    protected static final String GENIC_CLASS =
	"org.objectweb.jonas_ejb.genic.GenIC";

    /** Old GenIC class name (JOnAS 2.4.x). */
    protected static final String OLD_GENIC_CLASS_1 =
        "org.objectweb.jonas_ejb.tools.GenWholeIC";

    /** Old GenIC class name. */
    protected static final String OLD_GENIC_CLASS_2 =
        "org.objectweb.jonas_ejb.tools.GenIC";

    /**
     * Filename of the standard EJB descriptor (which is passed to this class
     * from the parent "ejbjar" task). This file is relative to the directory
     * specified by the "srcdir" attribute in the ejbjar task.
     */
    private String descriptorName;

    /**
     * Filename of the JOnAS-specific EJB descriptor (which is passed to this
     * class from the parent "ejbjar" task). This file is relative to the
     * directory specified by the "srcdir" attribute in the ejbjar task.
     */
    private String jonasDescriptorName;

    /* ------------- */
    /* GenIC options */
    /* ------------- */

    /**
     * Temporary output directory used by GenIC.
     */
    private File outputdir;

    /**
     * <code>true</code> if the intermediate Java source files generated by
     * GenIC must be deleted or not. The default is <code>false</code>
     */
    private boolean keepgenerated = false;

    /**
     * <code>true</code> if the generated source files must not be compiled via
     * the java and rmi compilers. The default is <code>false</code>.
     */
    private boolean nocompil = false;

    /**
     * <code>true</code> if the XML deployment descriptors must be parsed
     * without validation. The default is <code>false</code>.
     */
    private boolean novalidation = false;

    /**
     * Java compiler to use. The default is the value of
     * <code>build.compiler</code> property.
     */
    private String javac;

    /** Options to pass to the java compiler. */
    private String javacopts;

    /** Options to pass to the rmi compiler. */
    private String rmicopts;

    /**
     * <code>true</code> if the RMI Skel. and Stub. must be modified to
     * implement the implicit propagation of the security context (the
     * transactional context is always provided). The default is
     * <code>false</code>.
     */
    private boolean secpropag = false;

    /**
     * <code>true</code> if the GenIC call must be verbose. The default
     * is <code>false</code>. 
     */
    private boolean verbose = false;

    /** Additional args to send to GenIC. */
    private String additionalargs;

    /* ------------- */
    /* other options */
    /* ------------- */

    /** JOnAS root directory. */
    private File jonasroot;

    /**
     * <code>true</code> if the generic JAR file used as input to GenIC must be
     * retained. The default is <code>false</code>.
     */    
    private boolean keepgeneric = false;

    /** Stores the suffix for the JOnAS JAR file. The default is '.jar'. */
    private String suffix = ".jar";
   
    /**
     *  ORB to use (RMI, JEREMIE or DAVID). If omitted, it defaults to the one
     *  present in classpath. If specified, the corresponding JOnAS JAR is
     *  automatically added to the classpath.
     */
    private String orb;

    /** <code>true</code> if GenIC must not be run on the EJB JAR. The default is <code>false</code>. */    
    private boolean nogenic = false;

    /* -------------------- */
    /* GenIC options setter */
    /* -------------------- */

    /**
     * Set the {@link #keepgenerated} flag.
     *
     * @param aBoolean <code>true</code> if the flag must be set.
     */
    public void setKeepgenerated(boolean aBoolean) {
        keepgenerated = aBoolean;
    }

    /**
     * Set the {@link #additionalargs}.
     *
     * @param aString additional args.
     */
    public void setAdditionalargs(String aString) {
        additionalargs = aString;
    }

    /**
     * Set the {@link #nocompil} flag.
     *
     * @param aBoolean <code>true</code> if the flag must be set.
     */
    public void setNocompil(boolean aBoolean) {
        nocompil = aBoolean;
    }

    /**
     * Set the {@link #novalidation} flag.
     *
     * @param aBoolean <code>true</code> if the flag must be set.
     */
    public void setNovalidation(boolean aBoolean) {
        novalidation = aBoolean;
    }

    /**
     * Set the java compiler {@link #javac} to use.
     *
     * @param aString the java compiler.
     */
    public void setJavac(String aString) {
        javac = aString;
    }

    /**
     * Set the options to pass to the java compiler.
     *
     * @param aString the options.
     */
    public void setJavacopts(String aString) {
        javacopts = aString;
    }

    /**
     * Set the options to pass to the rmi compiler.
     *
     * @param aString the options.
     */
    public void setRmicopts(String aString) {
        rmicopts = aString;
    }

    /**
     * Set the {@link #secpropag} flag.
     *
     * @param aBoolean <code>true</code> if the flag must be set.
     */
    public void setSecpropag(boolean aBoolean) {
        secpropag = aBoolean;
    }

    /**
     * Set the {@link #verbose} flag.
     *
     * @param aBoolean <code>true</code> if the flag must be set.
     */
    public void setVerbose(boolean aBoolean) {
        verbose = aBoolean;
    }

    /* -------------------- */
    /* other options setter */
    /* -------------------- */

    /**
     * Set the JOnAS root directory.
     * 
     * @param aFile the JOnAS root directory.
     */
    public void setJonasroot(File aFile) {
        jonasroot = aFile;
    }

    /**
     * Set the {@link #keepgeneric} flag.
     *
     * @param aBoolean <code>true</code> if the flag must be set.
     */
    public void setKeepgeneric(boolean aBoolean) {
        keepgeneric = aBoolean;
    }

    /**
     * Set the {@link #jarsuffix}.
     *
     * @param aString the string to use as the suffix.
     */
    public void setJarsuffix(String aString) {
        suffix = aString;
    }

    /**
     * Set the {@link #orb} to construct classpath.
     *
     * @param aString 'RMI', 'JEREMIE', or 'DAVID'.
     */
    public void setOrb(String aString) {
        orb = aString;
    }

    /**
     * Set the {@link #nogenic} flag.
     *
     * @param aBoolean <code>true</code> if the flag must be set.
     */
    public void setNogenic(boolean aBoolean) {
        nogenic = aBoolean;
    }

    /* ------------- */
    /* other methods */
    /* ------------- */

    public void processDescriptor(String aDescriptorName, SAXParser saxParser) {
	
        descriptorName = aDescriptorName;
 
        log("JOnAS Deployment Tool processing: " + descriptorName,
            Project.MSG_VERBOSE);

        super.processDescriptor(descriptorName, saxParser);

	if ( outputdir != null ) {
	    // the method deleteOnExit() do not work because the directory is not empty	     
	    log("Deleting temp output directory '" + outputdir + "'.", Project.MSG_VERBOSE);
	    deleteAllFiles(outputdir);
	}
    }

    protected void writeJar(String baseName, File jarfile, Hashtable ejbFiles, String publicId) 
	throws BuildException {

	// create the generic jar first
	File genericJarFile = super.getVendorOutputJarFile(baseName);	
	super.writeJar(baseName, genericJarFile, ejbFiles, publicId);
	     
	// GenIC call on generic jar
	addGenICGeneratedFiles(genericJarFile, ejbFiles);

	// create the real jar
	super.writeJar(baseName, getVendorOutputJarFile(baseName), ejbFiles, publicId);

	if ( !keepgeneric ) {
	    log("Deleting generic JAR " + genericJarFile.toString(), Project.MSG_VERBOSE);
	    genericJarFile.delete();	     
	}
    }

    protected void addVendorFiles(Hashtable ejbFiles, String ddPrefix) {

	// JOnAS-specific descriptor deployment
	jonasDescriptorName = getJonasDescriptorName();
        File jonasDD = new File(getConfig().descriptorDir, jonasDescriptorName);
 
        if ( jonasDD.exists() ) {
            ejbFiles.put(META_DIR + JONAS_DD, jonasDD);
        } else {
            log("Unable to locate the JOnAS deployment descriptor. It was expected to be in: "
                + jonasDD.getPath() + ".", Project.MSG_WARN);
        }
    }

    protected File getVendorOutputJarFile(String baseName) {
        return new File(getDestDir(), baseName + suffix);
    }

    /**
     * Determines the name of the JOnAS-specific EJB descriptor using the
     * specified standard EJB descriptor name. In general, the standard
     * descriptor will be named "[basename]-ejb-jar.xml", and this method will
     * return "[basename]-jonas-ejb-jar.xml" or "jonas-[basename].xml"
     *
     * @return The name of the JOnAS-specific EJB descriptor file.
     */
    private String getJonasDescriptorName() {

        // descriptorName = <path><basename><basenameterminator><remainder>
        // examples = /org/objectweb/fooAppli/foo/Foo-ejb-jar.xml
        // examples = /org/objectweb/fooAppli/foo/Foo.xml (JOnAS convention)
 
        String jonasDescriptorName; // JOnAS-specific DD
        boolean jonasConvention = false; // true if the JOnAS convention is used for the DD
        String path;            // Directory path of the EJB descriptor
        String fileName;        // EJB descriptor file name
        String baseName;        // Filename appearing before name terminator
        String remainder;       // Filename appearing after the name terminator
 
        int startOfFileName = descriptorName.lastIndexOf(File.separatorChar);
        if ( startOfFileName != -1 ) {
            // extract path info
            path = descriptorName.substring(0, startOfFileName+1);
            fileName = descriptorName.substring(startOfFileName+1);
        } else {
            // descriptorName is just a file without path
            path = "";
            fileName = descriptorName;
        }
 
        if ( fileName.startsWith(EJB_DD) )
            return path + JONAS_DD;

        int endOfBaseName = descriptorName.indexOf(getConfig().baseNameTerminator, startOfFileName);
 
        /*
         * Check for the odd case where the terminator and/or filename
         * extension aren't found.  These will ensure "jonas-" appears at the
         * end of the name and before the '.' (if present).
         */
        if ( endOfBaseName < 0 ) { 
            // baseNameTerminator not found: the descriptor use the
            // JOnAS naming convention, ie [Foo.xml,jonas-Foo.xml] and
            // not [Foo<baseNameTerminator>-ejb-jar.xml,
            // Foo<baseNameTerminator>-jonas-ejb-jar.xml].
            endOfBaseName = descriptorName.lastIndexOf('.') - 1;
            if (endOfBaseName < 0) {
                // no . found
                endOfBaseName = descriptorName.length() - 1;
            }
 
            jonasConvention = true;
        }

        baseName = descriptorName.substring(startOfFileName + 1, endOfBaseName + 1);
        remainder = descriptorName.substring(endOfBaseName + 1);
 
        if ( jonasConvention ) {
            jonasDescriptorName = path + "jonas-" + baseName + ".xml";
        } else {
            jonasDescriptorName = path + baseName + "jonas-" + remainder;
        }
 
        log("Standard EJB descriptor name: " + descriptorName, Project.MSG_VERBOSE);
        log("JOnAS-specific descriptor name: " + jonasDescriptorName, Project.MSG_VERBOSE);
 
        return jonasDescriptorName;
    }

    protected String getJarBaseName(String descriptorFileName) {

        String baseName = null;

        if ( getConfig().namingScheme.getValue().equals(EjbJar.NamingScheme.DESCRIPTOR) ) {

            // try to find JOnAS specific convention name
            if ( descriptorFileName.indexOf(getConfig().baseNameTerminator) == -1 ) {

		// baseNameTerminator not found: the descriptor use the
		// JOnAS naming convention, ie [Foo.xml,jonas-Foo.xml] and
		// not [Foo<baseNameTerminator>-ejb-jar.xml,
		// Foo<baseNameTerminator>-jonas-ejb-jar.xml].
		
                String aCanonicalDescriptor = descriptorFileName.replace('\\', '/');
                int lastSeparatorIndex = aCanonicalDescriptor.lastIndexOf('/');
                int endOfBaseName;

                if ( lastSeparatorIndex != -1 ) {
                    endOfBaseName = descriptorFileName.indexOf(".xml", lastSeparatorIndex);
                } else {
                    endOfBaseName = descriptorFileName.indexOf(".xml");
                }

                if ( endOfBaseName != -1 ) {
                    baseName = descriptorFileName.substring(0, endOfBaseName);
                }
            }
        }

        if ( baseName == null ) {
            // else get standard baseName
            baseName = super.getJarBaseName(descriptorFileName);
        }
	
        log("JAR base name: " + baseName, Project.MSG_VERBOSE);	

        return baseName;
    }

    protected void registerKnownDTDs(DescriptorHandler handler) {
 	handler.registerDTD(EJB_JAR_1_1_PUBLIC_ID, 
			    jonasroot + File.separator + "xml" + File.separator + EJB_JAR_1_1_DTD);
 	handler.registerDTD(EJB_JAR_2_0_PUBLIC_ID, 
			    jonasroot + File.separator + "xml" + File.separator + EJB_JAR_2_0_DTD);

 	handler.registerDTD(JONAS_EJB_JAR_2_4_PUBLIC_ID, 
			    jonasroot + File.separator + "xml" + File.separator + JONAS_EJB_JAR_2_4_DTD);
 	handler.registerDTD(JONAS_EJB_JAR_2_5_PUBLIC_ID, 
			    jonasroot + File.separator + "xml" + File.separator + JONAS_EJB_JAR_2_5_DTD);
    }

    /**
     * Add to the given hashtable all the file generated by GenIC.
     *
     * @param genericJarFile jar file.
     * @param ejbFiles the hashtable.
     */
    private void addGenICGeneratedFiles(File genericJarFile, Hashtable ejbFiles) {
     
        Java genicTask = null;	// GenIC task 
	String genicClass = null; // GenIC class (3 GenIC classes for various versions of JOnAS are supported)
	boolean error = false;	// true if an error occurs during the GenIC call

        if ( nogenic ) {
	    return;
        }

	genicTask = (Java) getTask().getProject().createTask("java");
	genicTask.setTaskName("genic");
	genicTask.setFork(true);
	
	// jonasroot
	genicTask.createJvmarg().setValue("-Dinstall.root=" + jonasroot);
	
	// java policy file
	String jonasConfigDir = jonasroot + File.separator + "config";            
	File javaPolicyFile = new File(jonasConfigDir, "java.policy");
	if ( javaPolicyFile.exists() ) {
	    genicTask.createJvmarg().setValue("-Djava.security.policy="
					      + javaPolicyFile.toString());
	}
	
	// outputdir
	try {
	    outputdir = createTempDir();	    
	} catch (IOException aIOException) {
	    String msg = "Cannot create temp dir: " + aIOException.getMessage();
	    throw new BuildException(msg, aIOException);
	}
	log("Using temporary output directory: " + outputdir, Project.MSG_VERBOSE);
	
	genicTask.createArg().setValue("-d");
	genicTask.createArg().setFile(outputdir);
	
	// work around a bug of GenIC 2.5
	String key;
	File f;
	Enumeration keys = ejbFiles.keys();
	while ( keys.hasMoreElements() ) {
	    key = (String)keys.nextElement();
	    f = new File(outputdir + File.separator + key);	    
	    f.getParentFile().mkdirs();
	}
	log("Worked around a bug of GenIC 2.5.", Project.MSG_VERBOSE);

	// classpath 
	Path classpath = getCombinedClasspath();	    
	if ( classpath == null ) {
	    classpath = new Path(getTask().getProject());
	}
	classpath.append(new Path(classpath.getProject(), jonasConfigDir));
	classpath.append(new Path(classpath.getProject(), outputdir.toString()));
	
	// try to create the classpath for the correct ORB
	if ( orb != null ) {	    
	    String orbJar = jonasroot + File.separator + "lib" + File.separator + orb + "_jonas.jar";
	    classpath.append(new Path(classpath.getProject(), orbJar));
	}
	
	log("Using classpath: " + classpath.toString(), Project.MSG_VERBOSE);
	genicTask.setClasspath(classpath);
	
	// class name (search in the classpath provided for the ejbjar element)
	genicClass = getGenicClassName(classpath);
	if ( genicClass == null ) {
	    log("Cannot find GenIC class in classpath.", Project.MSG_ERR);
	    throw new BuildException("GenIC class not found, please check the classpath.");
	} else {
	    log("Using '" + genicClass + "' GenIC class." , Project.MSG_VERBOSE);	    
	    genicTask.setClassname(genicClass);
	}
	
	// keepgenerated
	if ( keepgenerated ) {
	    genicTask.createArg().setValue("-keepgenerated");
	}
	
	// nocompil
	if ( nocompil ) {
	    genicTask.createArg().setValue("-nocompil");
	}
	
	// novalidation
	if ( novalidation ) {
	    genicTask.createArg().setValue("-novalidation");
	}
	
	// javac
	if ( javac != null ) {
	    genicTask.createArg().setValue("-javac");
	    genicTask.createArg().setLine(javac);
	}
	
	// javacopts
	if ( javacopts != null && !javacopts.equals("") ) {
	    genicTask.createArg().setValue("-javacopts");
	    genicTask.createArg().setLine(javacopts);
	}

	// rmicopts
	if ( rmicopts != null && !rmicopts.equals("") ) {
	    genicTask.createArg().setValue("-rmicopts");
	    genicTask.createArg().setLine(rmicopts);
	}
	
	// secpropag
	if ( secpropag ) {
	    genicTask.createArg().setValue("-secpropag");
	}
	
	// verbose
	if ( verbose ) {
	    genicTask.createArg().setValue("-verbose");
            }
	
	// additionalargs
	if ( additionalargs != null ) {
	    genicTask.createArg().setValue(additionalargs);
	}
	
	// the generated classes must not be added in the generic JAR!
	// is that buggy on old JOnAS (2.4) ??
	genicTask.createArg().setValue("-noaddinjar");
	
	// input file to process by GenIC
	genicTask.createArg().setValue(genericJarFile.getPath());

	// calling GenIC task
	log("Calling " + genicClass + " for " + getConfig().descriptorDir + File.separator + descriptorName
	    + ".", Project.MSG_VERBOSE);

 	if ( genicTask.executeJava() != 0 ) {

	    // the method deleteOnExit() do not work because the directory is not empty
	    log("Deleting temp output directory '" + outputdir + "'.", Project.MSG_VERBOSE);
	    deleteAllFiles(outputdir);

	    if ( !keepgeneric ) {
		log("Deleting generic JAR " + genericJarFile.toString(), Project.MSG_VERBOSE);
		genericJarFile.delete();	     
	    }

	    throw new BuildException("GenIC reported an error.");
	}
	
	// add the generated files to the ejbFiles
	addAllFiles(outputdir, "", ejbFiles);
    }

    /**
     * Get the GenIC class name to use in the given classpath.
     *
     * @param classpath classpath where the GenIC class must be searched.
     * @return the GenIC class name. Return <code>null</code> if the class name
     * cannot be found.
     */
    String getGenicClassName(Path classpath) {

	log("Looking for GenIC class in classpath: " + classpath.toString(), Project.MSG_VERBOSE);

	AntClassLoader cl = new AntClassLoader(classpath.getProject(), classpath);

	try {
	    cl.loadClass(JonasDeploymentTool.GENIC_CLASS);
	    log("Found GenIC class '" + JonasDeploymentTool.GENIC_CLASS + "' in classpath.", Project.MSG_VERBOSE);
	    return JonasDeploymentTool.GENIC_CLASS;

	} catch (ClassNotFoundException cnf1) {
 	    log("GenIC class '" + JonasDeploymentTool.GENIC_CLASS + "' not found in classpath.", 
		Project.MSG_VERBOSE);
	} 

	try {
	    cl.loadClass(JonasDeploymentTool.OLD_GENIC_CLASS_1);
	    log("Found GenIC class '" + JonasDeploymentTool.OLD_GENIC_CLASS_1 + 
		"' in classpath.", Project.MSG_VERBOSE);
	    return JonasDeploymentTool.OLD_GENIC_CLASS_1;

	} catch (ClassNotFoundException cnf2) {
 	    log("GenIC class '" + JonasDeploymentTool.OLD_GENIC_CLASS_1 + 
		"' not found in classpath.",
		Project.MSG_VERBOSE);
	} 

	try {
	    cl.loadClass(JonasDeploymentTool.OLD_GENIC_CLASS_2);
	    log("Found GenIC class '" + JonasDeploymentTool.OLD_GENIC_CLASS_2 + 
		"' in classpath.", Project.MSG_VERBOSE);
	    return JonasDeploymentTool.OLD_GENIC_CLASS_2;

	} catch (ClassNotFoundException cnf3) {
 	    log("GenIC class '" + JonasDeploymentTool.OLD_GENIC_CLASS_2 + 
		"' not found in classpath.",
		Project.MSG_VERBOSE);
	} 
	return null;
    }

    protected void checkConfiguration(String descriptorFileName,
				      SAXParser saxParser) throws BuildException {

	// jonasroot
	if ( jonasroot == null ) {
	    throw new BuildException("The jonasroot attribut is not set.");
	} else if ( !jonasroot.isDirectory() ) {
	    throw new BuildException("The jonasroot attribut '" + jonasroot + 
				     "' is not a valid directory.");
	}

	// orb
	if ( orb != null && !orb.equals(RMI_ORB) && !orb.equals(JEREMIE_ORB) && !orb.equals(DAVID_ORB) ) {
	    throw new BuildException("The orb attribut '" + orb + "' is not valid (must be either " +
				     RMI_ORB + ", " + JEREMIE_ORB + " or " + DAVID_ORB + ").");
	}

	// additionalargs
	if ( additionalargs != null && additionalargs.equals("") ) {
	    throw new BuildException("Empty additionalargs attribut.");
	}

	// javac
	if ( javac != null && javac.equals("") ) {
	    throw new BuildException("Empty javac attribut.");	
	}
    }

    /* ----------------------------------------------------------------------------------- */
    /* utilitary methods */
    /* ----------------------------------------------------------------------------------- */    

    /**
     * Create a temporary directory for GenIC output.
     *
     * @return the temp directory.
     * @throws BuildException if a temp directory cannot be created.
     */
    private File createTempDir() throws IOException {       
	File tmpDir = File.createTempFile("genic", null, null);	    
	tmpDir.delete();
	if ( !tmpDir.mkdir() ) {
	    throw new IOException("Cannot create the temporary directory '" + tmpDir + "'.");
	}
	return tmpDir;
    }

    /**
     * Delete a file. If the file is a directory, delete recursivly all the
     * files inside.
     *
     * @param aFile file to delete.
     */
    private void deleteAllFiles(File aFile) {
        if ( aFile.isDirectory() ) {
            File someFiles[] = aFile.listFiles();

            for (int i = 0; i < someFiles.length; i++) {
                deleteAllFiles(someFiles[i]);
            }
        }
        aFile.delete();
    }

    /**
     * Add a file to the a given hashtable. If the file is a directory, add
     * recursivly all the files inside to the hashtable.
     *
     * @param file the file to add.
     * @param rootDir the current sub-directory to scan.
     * @param hashtable the hashtable where to add the files.
     */
    private void addAllFiles(File file, String rootDir, Hashtable hashtable) {

        if ( !file.exists() ) {
	    throw new IllegalArgumentException();
	}
	
	String newRootDir;
        if ( file.isDirectory() ) {
	    File files[] = file.listFiles();	
	    for (int i = 0; i < files.length; i++) {
		if ( rootDir.length() > 0 ) {
		    newRootDir = rootDir + File.separator + files[i].getName();
		} else {
		    newRootDir = files[i].getName();		     
		}
		addAllFiles(files[i], newRootDir, hashtable);
	    }
        } else {
            hashtable.put(rootDir, file);
        }
    }
}
