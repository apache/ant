/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Date;



import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.AttributeList;

/**
 * Compiles EJB stubs and skeletons for the iPlanet Application
 * Server (iAS).  The class will read a standard EJB descriptor (as well as an
 * EJB descriptor specific to iPlanet Application Server) to identify one or
 * more EJBs to process.  It will search for EJB "source" classes (the remote
; * interface, home interface, and EJB implementation class) and the EJB stubs
 * and skeletons in the specified destination directory.  Only if the stubs and
 * skeletons cannot be found or if they're out of date will the iPlanet
 * Application Server ejbc utility be run.
 * <p>
 * Because this class (and it's assorted inner classes) may be bundled into the
 * iPlanet Application Server distribution at some point (and removed from the
 * Ant distribution), the class has been written to be independent of all
 * Ant-specific classes.  It is also for this reason (and to avoid cluttering
 * the Apache Ant source files) that this utility has been packaged into a
 * single source file.
 * <p>
 * For more information on Ant Tasks for iPlanet Application Server, see the
 * <code>IPlanetDeploymentTool</code> and <code>IPlanetEjbcTask</code> classes.
 *
 * @see    IPlanetDeploymentTool
 * @see    IPlanetEjbcTask
 * @author Greg Nelson <a href="mailto:greg@netscape.com">greg@netscape.com</a>
 * @ant.task ignore="true"
 */
public class IPlanetEjbc {

    /* Constants used for the "beantype" attribute */
    private static final String ENTITY_BEAN       = "entity";
    private static final String STATELESS_SESSION = "stateless";
    private static final String STATEFUL_SESSION  = "stateful";

    /* Filenames of the standard EJB descriptor and the iAS-specific descriptor */
    private File        stdDescriptor;
    private File        iasDescriptor;

    /*
     * Directory where "source" EJB files are stored and where stubs and
     * skeletons will also be written.
     */
    private File        destDirectory;

    /* Classpath used when the iAS ejbc is called */
    private String      classpath;
    private String[]    classpathElements;

    /* Options passed to the iAS ejbc */
    private boolean     retainSource = false;
    private boolean     debugOutput  = false;

    /* iAS installation directory (used if ejbc isn't on user's PATH) */
    private File        iasHomeDir;

    /* Parser and handler used to process both EJB descriptor files */
    private SAXParser   parser;
    private EjbcHandler handler = new EjbcHandler();

    /*
     * This Hashtable maintains a list of EJB class files processed by the ejbc
     * utility (both "source" class files as well as stubs and skeletons). The
     * key for the Hashtable is a String representing the path to the class file
     * (relative to the destination directory).  The value for the Hashtable is
     * a File object which reference the actual class file.
     */
    private Hashtable   ejbFiles     = new Hashtable();

    /* Value of the display-name element read from the standard EJB descriptor */
    private String      displayName;

    /**
     * Constructs an instance which may be used to process EJB descriptors and
     * generate EJB stubs and skeletons, if needed.
     *
     * @param stdDescriptor File referencing a standard EJB descriptor.
     * @param iasDescriptor File referencing an iAS-specific EJB descriptor.
     * @param destDirectory File referencing the base directory where both
     *                      EJB "source" files are found and where stubs and
     *                      skeletons will be written.
     * @param classpath     String representation of the classpath to be used
     *                      by the iAS ejbc utility.
     * @param parser        SAXParser to be used to process both of the EJB
     *                      descriptors.
     * @todo classpathElements is not needed here, its never used
     *       (at least IDEA tells me so! :)
     */
    public IPlanetEjbc(File stdDescriptor,
                       File iasDescriptor,
                       File destDirectory,
                       String classpath,
                       SAXParser parser) {
        this.stdDescriptor = stdDescriptor;
        this.iasDescriptor      = iasDescriptor;
        this.destDirectory      = destDirectory;
        this.classpath          = classpath;
        this.parser             = parser;

        /*
         * Parse the classpath into it's individual elements and store the
         * results in the "classpathElements" instance variable.
         */
        List elements = new ArrayList();
        if (classpath != null) {
            StringTokenizer st = new StringTokenizer(classpath,
                                                        File.pathSeparator);
            while (st.hasMoreTokens()) {
                elements.add(st.nextToken());
            }
            classpathElements
                    = (String[]) elements.toArray(new String[elements.size()]);
        }
    }

    /**
     * If true, the Java source files which are generated by the
     * ejbc process are retained.
     *
     * @param retainSource A boolean indicating if the Java source files for
     *                     the stubs and skeletons should be retained.
     * @todo This is not documented in the HTML. On purpose?
     */
    public void setRetainSource(boolean retainSource) {
        this.retainSource = retainSource;
    }

    /**
     * If true, enables debugging output when ejbc is executed.
     *
     * @param debugOutput A boolean indicating if debugging output should be
     *                    generated
     */
    public void setDebugOutput(boolean debugOutput) {
        this.debugOutput = debugOutput;
    }

    /**
     * Registers the location of a local DTD file or resource.  By registering
     * a local DTD, EJB descriptors can be parsed even when the remote servers
     * which contain the "public" DTDs cannot be accessed.
     *
     * @param publicID The public DTD identifier found in an XML document.
     * @param location The file or resource name for the appropriate DTD stored
     *                 on the local machine.
     */
    public void registerDTD(String publicID, String location) {
        handler.registerDTD(publicID, location);
    }

    /**
     * May be used to specify the "home" directory for this iAS installation.
     * The directory specified should typically be
     * <code>[install-location]/iplanet/ias6/ias</code>.
     *
     * @param iasHomeDir The home directory for the user's iAS installation.
     */
    public void setIasHomeDir(File iasHomeDir) {
        this.iasHomeDir = iasHomeDir;
    }

