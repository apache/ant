/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2000 The Apache Software Foundation.  All rights
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

import org.xml.sax.*;
import javax.xml.parsers.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.*;

import java.io.*;
import java.util.*;

/**
 * Creates a ANTLIB archive. Code is similar to the War class, but with
 * bonus dtd validation.
 *
 * @author doc and layout changes by steve loughran, steve_l@iseran.com
 * @author <a href="mailto:j_a_fernandez@yahoo.com">Jose Alberto Fernandez</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 *      
 * @since ant1.5
 */
public class Antjar extends Jar {

    /**
     * location of the xml descriptor (antxml attribute)
     */
    private File libraryDescriptor;
    
    /**
     * status flag
     */
    private boolean descriptorAdded;


    /**
     * Constructor for the Antjar object
     */
    public Antjar() {
        super();
        archiveType = "jar";
        emptyBehavior = "create";
    }


    /**
     * Sets the Antxml attribute of the Antjar object
     *
     * @param descriptor The new Antxml value
     */
    public void setAntxml(File descriptor) {
        libraryDescriptor = descriptor;
        if (!libraryDescriptor.exists()) {
            throw new BuildException("Deployment descriptor: " + libraryDescriptor + " does not exist.");
        }

        //check 
        validateDescriptor();

        // Create a ZipFileSet for this file, and pass it up.
        ZipFileSet fs = new ZipFileSet();
        fs.setDir(new File(libraryDescriptor.getParent()));
        fs.setIncludes(libraryDescriptor.getName());
        fs.setFullpath(Antlib.ANT_DESCRIPTOR);
        super.addFileset(fs);
    }


    /**
     * override of superclass method; add check for
     * valid descriptor
     * @param zOut stream to init
     * @exception IOException io trouble
     * @exception BuildException other trouble
     */
    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
        // If no antxml file is specified, it's an error.
        if (libraryDescriptor == null) {
            throw new BuildException("webxml attribute is required", location);
        }

        super.initZipOutputStream(zOut);
    }


    /**
     * override of parent method; warn if a second descriptor is added
     *
     * @param file file to add
     * @param zOut stream to add to 
     * @param vPath the path to add it to in the zipfile
     * @exception IOException io trouble
     */
    protected void zipFile(File file, ZipOutputStream zOut, String vPath)
        throws IOException {
        // If the file being added is META-INF/antlib.xml, we warn if it's not the
        // one specified in the "antxml" attribute - or if it's being added twice,
        // meaning the same file is specified by the "antxml" attribute and in
        // a <fileset> element.
        if (vPath.equalsIgnoreCase(Antlib.ANT_DESCRIPTOR)) {
            if (libraryDescriptor == null || !libraryDescriptor.equals(file) || descriptorAdded) {
                log("Warning: selected " + archiveType + " files include a " +
                        Antlib.ANT_DESCRIPTOR + " which will be ignored " +
                        "(please use antxml attribute to " + archiveType + " task)", Project.MSG_WARN);
            }
            else {
                super.zipFile(file, zOut, vPath);
                descriptorAdded = true;
            }
        }
        else {
            super.zipFile(file, zOut, vPath);
        }
    }


    /**
     * Make sure we don't think we already have a descriptor next time this
     * task gets executed.
     */
    protected void cleanUp() {
        descriptorAdded = false;
        super.cleanUp();
    }


    /**
     * validate the descriptor against the DTD
     *
     * @exception BuildException failure to validate
     */
    protected void validateDescriptor()
        throws BuildException {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setValidating(true);
        InputStream is = null;
        try {
            SAXParser saxParser = saxFactory.newSAXParser();
            Parser parser = saxParser.getParser();
            is = new FileInputStream(libraryDescriptor);
            InputSource inputSource = new InputSource(is);
            inputSource.setSystemId("file:" + libraryDescriptor);
            project.log("Validating library descriptor: " + libraryDescriptor,
                    Project.MSG_VERBOSE);
            saxParser.parse(inputSource, new AntLibraryValidator());
        }
        catch (ParserConfigurationException exc) {
            throw new BuildException("Parser has not been configured correctly", exc);
        }
        catch (SAXParseException exc) {
            Location location =
                    new Location(libraryDescriptor.toString(),
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
     * Parses the document describing the content of the library.
     */
    private class AntLibraryValidator extends HandlerBase {

        /**
         * flag to track whether the DOCTYPE was hit in the prolog
         */
        private boolean doctypePresent = false;

        /**
         * doc locator
         */
        private Locator locator = null;

        /**
         * Sets the DocumentLocator attribute of the AntLibraryValidator
         * object
         *
         * @param locator The new DocumentLocator value
         */
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
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
            // By the time an element is found
            // the DOCTYPE should have been found.
            if (!doctypePresent) {
                String msg = "Missing DOCTYPE declaration or wrong SYSTEM ID";
                throw new SAXParseException(msg, locator);
            }
        }

        /**
         * Recognizes the DTD declaration for antlib and returns the corresponding
         * DTD definition from a resource. <P>
         *
         * To allow for future versions of the DTD format it will search
         * for any DTDs of the form "Antlib-V.*\.dtd".
         *
         * @param publicId public ID (ignored)
         * @param systemId system ID (matched against)
         * @return local DTD instance 
         */
        public InputSource resolveEntity(String publicId,
                String systemId) {

            log("Looking for entity with PublicID=" + publicId +
                    " and SystemId=" + systemId, Project.MSG_VERBOSE);
            if (Antlib.matchDtdId(systemId)) {
                String resId =
                        systemId.substring(Antlib.ANTLIB_DTD_URL.length());
                InputSource is =
                        new InputSource(this.getClass().getResourceAsStream(resId));

                is.setSystemId(systemId);
                doctypePresent = true;
                return is;
            }
            return null;
        }
    //end inner class AntLibraryValidator
    }
}

