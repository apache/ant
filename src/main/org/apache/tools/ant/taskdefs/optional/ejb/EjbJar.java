/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

// Standard java imports
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;
import java.util.jar.*;
import java.util.zip.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

// XML imports
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Parser;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.AttributeList;
import org.xml.sax.DocumentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.ParserFactory;

// Apache/Ant imports
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Java;

/**
 * <p>Provides automated ejb jar file creation for ant.  Extends the MatchingTask
 * class provided in the default ant distribution to provide a directory scanning
 * EJB jarfile generator.</p>
 *
 * <p>The task works by taking the deployment descriptors one at a time and
 * parsing them to locate the names of the classes which should be placed in
 * the jar.  The classnames are translated to java.io.Files by replacing periods
 * with File.separatorChar and resolving the generated filename as a relative
 * path under the srcdir attribute.  All necessary files are then assembled into
 * a jarfile.  One jarfile is constructed for each deployment descriptor found.
 * </p>
 *
 * <p>Functionality is currently provided for standard EJB1.1 jars and Weblogic
 * 5.1 jars. The weblogic deployment descriptors, used in constructing the 
 * Weblogic jar, are located based on a simple naming convention. The name of the
 * standard deployment descriptor is taken upto the first instance of a String,
 * specified by the attribute basenameterminator, and then the regular Weblogic
 * descriptor name is appended. For example if basenameterminator is set to '-',
 * its default value, and a standard descriptor is called Foo-ejb-jar.xml then
 * the files Foo-weblogic-ejb-jar.xml and Foo-weblogic-cmp-rdbms-jar.xml will be
 * looked for, and if found, included in the jarfile.</p>
 *
 * <p>Attributes and setter methods are provided to support optional generation
 * of Weblogic5.1 jars, optional deletion of generic jar files, setting alternate
 * values for basenameterminator, and setting the strings to append to the names
 * of the generated jarfiles.</p>
 *
 * @author <a href="mailto:tfennell@sapient.com">Tim Fennell</a>
 */
public class EjbJar extends MatchingTask {

    /**
     * Inner class used by EjbJar to facilitate the parsing of deployment
     * descriptors and the capture of appropriate information. Extends
     * HandlerBase so it only implements the methods needed. During parsing
     * creates a hashtable consisting of entries mapping the name it should be
     * inserted into an EJB jar as to a File representing the file on disk. This
     * list can then be accessed through the getFiles() method.
     */
    protected class DescriptorHandler extends org.xml.sax.HandlerBase {
        /**
         * Bunch of constants used for storing entries in a hashtable, and for
         * constructing the filenames of various parts of the ejb jar.
         */
        private static final String HOME_INTERFACE   = "home";
        private static final String REMOTE_INTERFACE = "remote";
        private static final String BEAN_CLASS       = "ejb-class";
        private static final String PK_CLASS         = "prim-key-class";

        /**
         * Instance variable used to store the name of the current attribute being
         * processed by the SAX parser.  Accessed by the SAX parser call-back methods
         * startElement() and endElement().
         */
        private String currentAttribute = null;

        /**
         * Instance variable that stores the names of the files as they will be
         * put into the jar file, mapped to File objects  Accessed by the SAX
         * parser call-back method characters().
         */
        private Hashtable ejbFiles = null;

        /** Instance variable to store the source directory of the task */
    

        /**
         * Getter method that returns the set of files to include in the EJB jar.
         */
        public Hashtable getFiles() {
            return (ejbFiles == null) ? new Hashtable() : ejbFiles;
        }


        /**
         * SAX parser call-back method that is used to initialize the values of some
         * instance variables to ensure safe operation.
         */
        public void startDocument() throws SAXException {
            this.ejbFiles         = new Hashtable(10, 1);
            this.currentAttribute = null;
        }


        /**
         * SAX parser call-back method that is invoked when a new element is entered
         * into.  Used to store the context (attribute name) in the currentAttribute
         * instance variable.
         * @param name The name of the element being entered.
         * @param attrs Attributes associated to the element.
         */
        public void startElement(String name, AttributeList attrs) 
            throws SAXException {
            this.currentAttribute = name;
        }


        /**
         * SAX parser call-back method that is invoked when an element is exited.
         * Used to blank out (set to the empty string, not nullify) the name of
         * the currentAttribute.  A better method would be to use a stack as an
         * instance variable, however since we are only interested in leaf-node
         * data this is a simpler and workable solution.
         * @param name The name of the attribute being exited. Ignored
         *        in this implementation.
         */
        public void endElement(String name) throws SAXException {
            this.currentAttribute = "";
        }