    /**
     * Returns a Hashtable which contains a list of EJB class files processed by
     * the ejbc utility (both "source" class files as well as stubs and
     * skeletons). The key for the Hashtable is a String representing the path
     * to the class file (relative to the destination directory).  The value for
     * the Hashtable is a File object which reference the actual class file.
     *
     * @return The list of EJB files processed by the ejbc utility.
     */
    public Hashtable getEjbFiles() {
        return ejbFiles;
    }

    /**
     * Returns the display-name element read from the standard EJB descriptor.
     *
     * @return The EJB-JAR display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the list of CMP descriptors referenced in the EJB descriptors.
     *
     * @return An array of CMP descriptors.
     */
    public String[] getCmpDescriptors() {
        List returnList = new ArrayList();

        EjbInfo[] ejbs = handler.getEjbs();

        for (int i = 0; i < ejbs.length ; i++) {
            List descriptors = (List) ejbs[i].getCmpDescriptors();
            returnList.addAll(descriptors);
        }

        return (String[]) returnList.toArray(new String[returnList.size()]);
    }

    /**
     * Main application method for the iPlanet Application Server ejbc utility.
     * If the application is run with no commandline arguments, a usage
     * statement is printed for the user.
     *
     * @param args The commandline arguments passed to the application.
     */
    public static void main(String[] args) {
        File        stdDescriptor;
        File        iasDescriptor;
        File        destDirectory = null;
        String      classpath     = null;
        SAXParser   parser        = null;
        boolean     debug         = false;
        boolean     retainSource  = false;
        IPlanetEjbc ejbc;

        if ((args.length < 2) || (args.length > 8)) {
            usage();
            return;
        }

        stdDescriptor = new File(args[args.length - 2]);
        iasDescriptor = new File(args[args.length - 1]);

        for (int i = 0; i < args.length - 2; i++) {
            if (args[i].equals("-classpath")) {
                classpath = args[++i];
            } else if (args[i].equals("-d")) {
                destDirectory = new File(args[++i]);
            } else if (args[i].equals("-debug")) {
                debug = true;
            } else if (args[i].equals("-keepsource")) {
                retainSource = true;
            } else {
                usage();
                return;
            }
        }

        /* If the -classpath flag isn't specified, use the system classpath */
        if (classpath == null) {
            Properties props = System.getProperties();
            classpath = props.getProperty("java.class.path");
        }

        /*
         * If the -d flag isn't specified, use the working directory as the
         * destination directory
         */
        if (destDirectory == null) {
            Properties props = System.getProperties();
            destDirectory = new File(props.getProperty("user.dir"));
        }

        /* Construct a SAXParser used to process the descriptors */
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setValidating(true);
        try {
            parser = parserFactory.newSAXParser();
        } catch (Exception e) {
            // SAXException or ParserConfigurationException may be thrown
            System.out.println("An exception was generated while trying to ");
            System.out.println("create a new SAXParser.");
            e.printStackTrace();
            return;
        }

        /* Build and populate an instance of the ejbc utility */
        ejbc = new IPlanetEjbc(stdDescriptor, iasDescriptor, destDirectory,
                                classpath, parser);
        ejbc.setDebugOutput(debug);
        ejbc.setRetainSource(retainSource);

        /* Execute the ejbc utility -- stubs/skeletons are rebuilt, if needed */
        try {
            ejbc.execute();
        } catch (IOException e) {
            System.out.println("An IOException has occurred while reading the "
                    + "XML descriptors (" + e.getMessage() + ").");
            return;
        } catch (SAXException e) {
            System.out.println("A SAXException has occurred while reading the "
                    + "XML descriptors (" + e.getMessage() + ").");
            return;
        } catch (IPlanetEjbc.EjbcException e) {
            System.out.println("An error has occurred while executing the ejbc "
                    + "utility (" + e.getMessage() + ").");
            return;
        }
    }

    /**
     * Print a usage statement.
     */
    private static void usage() {
        System.out.println("java org.apache.tools.ant.taskdefs.optional.ejb.IPlanetEjbc \\");
        System.out.println("  [OPTIONS] [EJB 1.1 descriptor] [iAS EJB descriptor]");
        System.out.println("");
        System.out.println("Where OPTIONS are:");
        System.out.println("  -debug -- for additional debugging output");
        System.out.println("  -keepsource -- to retain Java source files generated");
        System.out.println("  -classpath [classpath] -- classpath used for compilation");
        System.out.println("  -d [destination directory] -- directory for compiled classes");
        System.out.println("");
        System.out.println("If a classpath is not specified, the system classpath");
        System.out.println("will be used.  If a destination directory is not specified,");
        System.out.println("the current working directory will be used (classes will");
        System.out.println("still be placed in subfolders which correspond to their");
        System.out.println("package name).");
        System.out.println("");
        System.out.println("The EJB home interface, remote interface, and implementation");
        System.out.println("class must be found in the destination directory.  In");
        System.out.println("addition, the destination will look for the stubs and skeletons");
        System.out.println("in the destination directory to ensure they are up to date.");
    }

