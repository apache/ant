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
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.util.FileUtils;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;



/**
 * <p>This data type provides a catalog of resource locations (such as
 * DTDs and XML entities), based on the <a
 * href="http://oasis-open.org/committees/entity/spec-2001-08-06.html">
 * OASIS "Open Catalog" standard</a>.  The catalog entries are used
 * both for Entity resolution and URI resolution, in accordance with
 * the {@link org.xml.sax.EntityResolver EntityResolver} and {@link
 * javax.xml.transform.URIResolver URIResolver} interfaces as defined
 * in the <a href="http://java.sun.com/xml/jaxp">Java API for XML
 * Processing Specification</a>.</p>
 *
 * <p>Currently, only <code>&lt;dtd&gt;</code> and
 * <code>&lt;entity&gt;</code> elements may be specified inline; these
 * correspond to OASIS catalog entry types <code>PUBLIC</code> and
 * <code>URI</code> respectively.</p>
 *
 * <p>The following is a usage example:</p>
 *
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
 * <p>The following is a description of the resolution algorithm:
 * entities/URIs/dtds are looked up in each of the following contexts,
 * stopping when a valid and readable resource is found:
 * <ol>
 * <li>In the local filesystem</li>
 * <li>In the classpath</li>
 * <li>In URL-space</li>
 * </ol>
 * </p>
 *
 * <p>See {@link
 * org.apache.tools.ant.taskdefs.optional.XMLValidateTask
 * XMLValidateTask} for an example of a task that has integrated
 * support for XMLCatalogs.</p>
 *
 * <p>Possible future extension could provide for additional OASIS
 * entry types to be specified inline, and external catalog files
 * using the xml-commons resolver library</p>
 *
 * @author dIon Gillard
 * @author Erik Hatcher
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
 * @version $Id$
 */
public class XMLCatalog extends DataType implements Cloneable, EntityResolver, URIResolver {
    /** File utilities instance */
    private FileUtils fileUtils = FileUtils.newFileUtils();

    //-- Fields ----------------------------------------------------------------

    /** holds dtd/entity objects until needed */
    private Vector elements = new Vector();

    /**
     * Classpath in which to attempt to resolve resources.
     */
    private Path classpath;

    //-- Methods ---------------------------------------------------------------

    public XMLCatalog() {
        checked = false;
    }

    /**
     * Returns the elements of the catalog - DTDLocation objects.
     *
     * @return the elements of the catalog - DTDLocation objects
     */
    private Vector getElements() {
        return elements;
    }

    /**
     * Returns the classpath in which to attempt to resolve resources.
     *
     * @return the classpath
     */
    private Path getClasspath() {
        return classpath;
    }

    /**
     * Set the list of DTDLocation objects in the catalog.  Not
     * allowed if this catalog is itself a reference to another
     * catalog -- that is, a catalog cannot both refer to another
     * <em>and</em> contain elements or other attributes.
     *
     * @param aVector the new list of DTD Locations to use in the catalog.
     */
    private void setElements(Vector aVector) {
        elements = aVector;
    }