        /**
         * SAX parser call-back method invoked whenever characters are located within
         * an element.  currentAttribute (modified by startElement and endElement)
         * tells us whether we are in an interesting element (one of the up to four
         * classes of an EJB).  If so then converts the classname from the format
         * org.apache.tools.ant.Parser to the convention for storing such a class,
         * org/apache/tools/ant/Parser.class.  This is then resolved into a file
         * object under the srcdir which is stored in a Hashtable.
         * @param ch A character array containing all the characters in
         *        the element, and maybe others that should be ignored.
         * @param start An integer marking the position in the char
         *        array to start reading from.
         * @param length An integer representing an offset into the
         *        char array where the current data terminates.
         */
        public void characters(char[] ch, int start, int length) 
            throws SAXException {
            if (currentAttribute.equals(DescriptorHandler.HOME_INTERFACE)   ||
                currentAttribute.equals(DescriptorHandler.REMOTE_INTERFACE) ||
                currentAttribute.equals(DescriptorHandler.BEAN_CLASS)       ||
                currentAttribute.equals(DescriptorHandler.PK_CLASS)) {
                
                // Get the filename into a String object
                File classFile = null;
                String className = new String(ch, start, length);

                // If it's a primitive wrapper then we shouldn't try and put
                // it into the jar, so ignore it.
                if (!className.startsWith("java.lang")) {
                    // Translate periods into path separators, add .class to the
                    // name, create the File object and add it to the Hashtable.
                    className = className.replace('.', File.separatorChar);
                    className += ".class";
                    classFile = new File(srcdir, className);
                    ejbFiles.put(className, classFile);
                }
            }
        }
    } // End of DescriptorHandler

    /** Private constants that are used when constructing the standard jarfile */
    private static final String META_DIR  = "META-INF/";
    private static final String EJB_DD    = "ejb-jar.xml";
    private static final String WL_DD     = "weblogic-ejb-jar.xml";
    private static final String WL_CMP_DD = "weblogic-cmp-rdbms-jar.xml";

    /** Stores a handle to the directory under which to search for files */
    private File srcdir = null;

    /** Stores a handle to the directory to put the Jar files in */
    private File destdir = null;

    /** Instance variable that determines whether to generate weblogic jars. */
    private boolean generateweblogic = false;

    /** Instance variable that determines whether generic ejb jars are kept. */
    private boolean keepgeneric = true;
    
    /** Instance variable that marks the end of the 'basename' */
    private String basenameterminator = "-";

    /** Instance variable that stores the suffix for the generated jarfile. */
    private String genericjarsuffix = "-generic.jar";

    /** Instance variable that stores the suffix for the weblogic jarfile. */
    private String weblogicjarsuffix = "-wl.jar";

    /**
     * Setter used to store the value of srcdir prior to execute() being called.
     * @param inDir The string indicating the source directory.
     */
    public void setSrcdir(String inDir) {
        this.srcdir = this.project.resolveFile(inDir);
    }

    /**
     * Setter used to store the value of destination directory prior to execute()
     * being called.
     * @param inFile The string indicating the source directory.
     */
    public void setDestdir(String inDir) {
        this.destdir = this.project.resolveFile(inDir);
    }

    /**
     * Setter used to store the suffix for the generated jar file.
     * @param inString the string to use as the suffix.
     */
    public void setGenericjarsuffix(String inString) {
        this.genericjarsuffix = inString;
    }

    /**
     * Setter used to store the suffix for the generated weblogic jar file.
     * @param inString the string to use as the suffix.
     */
    public void setWeblogicjarsuffix(String inString) {
        this.weblogicjarsuffix = inString;
    }

    /**
     * Setter used to store the value of generateweblogic.
     * @param inValue a string, either 'true' or 'false'.
     */
    public void setGenerateweblogic(String inValue) {
        this.generateweblogic = Boolean.valueOf(inValue).booleanValue();
    }

    /**
     * Setter used to store the value of keepgeneric
     * @param inValue a string, either 'true' or 'false'.
     */
    public void setKeepgeneric(String inValue) {
        this.keepgeneric = Boolean.valueOf(inValue).booleanValue();
    }
    
    /**
     * Setter used to store the value of basenameterminator
     * @param inValue a string which marks the end of the basename.
     */
    public void setBasenameterminator(String inValue) {
        if (inValue != null) this.basenameterminator = inValue;
    }