    /**
     * Compiles the stub and skeletons for the specified EJBs, if they need to
     * be updated.
     *
     * @throws EjbcException If the ejbc utility cannot be correctly configured
     *                       or if one or more of the EJB "source" classes
     *                       cannot be found in the destination directory
     * @throws IOException   If the parser encounters a problem reading the XML
     *                       file
     * @throws SAXException  If the parser encounters a problem processing the
     *                       XML descriptor (it may wrap another exception)
     */
    public void execute() throws EjbcException, IOException, SAXException {

        checkConfiguration();   // Throws EjbcException if unsuccessful

        EjbInfo[] ejbs = getEjbs(); // Returns list of EJBs for processing

        for (int i = 0; i < ejbs.length ; i++) {
            log("EJBInfo...");
            log(ejbs[i].toString());
        }

        for (int i = 0; i < ejbs.length; i++) {
            EjbInfo ejb = ejbs[i];

            ejb.checkConfiguration(destDirectory);  // Throws EjbcException

            if (ejb.mustBeRecompiled(destDirectory)) {
                log(ejb.getName() + " must be recompiled using ejbc.");

                String[] arguments = buildArgumentList(ejb);
                callEjbc(arguments);

            } else {
                log(ejb.getName() + " is up to date.");
            }
        }
    }

    /**
     * Executes the iPlanet Application Server ejbc command-line utility.
     *
     * @param arguments Command line arguments to be passed to the ejbc utility.
     */
    private void callEjbc(String[] arguments) {

        /* Concatenate all of the command line arguments into a single String */
        StringBuffer args = new StringBuffer();
        for (int i = 0; i < arguments.length; i++) {
            args.append(arguments[i]).append(" ");
        }

        /* If an iAS home directory is specified, prepend it to the commmand */
        String command;
        if (iasHomeDir == null) {
            command = "";
        } else {
            command = iasHomeDir.toString() + File.separator + "bin"
                                                        + File.separator;
        }
        command += "ejbc ";

        log(command + args);

        /*
         * Use the Runtime object to execute an external command.  Use the
         * RedirectOutput inner class to direct the standard and error output
         * from the command to the JRE's standard output
         */
        try {
            Process p = Runtime.getRuntime().exec(command + args);
            RedirectOutput output = new RedirectOutput(p.getInputStream());
            RedirectOutput error  = new RedirectOutput(p.getErrorStream());
            output.start();
            error.start();
            p.waitFor();
            p.destroy();
        } catch (IOException e) {
            log("An IOException has occurred while trying to execute ejbc.");
            e.printStackTrace();
        } catch (InterruptedException e) {
            // Do nothing
        }
    }

    /**
     * Verifies that the user selections are valid.
     *
     * @throws EjbcException If the user selections are invalid.
     */
    protected void checkConfiguration() throws EjbcException {

        String msg = "";

        if (stdDescriptor == null) {
            msg += "A standard XML descriptor file must be specified.  ";
        }
        if (iasDescriptor == null) {
            msg += "An iAS-specific XML descriptor file must be specified.  ";
        }
        if (classpath == null) {
            msg += "A classpath must be specified.    ";
        }
        if (parser == null) {
            msg += "An XML parser must be specified.    ";
        }

        if (destDirectory == null) {
            msg += "A destination directory must be specified.  ";
        } else if (!destDirectory.exists()) {
            msg += "The destination directory specified does not exist.  ";
        } else if (!destDirectory.isDirectory()) {
            msg += "The destination specified is not a directory.  ";
        }

        if (msg.length() > 0) {
            throw new EjbcException(msg);
        }
    }

    /**
     * Parses the EJB descriptors and returns a list of EJBs which may need to
     * be compiled.
     *
     * @return               An array of objects which describe the EJBs to be
     *                       processed.
     * @throws IOException   If the parser encounters a problem reading the XML
     *                       files
     * @throws SAXException  If the parser encounters a problem processing the
     *                       XML descriptor (it may wrap another exception)
     */
    private EjbInfo[] getEjbs() throws IOException, SAXException {
        EjbInfo[] ejbs = null;

        /*
         * The EJB information is gathered from the standard XML EJB descriptor
         * and the iAS-specific XML EJB descriptor using a SAX parser.
         */

        parser.parse(stdDescriptor, handler);
        parser.parse(iasDescriptor, handler);
        ejbs = handler.getEjbs();

        return ejbs;
    }

    /**
     * Based on this object's instance variables as well as the EJB to be
     * processed, the correct flags and parameters are set for the ejbc
     * command-line utility.
     * @param ejb The EJB for which stubs and skeletons will be compiled.
     * @return    An array of Strings which are the command-line parameters for
     *            for the ejbc utility.
     */
    private String[] buildArgumentList(EjbInfo ejb) {

        List arguments = new ArrayList();

        /* OPTIONAL COMMAND LINE PARAMETERS */

        if (debugOutput) {
            arguments.add("-debug");
        }

        /* No beantype flag is needed for an entity bean */
        if (ejb.getBeantype().equals(STATELESS_SESSION)) {
            arguments.add("-sl");
        } else if (ejb.getBeantype().equals(STATEFUL_SESSION)) {
            arguments.add("-sf");
        }

        if (ejb.getIiop()) {
            arguments.add("-iiop");
        }

        if (ejb.getCmp()) {
            arguments.add("-cmp");
        }

        if (retainSource) {
            arguments.add("-gs");
        }

        if (ejb.getHasession()) {
            arguments.add("-fo");
        }

        /* REQUIRED COMMAND LINE PARAMETERS */

        arguments.add("-classpath");
        arguments.add(classpath);

        arguments.add("-d");
        arguments.add(destDirectory.toString());

        arguments.add(ejb.getHome().getQualifiedClassName());
        arguments.add(ejb.getRemote().getQualifiedClassName());
        arguments.add(ejb.getImplementation().getQualifiedClassName());

        /* Convert the List into an Array and return it */
        return (String[]) arguments.toArray(new String[arguments.size()]);
    }

    /**
     * Convenience method used to print messages to the user if debugging
     * messages are enabled.
     *
     * @param msg The String to print to standard output.
     */
    private void log(String msg) {
        if (debugOutput) {
            System.out.println(msg);
        }
    }


