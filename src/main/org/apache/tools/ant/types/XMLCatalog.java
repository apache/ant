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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.types;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.AntClassLoader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.tools.ant.util.LoaderUtils;
import org.apache.tools.ant.util.FileUtils;

/**
 * This data type provides a catalog of DTD locations.
 * <p>
 * <code>
 * &lt;xmlcatalog&gt;<br>
 * &nbsp;&nbsp;&lt;dtd publicId="" location="/path/to/file.jar" /&gt;<br>
 * &nbsp;&nbsp;&lt;dtd publicId="" location="/path/to/file2.jar" /&gt;<br>
 * &nbsp;&nbsp;&lt;entity publicId="" location="/path/to/file3.jar" /&gt;<br>
 * &nbsp;&nbsp;&lt;entity publicId="" location="/path/to/file4.jar" /&gt;<br>
 * &lt;/xmlcatalog&gt;<br>
 * </code>
 * <p>
 * The object implemention <code>sometask</code> must provide a method called
 * <code>createXMLCatalog</code> which returns an instance of 
 * <code>XMLCatalog</code>. Nested DTD and entity definitions are handled by
 * the XMLCatalog object and must be labeled <code>dtd</code> and
 * <code>entity</code> respectively.</p>
 *
 * <p>Possible future extension could allow a catalog file instead of nested
 * elements, or use Norman Walsh's entity resolver from xml-commons</p>
 *
 * @author dIon Gillard
 * @author Erik Hatcher
 * @version $Id$
 */
public class XMLCatalog extends DataType implements Cloneable, EntityResolver {
    /** File utilities instance */
    private FileUtils fileUtils = FileUtils.newFileUtils();
    
    //-- Fields ----------------------------------------------------------------
    
    /** holds dtd/entity objects until needed */
    private Vector elements = new Vector();

    private Path classpath;

    //-- Methods ---------------------------------------------------------------
    
    /**
     * @return the elements of the catalog - DTDLocation objects
     */
    private Vector getElements() {
        return elements;
    }

    /**
     * @return the classpath
     */
    private Path getClasspath() {
        return classpath;
    }

    /**
     * Set the list of DTDLocation object sin the catalog
     *
     * @param aVector the new list of DTD Locations to use in the catalog.
     */
    private void setElements(Vector aVector) {
        elements = aVector;
    }
    
    /**
     * Add a DTD Location to the catalog
     *
     * @param aDTD the DTDLocation instance to be aded to the catalog
     */
    private void addElement(DTDLocation aDTD) {
        getElements().addElement(aDTD);
    }

    /**
     * Allows nested classpath elements
     */
    public Path createClasspath() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Allows simple classpath string
     */
    public void setClasspath(Path classpath) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * Allows classpath reference
     */
    public void setClasspathRef(Reference r) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createClasspath().setRefid(r);
    }

    /**
     * Creates the nested <code>&lt;dtd&gt;</code> element.
     *
     * @param dtd the infromation about the DTD to be added to the catalog
     * @exception BuildException if this is a reference and no nested 
     *       elements are allowed.
     */
    public void addDTD(DTDLocation dtd) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }

        getElements().addElement(dtd);
    }
    
    /**
     * Creates the nested <code>&lt;entity&gt;</code> element
     *
     * @param dtd the infromation about the DTD to be added to the catalog
     * @exception BuildException if this is a reference and no nested 
     *       elements are allowed.
     */
    public void addEntity(DTDLocation dtd) throws BuildException {
        addDTD(dtd);
    }

    /**
     * Loads a nested XMLCatalog into our definition
     *
     * @param catalog Nested XMLCatalog
     */
    public void addConfiguredXMLCatalog(XMLCatalog catalog) {
        if (isReference()) {
            throw noChildrenAllowed();
        }

        // Add all nested elements to our catalog
        Vector newElements = catalog.getElements();
        Vector ourElements = getElements();
        Enumeration enum = newElements.elements();
        while (enum.hasMoreElements()) {
            ourElements.addElement(enum.nextElement());
        }

        // Append the classpath of the nested catalog
        Path nestedClasspath = catalog.getClasspath();
        createClasspath().append(nestedClasspath);
    }

    /**
     * Makes this instance in effect a reference to another XCatalog instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     *
     * @param r the reference to which this catalogi instance is associated
     * @exception BuildException if this instance already has been configured.
     */
    public void setRefid(Reference r) throws BuildException {
        if (!elements.isEmpty()) {
            throw tooManyAttributes();
        }
        // change this to get the objects from the other reference
        Object o = r.getReferencedObject(getProject());
        // we only support references to other XCatalogs
        if (o instanceof XMLCatalog) {
            // set all elements from referenced catalog to this one
            XMLCatalog catalog = (XMLCatalog) o;
            setElements(catalog.getElements());
        } else {
            String msg = r.getRefId() + " does not refer to an XMLCatalog";
            throw new BuildException(msg);
        }

        super.setRefid(r);
    }

    /**
     * @see org.xml.sax.EntityResolver#resolveEntity
     */
    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException, IOException {
        InputSource source = null;
        DTDLocation matchingDTD = findMatchingDTD(publicId);
        if (matchingDTD != null) {
            // check if publicId is mapped to a file
            log("Matching DTD found for publicId: '" + publicId +
                "' location: '" + matchingDTD.getLocation() + "'",
                Project.MSG_DEBUG);
            File dtdFile = project.resolveFile(matchingDTD.getLocation());
            if (dtdFile.exists() && dtdFile.canRead()) {
                source = new InputSource(new FileInputStream(dtdFile));
                URL dtdFileURL = fileUtils.getFileURL(dtdFile);
                source.setSystemId(dtdFileURL.toExternalForm());
                log("matched a readable file", Project.MSG_DEBUG);
            } else {
                // check if publicId is a resource

                AntClassLoader loader = null;
                if (classpath != null) {
                    loader = new AntClassLoader(project, classpath);
                } else {
                    loader = new AntClassLoader(project, Path.systemClasspath);
                }

                InputStream is
                    = loader.getResourceAsStream(matchingDTD.getLocation());
                if (is != null) {
                    source = new InputSource(is);
                    source.setSystemId(loader.getResource(
                        matchingDTD.getLocation()).toExternalForm());
                    log("matched a resource", Project.MSG_DEBUG);
                } else {
                    // check if it's a URL
                    try {
                        URL dtdUrl = new URL(matchingDTD.getLocation());
                        InputStream dtdIs = dtdUrl.openStream();
                        if (dtdIs != null) {
                            source = new InputSource(dtdIs);
                            source.setSystemId(dtdUrl.toExternalForm());
                            log("matched as a URL", Project.MSG_DEBUG);
                        } else {
                            log("No match, parser will use: '" + systemId + "'",
                                Project.MSG_DEBUG);
                        }
                    } catch (IOException ioe) {
                        //ignore
                    }
                }
            }
        } else {
            log("No match, parser will use: '" + systemId + "'",
                Project.MSG_DEBUG);
        }
        // else let the parser handle it as a URI as we don't know what to
        // do with it
        return source;
    }
    
    /**
     * Find a DTDLocation instance for the given publicId.
     *
     * @param publicId the publicId of the DTD for which local information is 
     *        required
     * @return a DTDLocation instance with information on the local location 
     *         of the DTD or null if no such information is available
     */
    private DTDLocation findMatchingDTD(String publicId) {
        Enumeration elements = getElements().elements();
        DTDLocation element = null;
        while (elements.hasMoreElements()) {
            element = (DTDLocation) elements.nextElement();
            if (element.getPublicId().equals(publicId)) {
                return element;
            }
        }
        return null;
    }
      
}