    /**
     * Utility method that encapsulates the logic of adding a file entry to
     * a .jar file.  Used by execute() to add entries to the jar file as it is
     * constructed.
     * @param jStream A JarOutputStream into which to write the
     *        jar entry.
     * @param iStream A FileInputStream from which to read the
     *        contents the file being added.
     * @param filename A String representing the name, including
     *        all relevant path information, that should be stored for the entry
     *        being added.
     */
    protected void addFileToJar(JarOutputStream jStream,
                                FileInputStream iStream,
                                String          filename)
        throws BuildException {
        try {
            // Create the zip entry and add it to the jar file
            ZipEntry zipEntry = new ZipEntry(filename);
            jStream.putNextEntry(zipEntry);
            
            // Create the file input stream, and buffer everything over
            // to the jar output stream
            byte[] byteBuffer = new byte[2 * 1024];
            int count = 0;
            do {
                jStream.write(byteBuffer, 0, count);
                count = iStream.read(byteBuffer, 0, byteBuffer.length);
            } while (count != -1);
            
            // Close up the file input stream for the class file
            iStream.close();
        }
        catch (IOException ioe) {
            String msg = "IOException while adding entry "
                         + filename + "to jarfile."
                         + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over the
     * filenames/java.io.Files in the Hashtable stored on the instance variable
     * ejbFiles.
     */
    public void writeJar(File jarfile, Hashtable files) throws BuildException{
        JarOutputStream jarStream = null;
        Iterator entryIterator = null;
        String entryName = null;
        File entryFile = null;

        try {
            /* If the jarfile already exists then whack it and recreate it.
             * Should probably think of a more elegant way to handle this
             * so that in case of errors we don't leave people worse off
             * than when we started =)
             */
            if (jarfile.exists()) jarfile.delete();
            jarfile.getParentFile().mkdirs();
            jarfile.createNewFile();
            
            // Create the streams necessary to write the jarfile
            jarStream = new JarOutputStream(new FileOutputStream(jarfile));
            jarStream.setMethod(JarOutputStream.DEFLATED);
            
            // Loop through all the class files found and add them to the jar
            entryIterator = files.keySet().iterator();
            while (entryIterator.hasNext()) {
                entryName = (String) entryIterator.next();
                entryFile = (File) files.get(entryName);
                
                this.log("adding file '" + entryName + "'",
                         Project.MSG_VERBOSE);

                addFileToJar(jarStream,
                             new FileInputStream(entryFile),
                             entryName);
            }
            // All done.  Close the jar stream.
            jarStream.close();
        }
        catch(IOException ioe) {
            String msg = "IOException while processing ejb-jar file '"
                + jarfile.toString()
                + "'. Details: "
                + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
    } // end of writeJar
    

    /**
     *
     */
    public void buildWeblogicJar(File sourceJar, File destJar) {
        org.apache.tools.ant.taskdefs.Java javaTask = null;
        
        try {
            // Unfortunately, because weblogic.ejbc calls system.exit(), we
            // cannot do it 'in-process'. If they ever fix this, we should
            // change this code - it would be much quicker!
            String args = "-noexit " + sourceJar + " " + destJar;
            
            javaTask = (Java) this.project.createTask("java");
            javaTask.setClassname("weblogic.ejbc");
            javaTask.setArgs(args);
            javaTask.setFork("false");

            this.log("Calling weblogic.ejbc for " + sourceJar.toString(),
                     Project.MSG_INFO);

            javaTask.execute();
        }
        catch (Exception e) {
            // Have to catch this because of the semantics of calling main()
            String msg = "Exception while calling ejbc. Details: " + e.toString();
            throw new BuildException(msg, e);
        }
    }


    /**
     * Invoked by Ant after the task is prepared, when it is ready to execute
     * this task.  Parses the XML deployment descriptor to acquire the list of
     * files, then constructs the destination jar file (first deleting it if it
     * already exists) from the list of classfiles encountered and the descriptor
     * itself.  File will be of the expected format with classes under full
     * package hierarchies and the descriptor in META-INF/ejb-jar.xml
     * @exception BuildException thrown whenever a problem is
     *            encountered that cannot be recovered from, to signal to ant
     *            that a major problem occurred within this task.
     */
    public void execute() throws BuildException {
        boolean          needBuild  = true;
        DirectoryScanner ds         = null;
        String[]         files      = null;
        int              index      = 0;
        File             weblogicDD = null;
        File             jarfile    = null;
        File             wlJarfile  = null;
        File             jarToCheck = null;
        DescriptorHandler handler   = null;
        Hashtable        ejbFiles   = null;
        String           baseName   = null;

        // Lets do a little asserting to make sure we have all the
        // required attributes from the task processor
        StringBuffer sb = new StringBuffer();
        boolean die = false;
        sb.append("Processing ejbjar - the following attributes ");
        sb.append("must be specified: ");
        if (this.srcdir     == null) { sb.append("srcdir ");     die = true; }
        if (this.destdir    == null) { sb.append("destdir");     die = true; }
        if ( die ) throw new BuildException(sb.toString());

        try {
            // Create the parser using whatever parser the system dictates
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(false);
            SAXParser saxParser = saxParserFactory.newSAXParser();

            ds = this.getDirectoryScanner(this.srcdir);
            ds.scan();
            files = ds.getIncludedFiles();

            this.log(files.length + " deployment descriptors located.",
                     Project.MSG_VERBOSE);

            // Loop through the files. Each file represents one deployment
            // descriptor, and hence one bean in our model.
            for (index=0; index < files.length; ++index) {

                // By default we assume we need to build.
                needBuild = true;

                // Work out what the base name is
                int endBaseName = 
                    files[index].indexOf(basenameterminator,
                                         files[index].lastIndexOf(File.separator));
                baseName = files[index].substring(0, endBaseName);

                /* Parse the ejb deployment descriptor.  While it may not
                 * look like much, passing 'this' in the above method allows
                 * the parser to call us back when it finds interesting things.
                 */
                handler = new DescriptorHandler();
                saxParser.parse(new InputSource
                                (new FileInputStream
                                 (new File(this.srcdir, files[index]))),
                                handler);

                ejbFiles = handler.getFiles();
        
                /* Now that we've parsed the deployment descriptor we have the
                 * bean name, so we can figure out all the .xml filenames and
                 * add them to the set of files for the jar.
                 */

                // First the regular deployment descriptor
                ejbFiles.put(EjbJar.META_DIR + EjbJar.EJB_DD,
                             new File(this.srcdir, files[index]));

                // Then the weblogic deployment descriptor
                weblogicDD = new File(this.srcdir,
                                      baseName 
                                      + this.basenameterminator
                                      + EjbJar.WL_DD);

                if (weblogicDD.exists()) {
                    ejbFiles.put(EjbJar.META_DIR + EjbJar.WL_DD,
                                 weblogicDD);
                }

                // The the weblogic cmp deployment descriptor
                weblogicDD = new File(this.srcdir,
                                      baseName
                                      + this.basenameterminator 
                                      + EjbJar.WL_CMP_DD);

                if (weblogicDD.exists()) {
                    ejbFiles.put(EjbJar.META_DIR + EjbJar.WL_CMP_DD,
                                 weblogicDD);
                }

                // Lastly for the jarfiles
                jarfile = new File(this.destdir,
                                   baseName
                                   + this.genericjarsuffix);
                
                wlJarfile = new File(this.destdir,
                                     baseName
                                     + this.weblogicjarsuffix);
                
                /* Check to see if the jar file is already up to date. 
                 * Unfortunately we have to parse the descriptor just to do
                 * that, but it's still a saving over re-constructing the jar
                 * file each time. Tertiary is used to determine which jarfile
                 * we should check times against...think about it.
                 */
                jarToCheck = this.generateweblogic ? wlJarfile : jarfile;
                
                if (jarToCheck.exists()) {
                    long    lastBuild = jarToCheck.lastModified();
                    Iterator fileIter = ejbFiles.values().iterator();
                    File currentFile  = null;
                    
                    // Set the need build to false until we find out otherwise.
                    needBuild = false;

                    // Loop through the files seeing if any has been touched
                    // more recently than the destination jar.
                    while( (needBuild == false) && (fileIter.hasNext()) ) {
                        currentFile = (File) fileIter.next();
                        needBuild = ( lastBuild < currentFile.lastModified() );
                    }
                }
                
                // Check to see if we need a build and start
                // doing the work!
                if (needBuild) {
                    // Log that we are going to build...
                    this.log( "building "
                              + jarfile.getName()
                              + " with "
                              + String.valueOf(ejbFiles.size())
                              + " total files",
                              Project.MSG_INFO);

                    // Use helper method to write the jarfile
                    this.writeJar(jarfile, ejbFiles);

                    // Generate weblogic jar if requested
                    if (this.generateweblogic) {
                        this.buildWeblogicJar(jarfile, wlJarfile);
                    }

                    // Delete the original jar if we weren't asked to keep it.
                    if (!this.keepgeneric) {
                        this.log("deleting jar " + jarfile.toString(),
                                 Project.MSG_INFO);
                        jarfile.delete();
                    }
                }
                else {
                    // Log that the file is up to date...
                    this.log(jarfile.toString() + " is up to date.",
                             Project.MSG_INFO);
                }
            }
        }
        catch (SAXException se) {
            String msg = "SAXException while parsing '"
                + files[index].toString()
                + "'. This probably indicates badly-formed XML."
                + "  Details: "
                + se.getMessage();
            throw new BuildException(msg, se);
        }
        catch (ParserConfigurationException pce) {
            String msg = "ParserConfigurationException while creating parser. "
                       + "Details: " + pce.getMessage();
            throw new BuildException(msg, pce);
        }
        catch (IOException ioe) {
            String msg = "IOException while parsing'"
                + files[index].toString()
                + "'.  This probably indicates that the descriptor"
                + " doesn't exist. Details:"
                + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
    } // end of execute()
}