    /* Inner classes follow */


    /**
     * This inner class is used to signal any problems during the execution of
     * the ejbc compiler.
     *
     * @author Greg Nelson
     *         <a href="mailto:greg@netscape.com">greg@netscape.com</a>
     */
    public class EjbcException extends Exception {

        /**
         * Constructs an exception with the given descriptive message.
         *
         * @param msg Description of the exception which has occurred.
         */
        public EjbcException(String msg) {
            super(msg);
        }
    }  // End of EjbcException inner class


    /**
     * This inner class is an XML document handler that can be used to parse EJB
     * descriptors (both the standard EJB descriptor as well as the iAS-specific
     * descriptor that stores additional values for iAS).  Once the descriptors
     * have been processed, the list of EJBs found can be obtained by calling
     * the <code>getEjbs()</code> method.
     *
     * @see    IPlanetEjbc.EjbInfo
     * @author Greg Nelson
     *         <a href="mailto:greg@netscape.com">greg@netscape.com</a>
     */
    private class EjbcHandler extends HandlerBase {

        /*
         * Two Maps are used to track local DTDs that will be used in case the
         * remote copies of these DTDs cannot be accessed.  The key for the Map
         * is the DTDs public ID and the value is the local location for the DTD
         */
        private Map       resourceDtds = new HashMap();
        private Map       fileDtds = new HashMap();

        private Map       ejbs = new HashMap();      // List of EJBs found in XML
        private EjbInfo   currentEjb;             // One item within the Map
        private boolean   iasDescriptor = false;  // Is doc iAS or EJB descriptor

        private String    currentLoc = "";        // Tracks current element
        private String    currentText;            // Tracks current text data
        private String    ejbType;                // "session" or "entity"

        /**
         * Constructs a new instance of the handler and registers local copies
         * of the standard EJB 1.1 descriptor DTD as well as iAS's EJB
         * descriptor DTD.
         */
        public EjbcHandler() {
            final String PUBLICID_EJB11 =
                "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
            final String PUBLICID_IPLANET_EJB_60 =
                "-//Sun Microsystems, Inc.//DTD iAS Enterprise JavaBeans 1.0//EN";


            final String DEFAULT_IAS60_EJB11_DTD_LOCATION =
                "ejb-jar_1_1.dtd";
            final String DEFAULT_IAS60_DTD_LOCATION =
                "IASEjb_jar_1_0.dtd";

            registerDTD(PUBLICID_EJB11, DEFAULT_IAS60_EJB11_DTD_LOCATION);
            registerDTD(PUBLICID_IPLANET_EJB_60, DEFAULT_IAS60_DTD_LOCATION);
        }

        /**
         * Returns the list of EJB objects found during the processing of the
         * standard EJB 1.1 descriptor and iAS-specific EJB descriptor.
         *
         * @return An array of EJBs which were found during the descriptor
         *         parsing.
         */
        public EjbInfo[] getEjbs() {
            return (EjbInfo[]) ejbs.values().toArray(new EjbInfo[ejbs.size()]);
        }

        /**
         * Returns the value of the display-name element found in the standard
         * EJB 1.1 descriptor.
         *
         * @return String display-name value.
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Registers a local DTD that will be used when parsing an EJB
         * descriptor.  When the DTD's public identifier is found in an XML
         * document, the parser will reference the local DTD rather than the
         * remote DTD.  This enables XML documents to be processed even when the
         * public DTD isn't available.
         *
         * @param publicID The DTD's public identifier.
         * @param location The location of the local DTD copy -- the location
         *                 may either be a resource found on the classpath or a
         *                 local file.
         */
        public void registerDTD(String publicID, String location) {
            log("Registering: " + location);
            if ((publicID == null) || (location == null)) {
                return;
            }

            if (ClassLoader.getSystemResource(location) != null) {
                log("Found resource: " + location);
                resourceDtds.put(publicID, location);
            } else {
                File dtdFile = new File(location);
                if (dtdFile.exists() && dtdFile.isFile()) {
                    log("Found file: " + location);
                    fileDtds.put(publicID, location);
                }
            }
        }

        /**
         * Resolves an external entity found during XML processing.  If a public
         * ID is found that has been registered with the handler, an <code>
         * InputSource</code> will be returned which refers to the local copy.
         * If the public ID hasn't been registered or if an error occurs, the
         * superclass implementation is used.
         *
         * @param publicId The DTD's public identifier.
         * @param systemId The location of the DTD, as found in the XML document.
         */
        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException {
            InputStream inputStream = null;


            try {

                /* Search the resource Map and (if not found) file Map */

                String location = (String) resourceDtds.get(publicId);
                if (location != null) {
                    inputStream
                        = ClassLoader.getSystemResource(location).openStream();
                } else {
                    location = (String) fileDtds.get(publicId);
                    if (location != null) {
                        inputStream = new FileInputStream(location);
                    }
                }
            } catch (IOException e) {
                return super.resolveEntity(publicId, systemId);
            }

            if (inputStream == null) {
                return super.resolveEntity(publicId, systemId);
            } else {
                return new InputSource(inputStream);
            }
        }

        /**
         * Receive notification that the start of an XML element has been found.
         *
         * @param name String name of the element found.
         * @param atts AttributeList of the attributes included with the element
         *             (if any).
         * @throws SAXException If the parser cannot process the document.
         */
        public void startElement(String name, AttributeList atts)
                throws SAXException {

            /*
             * I need to "push" the element onto the String (currentLoc) which
             * always represents the current location in the XML document.
             */
            currentLoc += "\\" + name;

            /* A new element has started, so reset the text being captured */
            currentText = "";

            if (currentLoc.equals("\\ejb-jar")) {
                iasDescriptor = false;
            } else if (currentLoc.equals("\\ias-ejb-jar")) {
                iasDescriptor = true;
            }

            if ((name.equals("session")) || (name.equals("entity"))) {
                ejbType = name;
            }
        }

