/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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

import java.lang.reflect.Method;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.JAXPUtils;
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
 * <p>Resource locations can be specified either in-line or in
 * external catalog file(s), or both.  In order to use an external
 * catalog file, the xml-commons resolver library ("resolver.jar")
 * must be in your classpath.  External catalog files may be either <a
 * href="http://oasis-open.org/committees/entity/background/9401.html">
 * plain text format</a> or <a
 * href="http://www.oasis-open.org/committees/entity/spec-2001-08-06.html">
 * XML format</a>.  If the xml-commons resolver library is not found
 * in the classpath, external catalog files, specified in
 * <code>&lt;catalogpath&gt;</code> paths, will be ignored and a warning will
 * be logged.  In this case, however, processing of inline entries will proceed
 * normally.</p>
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
 * &nbsp;&nbsp;&lt;catalogpath&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/etc/sgml/catalog"/&gt;<br>
 * &nbsp;&nbsp;&lt;/catalogpath&gt;<br>
 * &nbsp;&nbsp;&lt;catalogfiles dir="/opt/catalogs/" includes="**\catalog.xml" /&gt;<br>
 * &lt;/xmlcatalog&gt;<br>
 * </code>
 * <p>
 * Tasks wishing to use <code>&lt;xmlcatalog&gt;</code> must provide a method called
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
 * <li>Using the Apache xml-commons resolver (if it is available)</li>
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
 * entry types to be specified inline.</p>
 *
 * @author dIon Gillard
 * @author Erik Hatcher
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
 * @author Jeff Turner
 * @version $Id$
 */
