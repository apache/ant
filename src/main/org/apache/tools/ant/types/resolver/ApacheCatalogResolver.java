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

package org.apache.tools.ant.types.resolver;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.tools.ant.BuildException;

import org.apache.tools.ant.types.XMLCatalog;
import org.apache.tools.ant.types.ResourceLocation;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;

import org.apache.xml.resolver.tools.CatalogResolver;

/**
 * <p>This class extends the CatalogResolver class provided by Norman
 * Walsh's resolver library in xml-commons.  It provides the bridge
 * between the Ant XMLCatalog datatype and the xml-commons Catalog
 * class.  XMLCatalog calls methods in this class using Reflection in
 * order to avoid requiring the xml-commons resolver library in the
 * path.</p>
 *
 * <p>The {@link org.apache.tools.ant.types.resolver.ApacheCatalog
 * ApacheCatalog} class is used to parse external catalog files, which
 * can be in either <a
 * href="http://oasis-open.org/committees/entity/background/9401.html">
 * plain text format</a> or <a
 * href="http://www.oasis-open.org/committees/entity/spec-2001-08-06.html">
 * XML format</a>.</p>
 *
 * <p>For each entry found in an external catalog file, if any, an
 * instance of {@link org.apache.tools.ant.types.ResourceLocation
 * ResourceLocation} is created and added to the controlling
 * XMLCatalog datatype.  In this way, these entries will be included
 * in XMLCatalog's lookup algorithm.  See XMLCatalog.java for more
 * details.</p>
 *
 * @see org.apache.tools.ant.types.XMLCatalog.CatalogResolver
 * @see org.apache.xml.resolver.CatalogManager
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
 * @version $Id$
 * @since Ant 1.6
 */

public class ApacheCatalogResolver extends CatalogResolver {

    /** The XMLCatalog object to callback. */
    private XMLCatalog xmlCatalog = null;

    static {
        //
        // If you don't do this, you get all sorts of annoying
        // warnings about a missing properties file.  However, it
        // seems to work just fine with default values.  Ultimately,
        // we should probably include a "CatalogManager.properties"
        // file in the ant jarfile with some default property
        // settings.  See CatalogManager.java for more details.
        //
        CatalogManager.getStaticManager().setIgnoreMissingProperties(true);

        //
        // Make sure CatalogResolver instantiates ApacheCatalog,
        // rather than a plain Catalog
        //
        System.getProperties().put("xml.catalog.className",
                                   ApacheCatalog.class.getName());

        CatalogManager.getStaticManager().setUseStaticCatalog(false);

        // debug
        // CatalogManager.getStaticManager().setVerbosity(4);
    }

    /** Set the XMLCatalog object to callback. */
    public void setXMLCatalog(XMLCatalog xmlCatalog) {
        this.xmlCatalog = xmlCatalog;
    }

    /**
     * XMLCatalog calls this to add an external catalog file for each
     * file within a <code>&lt;catalogfiles&gt;</code> fileset.
     */
    public void parseCatalog(String file) {

        Catalog _catalog = getCatalog();
        if (!(_catalog instanceof ApacheCatalog)) {
            throw new BuildException("Wrong catalog type found: " + _catalog.getClass().getName());
        }
        ApacheCatalog catalog = (ApacheCatalog) _catalog;

        // Pass in reference to ourselves so we can be called back.
        catalog.setResolver(this);

        try {
            catalog.parseCatalog(file);
        } catch (MalformedURLException ex) {
            throw new BuildException(ex);
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
    }

    /**
     * <p>Add a PUBLIC catalog entry to the controlling XMLCatalog instance.
     * ApacheCatalog calls this for each PUBLIC entry found in an external
     * catalog file.</p>
     *
     * @param publicid The public ID of the resource
     * @param systemid The system ID (aka location) of the resource
     * @param base The base URL of the resource.  If the systemid
     * specifies a relative URL/pathname, it is resolved using the
     * base.  The default base for an external catalog file is the
     * directory in which the catalog is located.
     *
     */
    public void addPublicEntry(String publicid,
                               String systemid,
                               URL base) {

        ResourceLocation dtd = new ResourceLocation();
        dtd.setBase(base);
        dtd.setPublicId(publicid);
        dtd.setLocation(systemid);

        xmlCatalog.addDTD(dtd);
    }

    /**
     * <p>Add a URI catalog entry to the controlling XMLCatalog instance.
     * ApacheCatalog calls this for each URI entry found in an external
     * catalog file.</p>
     *
     * @param uri The URI of the resource
     * @param altURI The URI to which the resource should be mapped
     * (aka the location)
     * @param base The base URL of the resource.  If the altURI
     * specifies a relative URL/pathname, it is resolved using the
     * base.  The default base for an external catalog file is the
     * directory in which the catalog is located.
     *
     */
    public void addURIEntry(String uri,
                            String altURI,
                            URL base) {

        ResourceLocation entity = new ResourceLocation();
        entity.setBase(base);
        entity.setPublicId(uri);
        entity.setLocation(altURI);

        xmlCatalog.addEntity(entity);
    }

} //-- ApacheCatalogResolver