        /**
         * Receive notification that character data has been found in the XML
         * document
         *
         * @param ch Array of characters which have been found in the document.
         * @param start Starting index of the data found in the document.
         * @param len The number of characters found in the document.
         * @throws SAXException If the parser cannot process the document.
         */
        public void characters(char[] ch, int start, int len)
                throws SAXException {

            currentText += new String(ch).substring(start, start + len);
        }

        /**
         * Receive notification that the end of an XML element has been found.
         *
         * @param name String name of the element.
         * @throws SAXException If the parser cannot process the document.
         */
        public void endElement(String name) throws SAXException {

            /*
             * If this is a standard EJB 1.1 descriptor, we are looking for one
             * set of data, while if this is an iAS-specific descriptor, we're
             * looking for different set of data.  Hand the processing off to
             * the appropriate method.
             */
            if (iasDescriptor) {
                iasCharacters(currentText);
            } else {
                stdCharacters(currentText);
            }

            /*
             * I need to "pop" the element off the String (currentLoc) which
             * always represents my current location in the XML document.
             */

            int nameLength = name.length() + 1; // Add one for the "\"
            int locLength  = currentLoc.length();

            currentLoc = currentLoc.substring(0, locLength - nameLength);
        }

        /**
         * Receive notification that character data has been found in a standard
         * EJB 1.1 descriptor.  We're interested in retrieving the home
         * interface, remote interface, implementation class, the type of bean,
         * and if the bean uses CMP.
         *
         * @param value String data found in the XML document.
         */
        private void stdCharacters(String value) {

            if (currentLoc.equals("\\ejb-jar\\display-name")) {
                displayName = value;
                return;
            }

            String base = "\\ejb-jar\\enterprise-beans\\" + ejbType;

            if (currentLoc.equals(base + "\\ejb-name")) {
                currentEjb = (EjbInfo) ejbs.get(value);
                if (currentEjb == null) {
                    currentEjb = new EjbInfo(value);
                    ejbs.put(value, currentEjb);
                }
            } else if (currentLoc.equals(base + "\\home")) {
                currentEjb.setHome(value);
            } else if (currentLoc.equals(base + "\\remote")) {
                currentEjb.setRemote(value);
            } else if (currentLoc.equals(base + "\\ejb-class")) {
                currentEjb.setImplementation(value);
            } else if (currentLoc.equals(base + "\\prim-key-class")) {
                currentEjb.setPrimaryKey(value);
            } else if (currentLoc.equals(base + "\\session-type")) {
                currentEjb.setBeantype(value);
            } else if (currentLoc.equals(base + "\\persistence-type")) {
                currentEjb.setCmp(value);
            }
        }

        /**
         * Receive notification that character data has been found in an
         * iAS-specific descriptor.  We're interested in retrieving data
         * indicating whether the bean must support RMI/IIOP access, whether
         * the bean must provide highly available stubs and skeletons (in the
         * case of stateful session beans), and if this bean uses additional
         * CMP XML descriptors (in the case of entity beans with CMP).
         *
         * @param value String data found in the XML document.
         */
        private void iasCharacters(String value) {
            String base = "\\ias-ejb-jar\\enterprise-beans\\" + ejbType;

            if (currentLoc.equals(base + "\\ejb-name")) {
                currentEjb = (EjbInfo) ejbs.get(value);
                if (currentEjb == null) {
                    currentEjb = new EjbInfo(value);
                    ejbs.put(value, currentEjb);
                }
            } else if (currentLoc.equals(base + "\\iiop")) {
                currentEjb.setIiop(value);
            } else if (currentLoc.equals(base + "\\failover-required")) {
                currentEjb.setHasession(value);
            } else if (currentLoc.equals(base + "\\persistence-manager"
                                              + "\\properties-file-location")) {
                currentEjb.addCmpDescriptor(value);
            }
        }
    }  // End of EjbcHandler inner class


    /**
     * This inner class represents an EJB that will be compiled using ejbc.
     *
     * @author Greg Nelson
     *         <a href="mailto:greg@netscape.com">greg@netscape.com</a>
     */
    private class EjbInfo {
        private String     name;              // EJB's display name
        private Classname  home;              // EJB's home interface name
        private Classname  remote;            // EJB's remote interface name
        private Classname  implementation;      // EJB's implementation class
        private Classname  primaryKey;        // EJB's primary key class
        private String  beantype = "entity";  // or "stateful" or "stateless"
        private boolean cmp       = false;      // Does this EJB support CMP?
        private boolean iiop      = false;      // Does this EJB support IIOP?
        private boolean hasession = false;      // Does this EJB require failover?
        private List cmpDescriptors = new ArrayList();  // CMP descriptor list

        /**
         * Construct a new EJBInfo object with the given name.
         *
         * @param name The display name for the EJB.
         */
        public EjbInfo(String name) {
            this.name = name;
        }

        /**
         * Returns the display name of the EJB.  If a display name has not been
         * set, it returns the EJB implementation classname (if the
         * implementation class is not set, it returns "[unnamed]").
         *
         * @return The display name for the EJB.
         */
        public String getName() {
            if (name == null) {
                if (implementation == null) {
                    return "[unnamed]";
                } else {
                    return implementation.getClassName();
                }
            }
            return name;
        }