    /**
     * Allows nested classpath elements. Not allowed if this catalog
     * is itself a reference to another catalog -- that is, a catalog
     * cannot both refer to another <em>and</em> contain elements or
     * other attributes.
     */
    public Path createClasspath() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        checked = false;
        return this.classpath.createPath();
    }

    /**
     * Allows simple classpath string.  Not allowed if this catalog is
     * itself a reference to another catalog -- that is, a catalog
     * cannot both refer to another <em>and</em> contain elements or
     * other attributes.
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
        checked = false;
    }

    /**
     * Allows classpath reference.  Not allowed if this catalog is
     * itself a reference to another catalog -- that is, a catalog
     * cannot both refer to another <em>and</em> contain elements or
     * other attributes.
     */
    public void setClasspathRef(Reference r) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createClasspath().setRefid(r);
        checked = false;
    }

    /**
     * Creates the nested <code>&lt;dtd&gt;</code> element.  Not
     * allowed if this catalog is itself a reference to another
     * catalog -- that is, a catalog cannot both refer to another
     * <em>and</em> contain elements or other attributes.
     *
     * @param dtd the information about the PUBLIC resource mapping to
     *            be added to the catalog
     * @exception BuildException if this is a reference and no nested
     *       elements are allowed.
     */
    public void addDTD(DTDLocation dtd) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }

        getElements().addElement(dtd);
        checked = false;
    }

    /**
     * Creates the nested <code>&lt;entity&gt;</code> element.    Not
     * allowed if this catalog is itself a reference to another
     * catalog -- that is, a catalog cannot both refer to another
     * <em>and</em> contain elements or other attributes.
     *
     * @param dtd the information about the URI resource mapping to be
     *       added to the catalog
     * @exception BuildException if this is a reference and no nested
     *       elements are allowed.
     */
    public void addEntity(DTDLocation dtd) throws BuildException {
        addDTD(dtd);
    }

    /**
     * Loads a nested <code>&lt;xmlcatalog&gt;</code> into our
     * definition.  Not allowed if this catalog is itself a reference
     * to another catalog -- that is, a catalog cannot both refer to
     * another <em>and</em> contain elements or other attributes.
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
        checked = false;
    }

    /**
     * Makes this instance in effect a reference to another XMLCatalog
     * instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.  That is, a catalog
     * cannot both refer to another <em>and</em> contain elements or
     * attributes.</p>
     *
     * @param r the reference to which this catalog instance is associated
     * @exception BuildException if this instance already has been configured.
     */
    public void setRefid(Reference r) throws BuildException {
        if (!elements.isEmpty()) {
            throw tooManyAttributes();
        }
        // change this to get the objects from the other reference
        Object o = r.getReferencedObject(getProject());
        // we only support references to other XMLCatalogs
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
     * Implements the EntityResolver.resolveEntity() interface method.
     *
     * @see org.xml.sax.EntityResolver#resolveEntity
     */
    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException, IOException {

       if (!checked) {
          // make sure we don't have a circular reference here
          Stack stk = new Stack();
          stk.push(this);
          dieOnCircularReference(stk, getProject());
       }

        log("resolveEntity: '" + publicId + "': '" + systemId + "'",
            Project.MSG_DEBUG);

        InputSource inputSource = resolveEntityImpl(publicId );

        if (inputSource == null) {
            log("No matching catalog entry found, parser will use: '" +
                systemId + "'", Project.MSG_DEBUG);
        }

        return inputSource;
    }

    /**
     * Implements the URIResolver.resolve() interface method.
     *
     * @see javax.xml.transform.URIResolver#resolve
     */
    public Source resolve(String href, String base)
        throws TransformerException {

       if (!checked) {
          // make sure we don't have a circular reference here
          Stack stk = new Stack();
          stk.push(this);
          dieOnCircularReference(stk, getProject());
       }

        SAXSource source = null;

        String uri = removeFragment(href);

        log("resolve: '" + uri + "' with base: '" + base + "'", Project.MSG_DEBUG);

        source = resolveImpl(uri, base);

        if (source == null) {
            log("No matching catalog entry found, parser will use: '" +
                href + "'", Project.MSG_DEBUG);
            //
            // Cannot return a null source, because we have to call
            // setEntityResolver (see setEntityResolver javadoc comment)
            //
            source = new SAXSource();
            try
            {
                URL baseURL = new URL(base);
                URL url = (uri.length() == 0 ? baseURL : new URL(baseURL, uri));
                source.setInputSource(new InputSource(url.toString()));
            }
            catch (MalformedURLException ex) {
                // At this point we are probably in failure mode, but
                // try to use the bare URI as a last gasp
                source.setInputSource(new InputSource(uri));
            }
        }

        setEntityResolver(source);
        return source;
    }

    /**
     * Find a DTDLocation instance for the given publicId.
     *
     * @param publicId the publicId of the Resource for which local information
     *        is required
     * @return a DTDLocation instance with information on the local location
     *         of the Resource or null if no such information is available
     */
    private DTDLocation findMatchingEntry(String publicId) {
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

    /**
     * <p>This is called from the URIResolver to set an EntityResolver
     * on the SAX parser to be used for new XML documents that are
     * encountered as a result of the document() function, xsl:import,
     * or xsl:include.  This is done because the XSLT processor calls
     * out to the SAXParserFactory itself to create a new SAXParser to
     * parse the new document.  The new parser does not automatically
     * inherit the EntityResolver of the original (although arguably
     * it should).  See below:</p>
     *
     * <tt>"If an application wants to set the ErrorHandler or
     * EntityResolver for an XMLReader used during a transformation,
     * it should use a URIResolver to return the SAXSource which
     * provides (with getXMLReader) a reference to the XMLReader"</tt>
     *
     * <p>...quoted from page 118 of the Java API for XML
     * Processing 1.1 specification</p>
     *
     */
    private void setEntityResolver(SAXSource source) throws TransformerException {

        XMLReader reader = source.getXMLReader();
        if (reader == null) {
            SAXParserFactory spFactory = SAXParserFactory.newInstance();
            spFactory.setNamespaceAware(true);
            try {
                reader = spFactory.newSAXParser().getXMLReader();
            }
            catch (ParserConfigurationException ex) {
                throw new TransformerException(ex);
            }
            catch (SAXException ex) {
                throw new TransformerException(ex);
            }
        }
        reader.setEntityResolver(this);
        source.setXMLReader(reader);
    }

    /**
     * Utility method to remove trailing fragment from a URI.
     * For example,
     * <code>http://java.sun.com/index.html#chapter1</code>
     * would return <code>http://java.sun.com/index.html</code>.
     *
     * @param uri The URI to process.  It may or may not contain a
     *            fragment.
     * @return The URI sans fragment.
     */
    private String removeFragment(String uri) {
        String result = uri;
        int hashPos = uri.indexOf("#");
        if (hashPos >= 0) {
            result = uri.substring(0, hashPos);
        }
        return result;
    }

    /**
     * Utility method to lookup a DTDLocation in the filesystem.
     *
     * @return An InputSource for reading the file, or <code>null</code>
     *     if the file does not exist or is not readable.
     */
    private InputSource filesystemLookup(DTDLocation matchingEntry) {

        String uri = matchingEntry.getLocation();

        //
        // The DTDLocation may specify a relative path for its
        // location attribute.  This is resolved using the appropriate
        // base.
        //
        File resFile = getProject().resolveFile(uri);
        InputSource source = null;

        if (resFile.exists() && resFile.canRead()) {
            try {
                source = new InputSource(new FileInputStream(resFile));
                URL resFileURL = fileUtils.getFileURL(resFile);
                String sysid = resFileURL.toExternalForm();
                source.setSystemId(sysid);
                log("catalog entry matched a readable file: '" +
                    sysid + "'", Project.MSG_DEBUG);
            } catch(FileNotFoundException ex) {
                // ignore
            } catch(MalformedURLException ex) {
                // ignore
            } catch(IOException ex) {
                // ignore
            }
        }

        return source;
    }

    /**
     * Utility method to lookup a DTDLocation in the classpath.
     *
     * @return An InputSource for reading the resource, or <code>null</code>
     *    if the resource does not exist in the classpath or is not readable.
     */
    private InputSource classpathLookup(DTDLocation matchingEntry) {

        InputSource source = null;

        AntClassLoader loader = null;
        if (classpath != null) {
            loader = new AntClassLoader(getProject(), classpath);
        } else {
            loader = new AntClassLoader(getProject(), Path.systemClasspath);
        }

        //
        // for classpath lookup we ignore the base directory
        //
        InputStream is
            = loader.getResourceAsStream(matchingEntry.getLocation());

        if (is != null) {
            source = new InputSource(is);
            URL entryURL = loader.getResource(matchingEntry.getLocation());
            String sysid = entryURL.toExternalForm();
            source.setSystemId(sysid);
            log("catalog entry matched a resource in the classpath: '" +
                sysid + "'", Project.MSG_DEBUG);
        }

        return source;
    }

    /**
     * Utility method to lookup a DTDLocation in URL-space.
     *
     * @return An InputSource for reading the resource, or <code>null</code>
     *    if the resource does not identify a valid URL or is not readable.
     */
    private InputSource urlLookup(String uri, String base) {

        InputSource source = null;
        URL url = null;

        try {
            if (base == null) {
                url = new URL(uri);
            }
            else {
                URL baseURL = new URL(base);
                url = (uri.length() == 0 ? baseURL : new URL(baseURL, uri));
            }
        }
        catch (MalformedURLException ex) {
            // ignore
        }

        if (url != null) {
            try {
                InputStream is = url.openStream();
                if (is != null) {
                    source = new InputSource(is);
                    String sysid = url.toExternalForm();
                    source.setSystemId(sysid);
                    log("catalog entry matched as a URL: '" +
                        sysid + "'", Project.MSG_DEBUG);
                }
            } catch(IOException ex) {
                // ignore
            }
        }

        return source;

    }

    /**
     * Implements the guts of the resolveEntity() lookup strategy.
     */
    private InputSource resolveEntityImpl(String publicId) {

        InputSource result = null;

        DTDLocation matchingEntry = findMatchingEntry(publicId);

        if (matchingEntry != null) {

            log("Matching catalog entry found for publicId: '" +
                matchingEntry.getPublicId() + "' location: '" +
                matchingEntry.getLocation() + "'",
                Project.MSG_DEBUG);

            result = filesystemLookup(matchingEntry);

            if (result == null) {
                result = classpathLookup(matchingEntry);
            }

            if (result == null) {
                result = urlLookup(matchingEntry.getLocation(), null);
            }
        }
        return result;
    }

    /**
     * Implements the guts of the resolve() lookup strategy.
     */
    private SAXSource resolveImpl(String href, String base) {

        SAXSource result = null;
        InputSource source = null;

        DTDLocation matchingEntry = findMatchingEntry(href);

        if (matchingEntry != null) {

            log("Matching catalog entry found for uri: '" +
                matchingEntry.getPublicId() + "' location: '" +
                matchingEntry.getLocation() + "'",
                Project.MSG_DEBUG);

            source = filesystemLookup(matchingEntry);

            if (source == null) {
                source = classpathLookup(matchingEntry);
            }

            if (source == null) {
                source = urlLookup(matchingEntry.getLocation(), base);
            }

            if (source != null) {
                result = new SAXSource(source);
            }
        }
        return result;
    }
}