public class XMLCatalog extends DataType 
    implements Cloneable, EntityResolver, URIResolver {

    //-- Fields ----------------------------------------------------------------

    /** Holds dtd/entity objects until needed. */
    private Vector elements = new Vector();

    /**
     * Classpath in which to attempt to resolve resources.
     */
    private Path classpath;

    /**
     * Path listing external catalog files to search when resolving entities
     */
    private Path catalogPath;
   
    /**
     * The name of the bridge to the Apache xml-commons resolver
     * class, used to determine whether resolver.jar is present in the
     * classpath.
     */
    public static final String APACHE_RESOLVER
        = "org.apache.tools.ant.types.resolver.ApacheCatalogResolver";

    //-- Methods ---------------------------------------------------------------

    public XMLCatalog() {
        setChecked( false );
    }

    /**
     * Returns the elements of the catalog - ResourceLocation objects.
     *
     * @return the elements of the catalog - ResourceLocation objects
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
     * Set the list of ResourceLocation objects in the catalog.
     * Not allowed if this catalog is itself a reference to another catalog --
     * that is, a catalog cannot both refer to another <em>and</em> contain
     * elements or other attributes.
     *
     * @param aVector the new list of ResourceLocations
     * to use in the catalog.
     */
    private void setElements(Vector aVector) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
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
        setChecked( false );
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
        setChecked( false );
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
        setChecked( false );
    }

    /** Creates a nested <code>&lt;catalogpath&gt;</code> element.
     * Not allowed if this catalog is itself a reference to another
     * catalog -- that is, a catalog cannot both refer to another
     * <em>and</em> contain elements or other attributes.
     *
     * @exception BuildException
     * if this is a reference and no nested elements are allowed.
     */
    public Path createCatalogPath() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.catalogPath == null) {
            this.catalogPath = new Path(getProject());
        }
        setChecked( false );
        return this.catalogPath.createPath();
    }

    /**
     * Allows catalogpath reference.  Not allowed if this catalog is
     * itself a reference to another catalog -- that is, a catalog
     * cannot both refer to another <em>and</em> contain elements or
     * other attributes.
     */
    public void setCatalogPathRef(Reference r) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createCatalogPath().setRefid(r);
        setChecked( false );
    }


    /**
     * Returns the catalog path in which to attempt to resolve DTDs.
     *
     * @return the catalog path
     */
    public Path getCatalogPath() {
        return this.catalogPath;
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
    public void addDTD(ResourceLocation dtd) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }

        getElements().addElement(dtd);
        setChecked( false );
    }

    /**
     * Creates the nested <code>&lt;entity&gt;</code> element.    Not
     * allowed if this catalog is itself a reference to another
     * catalog -- that is, a catalog cannot both refer to another
     * <em>and</em> contain elements or other attributes.
     *
     * @param entity the information about the URI resource mapping to be
     *       added to the catalog.
     * @exception BuildException if this is a reference and no nested
     *       elements are allowed.
     */
    public void addEntity(ResourceLocation entity) throws BuildException {
        addDTD(entity);
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

        // Append the catalog path of the nested catalog
        Path nestedCatalogPath = catalog.getCatalogPath();
        createCatalogPath().append(nestedCatalogPath);
        setChecked( false );
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

        if (!isChecked()) {
            // make sure we don't have a circular reference here
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, getProject());
        }

        log("resolveEntity: '" + publicId + "': '" + systemId + "'",
            Project.MSG_DEBUG);

        InputSource inputSource = 
            getCatalogResolver().resolveEntity(publicId, systemId);

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

        if (!isChecked()) {
            // make sure we don't have a circular reference here
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, getProject());
        }

        SAXSource source = null;
        
        String uri = removeFragment(href);

        log("resolve: '" + uri + "' with base: '" + base + "'", Project.MSG_DEBUG);

        source = (SAXSource)getCatalogResolver().resolve(uri, base);

        if (source == null) {
            log("No matching catalog entry found, parser will use: '" +
                href + "'", Project.MSG_DEBUG);
            //
            // Cannot return a null source, because we have to call
            // setEntityResolver (see setEntityResolver javadoc comment)
            //
            source = new SAXSource();
            URL baseURL = null;
            try {
                if (base == null) {
                    baseURL = getProject().getBaseDir().toURL();
                }
                else {
                    baseURL = new URL(base);
                }
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
     * The instance of the CatalogResolver strategy to use.
     */
    private CatalogResolver catalogResolver = null;

    /**
     * Factory method for creating the appropriate CatalogResolver
     * strategy implementation.  
     * <p> Until we query the classpath, we don't know whether the Apache
     * resolver (Norm Walsh's library from xml-commons) is available or not.
     * This method determines whether the library is available and creates the
     * appropriate implementation of CatalogResolver based on the answer.</p>
     * <p>This is an application of the Gang of Four Strategy Pattern
     * combined with Template Method.</p>
     */
    private CatalogResolver getCatalogResolver() {

        if (catalogResolver == null) {

            AntClassLoader loader = null;

            loader = new AntClassLoader(getProject(), Path.systemClasspath);

            try {
                Class clazz = loader.forceLoadSystemClass(APACHE_RESOLVER);
                Object obj  = clazz.newInstance();
                //
                // Success!  The xml-commons resolver library is
                // available, so use it.
                //
                catalogResolver = new ApacheResolver(clazz, obj);
            } 
            catch (Throwable ex) {
                //
                // The xml-commons resolver library is not 
                // available, so we can't use it.
                //
                catalogResolver = new InternalResolver();
                if (getCatalogPath() != null &&
                    getCatalogPath().list().length != 0) {
                        log("Warning: catalogpath listing external catalogs"+
                                " will be ignored",
                            Project.MSG_WARN);
                }
            }
        }
        return catalogResolver;
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
     * Find a ResourceLocation instance for the given publicId.
     *
     * @param publicId the publicId of the Resource for which local information
     *        is required.
     * @return a ResourceLocation instance with information on the local location 
     *         of the Resource or null if no such information is available.
     */
    private ResourceLocation findMatchingEntry(String publicId) {
        Enumeration enum = getElements().elements();
        ResourceLocation element = null;
        while (enum.hasMoreElements()) {
            Object o = enum.nextElement();
            if (o instanceof ResourceLocation) {
                element = (ResourceLocation)o;
                if (element.getPublicId().equals(publicId)) {
                    return element;
                }
            }
        }
        return null;
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
     * Utility method to lookup a ResourceLocation in the filesystem.
     *
     * @return An InputSource for reading the file, or <code>null</code>
     *     if the file does not exist or is not readable.
     */
    private InputSource filesystemLookup(ResourceLocation matchingEntry) {

        String uri = matchingEntry.getLocation();
        URL baseURL = null;

        //
        // The ResourceLocation may specify a relative path for its
        // location attribute.  This is resolved using the appropriate
        // base.
        //
        if (matchingEntry.getBase() != null) {
            baseURL = matchingEntry.getBase();
        } else {
            try {
                baseURL = getProject().getBaseDir().toURL();                
            }
            catch (MalformedURLException ex) {
                throw new BuildException("Project basedir cannot be converted to a URL");
            }
        }
        
        InputSource source = null;
        URL url = null;
        
        try {
            url = new URL(baseURL, uri);
        }
        catch (MalformedURLException ex) {
            // ignore
        }
        
        if (url != null) {
            String fileName = url.getFile();
            if (fileName != null) {
                log("fileName"+fileName, Project.MSG_DEBUG);
                File resFile = new File(fileName);
                if (resFile.exists() && resFile.canRead()) {
                    try {
                        source = new InputSource(new FileInputStream(resFile));
                        String sysid = JAXPUtils.getSystemId(resFile);
                        source.setSystemId(sysid);
                        log("catalog entry matched a readable file: '" +
                            sysid + "'", Project.MSG_DEBUG);
                    } catch(FileNotFoundException ex) {
                        // ignore
                    } catch(IOException ex) {
                        // ignore
                    }
                }
            }
        }
        return source;
    }

    /**
     * Utility method to lookup a ResourceLocation in the classpath.
     *
     * @return An InputSource for reading the resource, or <code>null</code>
     *    if the resource does not exist in the classpath or is not readable.
     */
    private InputSource classpathLookup(ResourceLocation matchingEntry) {

        InputSource source = null;

        AntClassLoader loader = null;
        Path cp = classpath;
        if (cp != null) {
            cp = classpath.concatSystemClasspath("ignore");
        } else {
            cp = (new Path(getProject())).concatSystemClasspath("last");
        }
        loader = new AntClassLoader(getProject(), cp);

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
     * Utility method to lookup a ResourceLocation in URL-space.
     *
     * @return An InputSource for reading the resource, or <code>null</code>
     *    if the resource does not identify a valid URL or is not readable.
     */
    private InputSource urlLookup(ResourceLocation matchingEntry) {
        
        String uri = matchingEntry.getLocation();
        URL baseURL = null;

        //
        // The ResourceLocation may specify a relative url for its
        // location attribute.  This is resolved using the appropriate
        // base.
        //
        if (matchingEntry.getBase() != null) {
            baseURL = matchingEntry.getBase();
        } else {
            try {
                baseURL = getProject().getBaseDir().toURL();
            }
            catch (MalformedURLException ex) {
                throw new BuildException("Project basedir cannot be converted to a URL");
            }
        }

        InputSource source = null;
        URL url = null;

        try {
            url = new URL(baseURL, uri);
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
     * Interface implemented by both the InternalResolver strategy and
     * the ApacheResolver strategy.
     */
    private interface CatalogResolver extends URIResolver, EntityResolver {

        InputSource resolveEntity(String publicId, String systemId);

        Source resolve(String href, String base) throws TransformerException;
    }

    /**
     * The InternalResolver strategy is used if the Apache resolver
     * library (Norm Walsh's library from xml-commons) is not
     * available.  In this case, external catalog files will be
     * ignored.
     * 
     */
    private class InternalResolver implements CatalogResolver {

        public InternalResolver() {
            log("Apache resolver library not found, internal resolver will be used",
                Project.MSG_VERBOSE);
        }

        public InputSource resolveEntity(String publicId,
                                         String systemId) {
            InputSource result = null;
            ResourceLocation matchingEntry = findMatchingEntry(publicId);

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
                    result = urlLookup(matchingEntry);
                }
            }
            return result;
        }

        public Source resolve(String href, String base)
            throws TransformerException {

            SAXSource result = null;
            InputSource source = null;

            ResourceLocation matchingEntry = findMatchingEntry(href);

            if (matchingEntry != null) {

                log("Matching catalog entry found for uri: '" + 
                    matchingEntry.getPublicId() + "' location: '" + 
                    matchingEntry.getLocation() + "'",
                    Project.MSG_DEBUG);

                //
                // Use the passed in base in preference to the base
                // from matchingEntry, which is either null or the
                // directory in which the external catalog file from
                // which it was obtained is located.  We make a copy
                // so matchingEntry's original base is untouched.
                //
                // This is the standard behavior as per my reading of
                // the JAXP and XML Catalog specs.  CKS 11/7/2002
                // 
                ResourceLocation entryCopy = matchingEntry;
                if (base != null) {
                    try {
                        URL baseURL = new URL(base);
                        entryCopy = new ResourceLocation();
                        entryCopy.setBase(baseURL);
                    }
                    catch (MalformedURLException ex) {
                        // ignore
                    }
                }
                entryCopy.setPublicId(matchingEntry.getPublicId());
                entryCopy.setLocation(matchingEntry.getLocation());

                source = filesystemLookup(entryCopy);

                if (source == null) {
                    source = classpathLookup(entryCopy);
                }

                if (source == null) {
                    source = urlLookup(entryCopy);
                }

                if (source != null) {
                    result = new SAXSource(source);
                }
            }
            return result;
        }
    }

    /**
     * The ApacheResolver strategy is used if the Apache resolver
     * library (Norm Walsh's library from xml-commons) is available in
     * the classpath.  The ApacheResolver is a essentially a superset
     * of the InternalResolver.
     * 
     */
    private class ApacheResolver implements CatalogResolver {

        private Method setXMLCatalog = null;
        private Method parseCatalog = null;
        private Method resolveEntity = null;
        private Method resolve = null;

        /** The instance of the ApacheCatalogResolver bridge class */
        private Object resolverImpl = null;

        private boolean externalCatalogsProcessed = false;

        public ApacheResolver(Class resolverImplClass, 
                              Object resolverImpl) {

            this.resolverImpl = resolverImpl;

            //
            // Get Method instances for each of the methods we need to
            // call on the resolverImpl using reflection.  We can't
            // call them directly, because they require on the
            // xml-commons resolver library which may not be available
            // in the classpath.
            //
            try {
                setXMLCatalog =
                    resolverImplClass.getMethod("setXMLCatalog",
                                                new Class[] 
                        { XMLCatalog.class });

                parseCatalog =
                    resolverImplClass.getMethod("parseCatalog",
                                                new Class[] 
                        { String.class });

                resolveEntity =
                    resolverImplClass.getMethod("resolveEntity",
                                                new Class[] 
                        { String.class, String.class });

                resolve =
                    resolverImplClass.getMethod("resolve",
                                                new Class[] 
                        { String.class, String.class });
            }
            catch (NoSuchMethodException ex) {
                throw new BuildException(ex);
            }

            log("Apache resolver library found, xml-commons resolver will be used",
                Project.MSG_VERBOSE);
        }

        public InputSource resolveEntity(String publicId,
                                         String systemId) {
            InputSource result = null;

            processExternalCatalogs();

            ResourceLocation matchingEntry = findMatchingEntry(publicId);

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
                    try {
                        result = 
                            (InputSource)resolveEntity.invoke(resolverImpl,
                                                              new Object[]
                                { publicId, systemId });
                    }
                    catch (Exception ex) {
                        throw new BuildException(ex);
                    }
                }
            }
            else {
                //
                // We didn't match a ResourceLocation, but since we
                // only support PUBLIC and URI entry types internally,
                // it is still possible that there is another entry in
                // an external catalog that will match.  We call
                // Apache resolver's resolveEntity method to cover
                // this possibility.
                //
                try {
                    result = 
                        (InputSource)resolveEntity.invoke(resolverImpl,
                                                          new Object[]
                            { publicId, systemId });
                }
                catch (Exception ex) {
                    throw new BuildException(ex);
                }
            }

            return result;
        }

        public Source resolve(String href, String base)
            throws TransformerException {

            SAXSource result = null;
            InputSource source = null;

            processExternalCatalogs();

            ResourceLocation matchingEntry = findMatchingEntry(href);

            if (matchingEntry != null) {

                log("Matching catalog entry found for uri: '" + 
                    matchingEntry.getPublicId() + "' location: '" + 
                    matchingEntry.getLocation() + "'",
                    Project.MSG_DEBUG);

                //
                // Use the passed in base in preference to the base
                // from matchingEntry, which is either null or the
                // directory in which the external catalog file from
                // which it was obtained is located.  We make a copy
                // so matchingEntry's original base is untouched.  Of
                // course, if there is no base, no need to make a
                // copy...
                //
                // This is the standard behavior as per my reading of
                // the JAXP and XML Catalog specs.  CKS 11/7/2002
                // 
                ResourceLocation entryCopy = matchingEntry;
                if (base != null) {
                    try {
                        URL baseURL = new URL(base);
                        entryCopy = new ResourceLocation();
                        entryCopy.setBase(baseURL);
                    }
                    catch (MalformedURLException ex) {
                        // ignore
                    }
                }
                entryCopy.setPublicId(matchingEntry.getPublicId());
                entryCopy.setLocation(matchingEntry.getLocation());

                source = filesystemLookup(entryCopy);

                if (source == null) {
                    source = classpathLookup(entryCopy);
                }

                if (source != null) {
                    result = new SAXSource(source);
                } else {
                    try {
                        result = 
                            (SAXSource)resolve.invoke(resolverImpl,
                                                      new Object[]
                                { href, base });
                    }
                    catch (Exception ex) {
                        throw new BuildException(ex);
                    }
                }
            }
            else {
                //
                // We didn't match a ResourceLocation, but since we
                // only support PUBLIC and URI entry types internally,
                // it is still possible that there is another entry in
                // an external catalog that will match.  We call
                // Apache resolver's resolveEntity method to cover
                // this possibility.
                //
                try {
                    result = 
                        (SAXSource)resolve.invoke(resolverImpl,
                                                  new Object[]
                            { href, base });
                }
                catch (Exception ex) {
                    throw new BuildException(ex);
                }
            }
            return result;
        }

        /**
         * Process each external catalog file specified in a
         * <code>&lt;catalogpath&gt;</code>.  It will be
         * parsed by the resolver library, and the individual elements
         * will be added back to us (that is, the controlling
         * XMLCatalog instance) via a callback mechanism.
         */
        private void processExternalCatalogs() {

            if (externalCatalogsProcessed == false) {

                try {
                    setXMLCatalog.invoke(resolverImpl, 
                                         new Object[] 
                    { XMLCatalog.this });
                }
                catch (Exception ex) {
                    throw new BuildException(ex);
                }

                // Parse each catalog listed in nested <catalogpath> elements
                Path catPath = getCatalogPath();
                if (catPath != null) {
                    log("Using catalogpath '" + getCatalogPath()+"'", Project.MSG_DEBUG);
                    String[] catPathList = getCatalogPath().list();

                    for (int i=0; i< catPathList.length; i++) {
                        File catFile = new File(catPathList[i]); 
                        log("Parsing "+catFile, Project.MSG_DEBUG);
                        try {
                            parseCatalog.invoke(resolverImpl, 
                                    new Object[] 
                                    { catFile.getPath() });
                        }
                        catch (Exception ex) {
                            throw new BuildException(ex);
                        }
                    }
                }
            }
            externalCatalogsProcessed = true;
        }
    }
} //-- XMLCatalog