        /*
         * Below are getter's and setter's for each of the instance variables.
         * Note that (in addition to supporting setters with the same type as
         * the instance variable) a setter is provided with takes a String
         * argument -- this are provided so the XML document handler can set
         * the EJB values using the Strings it parses.
         */

        public void setHome(String home) {
            setHome(new Classname(home));
        }

        public void setHome(Classname home) {
            this.home = home;
        }

        public Classname getHome() {
            return home;
        }

        public void setRemote(String remote) {
            setRemote(new Classname(remote));
        }

        public void setRemote(Classname remote) {
            this.remote = remote;
        }

        public Classname getRemote() {
            return remote;
        }

        public void setImplementation(String implementation) {
            setImplementation(new Classname(implementation));
        }

        public void setImplementation(Classname implementation) {
            this.implementation = implementation;
        }

        public Classname getImplementation() {
            return implementation;
        }

        public void setPrimaryKey(String primaryKey) {
            setPrimaryKey(new Classname(primaryKey));
        }

        public void setPrimaryKey(Classname primaryKey) {
            this.primaryKey = primaryKey;
        }

        public Classname getPrimaryKey() {
            return primaryKey;
        }

        public void setBeantype(String beantype) {
            this.beantype = beantype.toLowerCase();
        }

        public String getBeantype() {
            return beantype;
        }

        public void setCmp(boolean cmp) {
            this.cmp = cmp;
        }

        public void setCmp(String cmp) {
            setCmp(cmp.equals("Container"));
        }

        public boolean getCmp() {
            return cmp;
        }

        public void setIiop(boolean iiop) {
            this.iiop = iiop;
        }

        public void setIiop(String iiop) {
            setIiop(iiop.equals("true"));
        }

        public boolean getIiop() {
            return iiop;
        }

        public void setHasession(boolean hasession) {
            this.hasession = hasession;
        }

        public void setHasession(String hasession) {
            setHasession(hasession.equals("true"));
        }

        public boolean getHasession() {
            return hasession;
        }

        public void addCmpDescriptor(String descriptor) {
            cmpDescriptors.add(descriptor);
        }

        public List getCmpDescriptors() {
            return cmpDescriptors;
        }

        /**
         * Verifies that the EJB is valid--if it is invalid, an exception is
         * thrown
         *
         *
         * @param buildDir The directory where the EJB remote interface, home
         *                 interface, and implementation class must be found.
         * @throws EjbcException If the EJB is invalid.
         */
        private void checkConfiguration(File buildDir) throws EjbcException  {

            /* Check that the specified instance variables are valid */
            if (home == null) {
                throw new EjbcException("A home interface was not found "
                            + "for the " + name + " EJB.");
            }
            if (remote == null) {
                throw new EjbcException("A remote interface was not found "
                            + "for the " + name + " EJB.");
            }
            if (implementation == null) {
                throw new EjbcException("An EJB implementation class was not "
                            + "found for the " + name + " EJB.");
            }

            if ((!beantype.equals(ENTITY_BEAN))
                        && (!beantype.equals(STATELESS_SESSION))
                        && (!beantype.equals(STATEFUL_SESSION))) {
                throw new EjbcException("The beantype found (" + beantype + ") "
                            + "isn't valid in the " + name + " EJB.");
            }

            if (cmp && (!beantype.equals(ENTITY_BEAN))) {
                System.out.println("CMP stubs and skeletons may not be generated"
                    + " for a Session Bean -- the \"cmp\" attribute will be"
                    + " ignoredfor the " + name + " EJB.");
            }

            if (hasession && (!beantype.equals(STATEFUL_SESSION))) {
                System.out.println("Highly available stubs and skeletons may "
                    + "only be generated for a Stateful Session Bean -- the "
                    + "\"hasession\" attribute will be ignored for the "
                    + name + " EJB.");
            }

            /* Check that the EJB "source" classes all exist */
            if (!remote.getClassFile(buildDir).exists()) {
                throw new EjbcException("The remote interface "
                            + remote.getQualifiedClassName() + " could not be "
                            + "found.");
            }
            if (!home.getClassFile(buildDir).exists()) {
                throw new EjbcException("The home interface "
                            + home.getQualifiedClassName() + " could not be "
                            + "found.");
            }
            if (!implementation.getClassFile(buildDir).exists()) {
                throw new EjbcException("The EJB implementation class "
                            + implementation.getQualifiedClassName() + " could "
                            + "not be found.");
            }
        }

        /**
         * Determines if the ejbc utility needs to be run or not.  If the stubs
         * and skeletons can all be found in the destination directory AND all
         * of their timestamps are more recent than the EJB source classes
         * (home, remote, and implementation classes), the method returns
         * <code>false</code>.  Otherwise, the method returns <code>true</code>.
         *
         * @param destDir The directory where the EJB source classes, stubs and
         *                skeletons are located.
         * @return A boolean indicating whether or not the ejbc utility needs to
         *         be run to bring the stubs and skeletons up to date.
         */
        public boolean mustBeRecompiled(File destDir) {

            long sourceModified = sourceClassesModified(destDir);

            long destModified = destClassesModified(destDir);

            return (destModified < sourceModified);
        }

