/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types.resolver;

import java.io.IOException;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.ResourceLocation;
import org.apache.tools.ant.types.XMLCatalog;
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
 * href="https://oasis-open.org/committees/entity/background/9401.html">
 * plain text format</a> or <a
 * href="https://www.oasis-open.org/committees/entity/spec-2001-08-06.html">
 * XML format</a>.</p>
 *
 * <p>For each entry found in an external catalog file, if any, an
 * instance of {@link org.apache.tools.ant.types.ResourceLocation
 * ResourceLocation} is created and added to the controlling
 * XMLCatalog datatype.  In this way, these entries will be included
 * in XMLCatalog's lookup algorithm.  See XMLCatalog.java for more
 * details.</p>
 *
 * @see org.apache.tools.ant.types.XMLCatalog
 * @see org.apache.xml.resolver.CatalogManager
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

    /**
     * Set the XMLCatalog object to callback.
     * @param xmlCatalog the XMLCatalog to use.
     */
    public void setXMLCatalog(final XMLCatalog xmlCatalog) {
        this.xmlCatalog = xmlCatalog;
    }

    /**
     * XMLCatalog calls this to add an external catalog file for each
     * file within a <code>&lt;catalogfiles&gt;</code> fileset.
     * @param file the external catalog file.
     */
    public void parseCatalog(final String file) {

        final Catalog catalog = getCatalog();
        if (!(catalog instanceof ApacheCatalog)) {
            throw new BuildException("Wrong catalog type found: %s", catalog.getClass().getName());
        }
        final ApacheCatalog apacheCatalog = (ApacheCatalog) catalog;

        // Pass in reference to ourselves so we can be called back.
        apacheCatalog.setResolver(this);

        try {
            apacheCatalog.parseCatalog(file);
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
    public void addPublicEntry(final String publicid,
                               final String systemid,
                               final URL base) {

        final ResourceLocation dtd = new ResourceLocation();
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
    public void addURIEntry(final String uri,
                            final String altURI,
                            final URL base) {

        final ResourceLocation entity = new ResourceLocation();
        entity.setBase(base);
        entity.setPublicId(uri);
        entity.setLocation(altURI);

        xmlCatalog.addEntity(entity);
    }

}