        /**
         * Examines each of the EJB source classes (home, remote, and
         * implementation) and returns the modification timestamp for the
         * "oldest" class.
         *
         * @param classpath The classpath to be used to find the source EJB
         *                  classes.  If <code>null</code>, the system classpath
         *                  is used.
         * @return The modification timestamp for the "oldest" EJB source class.
         * @throws BuildException If one of the EJB source classes cannot be
         *                        found on the classpath.
         */
        private long sourceClassesModified(File buildDir) {
            long latestModified; // The timestamp of the "newest" class
            long modified;       // Timestamp for a given class
            File remoteFile;     // File for the remote interface class
            File homeFile;       // File for the home interface class
            File implFile;       // File for the EJB implementation class
            File pkFile;         // File for the EJB primary key class

            /* Check the timestamp on the remote interface */
            remoteFile = remote.getClassFile(buildDir);
            modified = remoteFile.lastModified();
            if (modified == -1) {
                System.out.println("The class "
                                + remote.getQualifiedClassName() + " couldn't "
                                + "be found on the classpath");
                return -1;
            }
            latestModified = modified;

            /* Check the timestamp on the home interface */
            homeFile = home.getClassFile(buildDir);
            modified = homeFile.lastModified();
            if (modified == -1) {
                System.out.println("The class "
                                + home.getQualifiedClassName() + " couldn't be "
                                + "found on the classpath");
                return -1;
            }
            latestModified = Math.max(latestModified, modified);

            /* Check the timestamp of the primary key class */
            if (primaryKey != null) {
                pkFile = primaryKey.getClassFile(buildDir);
                modified = pkFile.lastModified();
                if (modified == -1) {
                    System.out.println("The class "
                                    + primaryKey.getQualifiedClassName() + "couldn't be "
                                    + "found on the classpath");
                    return -1;
                }
                latestModified = Math.max(latestModified, modified);
            } else {
                pkFile = null;
            }

            /* Check the timestamp on the EJB implementation class.
             *
             * Note that if ONLY the implementation class has changed, it's not
             * necessary to rebuild the EJB stubs and skeletons.  For this
             * reason, we ensure the file exists (using lastModified above), but
             * we DON'T compare it's timestamp with the timestamps of the home
             * and remote interfaces (because it's irrelevant in determining if
             * ejbc must be run)
             */
            implFile = implementation.getClassFile(buildDir);
            modified = implFile.lastModified();
            if (modified == -1) {
                System.out.println("The class "
                                + implementation.getQualifiedClassName()
                                + " couldn't be found on the classpath");
                return -1;
            }

            String pathToFile = remote.getQualifiedClassName();
            pathToFile = pathToFile.replace('.', File.separatorChar) + ".class";
            ejbFiles.put(pathToFile, remoteFile);

            pathToFile = home.getQualifiedClassName();
            pathToFile = pathToFile.replace('.', File.separatorChar) + ".class";
            ejbFiles.put(pathToFile, homeFile);

            pathToFile = implementation.getQualifiedClassName();
            pathToFile = pathToFile.replace('.', File.separatorChar) + ".class";
            ejbFiles.put(pathToFile, implFile);

            if (pkFile != null) {
                pathToFile = primaryKey.getQualifiedClassName();
                pathToFile = pathToFile.replace('.', File.separatorChar) + ".class";
                ejbFiles.put(pathToFile, pkFile);
            }

            return latestModified;
        }

        /**
         * Examines each of the EJB stubs and skeletons in the destination
         * directory and returns the modification timestamp for the "oldest"
         * class. If one of the stubs or skeletons cannot be found, <code>-1
         * </code> is returned.
         *
         * @param dest The directory in which the EJB stubs and skeletons are
         *             stored.
         * @return The modification timestamp for the "oldest" EJB stub or
         *         skeleton.  If one of the classes cannot be found, <code>-1
         *         </code> is returned.
         * @throws BuildException If the canonical path of the destination
         *                        directory cannot be found.
         */
        private long destClassesModified(File destDir) {
            String[] classnames = classesToGenerate(); // List of all stubs & skels
            long destClassesModified = new Date().getTime(); // Earliest mod time
            boolean allClassesFound  = true;           // Has each been found?

            /*
             * Loop through each stub/skeleton class that must be generated, and
             * determine (if all exist) which file has the most recent timestamp
             */
            for (int i = 0; i < classnames.length; i++) {

                String pathToClass =
                        classnames[i].replace('.', File.separatorChar) + ".class";
                File classFile = new File(destDir, pathToClass);

                /*
                 * Add each stub/skeleton class to the list of EJB files.  Note
                 * that each class is added even if it doesn't exist now.
                 */
                ejbFiles.put(pathToClass, classFile);

                allClassesFound = allClassesFound && classFile.exists();

                if (allClassesFound) {
                    long fileMod = classFile.lastModified();

                    /* Keep track of the oldest modification timestamp */
                    destClassesModified = Math.min(destClassesModified, fileMod);
                }
            }

            return (allClassesFound) ? destClassesModified : -1;
        }

        /**
         * Builds an array of class names which represent the stubs and
         * skeletons which need to be generated for a given EJB.  The class
         * names are fully qualified.  Nine classes are generated for all EJBs
         * while an additional six classes are generated for beans requiring
         * RMI/IIOP access.
         *
         * @return An array of Strings representing the fully-qualified class
         *         names for the stubs and skeletons to be generated.
         */
        private String[] classesToGenerate() {
            String[] classnames = (iiop) ? new String[15] : new String[9];

            final String remotePkg     = remote.getPackageName() + ".";
            final String remoteClass   = remote.getClassName();
            final String homePkg       = home.getPackageName() + ".";
            final String homeClass     = home.getClassName();
            final String implPkg       = implementation.getPackageName() + ".";
            final String implFullClass = implementation.getQualifiedWithUnderscores();
            int index = 0;

            String fullPath;

            classnames[index++] = implPkg + "ejb_fac_" + implFullClass;
            classnames[index++] = implPkg + "ejb_home_" + implFullClass;
            classnames[index++] = implPkg + "ejb_skel_" + implFullClass;
            classnames[index++] = remotePkg + "ejb_kcp_skel_" + remoteClass;
            classnames[index++] = homePkg + "ejb_kcp_skel_" + homeClass;
            classnames[index++] = remotePkg + "ejb_kcp_stub_" + remoteClass;
            classnames[index++] = homePkg + "ejb_kcp_stub_" + homeClass;
            classnames[index++] = remotePkg + "ejb_stub_" + remoteClass;
            classnames[index++] = homePkg + "ejb_stub_" + homeClass;

            if (!iiop) {
                return classnames;
            }

            classnames[index++] = "org.omg.stub." + remotePkg + "_"
                                    + remoteClass + "_Stub";
            classnames[index++] = "org.omg.stub." + homePkg + "_"
                                    + homeClass + "_Stub";
            classnames[index++] = "org.omg.stub." + remotePkg
                                    + "_ejb_RmiCorbaBridge_"
                                    + remoteClass + "_Tie";
            classnames[index++] = "org.omg.stub." + homePkg
                                    + "_ejb_RmiCorbaBridge_"
                                    + homeClass + "_Tie";

            classnames[index++] = remotePkg + "ejb_RmiCorbaBridge_"
                                                        + remoteClass;
            classnames[index++] = homePkg + "ejb_RmiCorbaBridge_" + homeClass;

            return classnames;
        }

        /**
         * Convenience method which creates a String representation of all the
         * instance variables of an EjbInfo object.
         *
         * @return A String representing the EjbInfo instance.
         */
        public String toString() {
            String s = "EJB name: " + name
                        + "\n\r              home:      " + home
                        + "\n\r              remote:    " + remote
                        + "\n\r              impl:      " + implementation
                        + "\n\r              primaryKey: " + primaryKey
                        + "\n\r              beantype:  " + beantype
                        + "\n\r              cmp:       " + cmp
                        + "\n\r              iiop:      " + iiop
                        + "\n\r              hasession: " + hasession;

            Iterator i = cmpDescriptors.iterator();
            while (i.hasNext()) {
                s += "\n\r              CMP Descriptor: " + i.next();
            }

            return s;
        }

    } // End of EjbInfo inner class

    /**
     * Convenience class used to represent the fully qualified name of a Java
     * class.  It provides an easy way to retrieve components of the class name
     * in a format that is convenient for building iAS stubs and skeletons.
     *
     * @author Greg Nelson <a href="mailto:greg@netscape.com">greg@netscape.com</a>
     */
    private class Classname {
        private String qualifiedName;  // Fully qualified name of the Java class
        private String packageName;    // Name of the package for this class
        private String className;      // Name of the class without the package

        /**
         * This constructor builds an object which represents the name of a Java
         * class.
         *
         * @param qualifiedName String representing the fully qualified class
         *                      name of the Java class.
         */
        public Classname(String qualifiedName) {
            if (qualifiedName == null) {
                return;
            }

            this.qualifiedName = qualifiedName;

            int index = qualifiedName.lastIndexOf('.');
            if (index == -1) {
                className = qualifiedName;
                packageName = "";
            } else {
                packageName = qualifiedName.substring(0, index);
                className   = qualifiedName.substring(index + 1);
            }
        }

        /**
         * Gets the fully qualified name of the Java class.
         *
         * @return String representing the fully qualified class name.
         */
        public String getQualifiedClassName() {
            return qualifiedName;
        }

        /**
         * Gets the package name for the Java class.
         *
         * @return String representing the package name for the class.
         */
        public String getPackageName() {
            return packageName;
        }

        /**
         * Gets the Java class name without the package structure.
         *
         * @return String representing the name for the class.
         */
        public String getClassName() {
            return className;
        }

        /**
         * Gets the fully qualified name of the Java class with underscores
         * separating the components of the class name rather than periods.
         * This format is used in naming some of the stub and skeleton classes
         * for the iPlanet Application Server.
         *
         * @return String representing the fully qualified class name using
         *         underscores instead of periods.
         */
        public String getQualifiedWithUnderscores() {
            return qualifiedName.replace('.', '_');
        }

        /**
         * Returns a File which references the class relative to the specified
         * directory.  Note that the class file may or may not exist.
         *
         * @param  directory A File referencing the base directory containing
         *                   class files.
         * @return File referencing this class.
         */
        public File getClassFile(File directory) {
            String pathToFile = qualifiedName.replace('.', File.separatorChar)
                                            + ".class";
            return new File(directory, pathToFile);
        }

        /**
         * String representation of this class name.  It returns the fully
         * qualified class name.
         *
         * @return String representing the fully qualified class name.
         */
        public String toString() {
            return getQualifiedClassName();
        }
    }  // End of Classname inner class


    /**
     * Thread class used to redirect output from an <code>InputStream</code> to
     * the JRE standard output.  This class may be used to redirect output from
     * an external process to the standard output.
     *
     * @author Greg Nelson <a href="mailto:greg@netscape.com">greg@netscape.com</a>
     */
    private class RedirectOutput extends Thread {
        InputStream stream;  // Stream to read and redirect to standard output

        /**
         * Constructs a new instance that will redirect output from the
         * specified stream to the standard output.
         *
         * @param stream InputStream which will be read and redirected to the
         *               standard output.
         */
        public RedirectOutput(InputStream stream) {
            this.stream = stream;
        }

        /**
         * Reads text from the input stream and redirects it to standard output
         * using a separate thread.
         */
        public void run() {
            BufferedReader reader = new BufferedReader(
                                            new InputStreamReader(stream));
            String text;
            try {
                while ((text = reader.readLine()) != null) {
                    System.out.println(text);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Do nothing
                }
            }
        }
    }  // End of RedirectOutput inner class

}
