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

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogEntry;

import org.apache.xml.resolver.helpers.PublicId;

/**
 * This class extends the Catalog class provided by Norman Walsh's
 * resolver library in xml-commons in order to add classpath entity
 * and URI resolution.  Since XMLCatalog already does classpath
 * resolution, we simply add all CatalogEntry instances back to the
 * controlling XMLCatalog instance.  This is done via a callback
 * mechanism.  ApacheCatalog is <em>only</em> used for external
 * catalog files.  Inline entries (currently <code>&lt;dtd&gt;</code>
 * and <code>&lt;entity&gt;</code>) are not added to ApacheCatalog.
 * See XMLCatalog.java for the details of the entity and URI
 * resolution algorithms.
 *
 * @see org.apache.tools.ant.types.XMLCatalog.CatalogResolver
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
 * @version $Id$
 * @since Ant 1.6
 */
public class ApacheCatalog extends Catalog {

    /** The resolver object to callback. */
    private ApacheCatalogResolver resolver = null;

    /**
     * <p>Create a new ApacheCatalog instance.</p>
     *
     * <p>This method overrides the superclass method of the same name
     *  in order to set the resolver object for callbacks.  The reason
     *  we have to do this is that internally Catalog creates a new
     *  instance of itself for each external catalog file processed.
     *  That is, if two external catalog files are processed, there
     *  will be a total of two ApacheCatalog instances, and so on.</p>
     */
    protected Catalog newCatalog() {
        ApacheCatalog cat = (ApacheCatalog)super.newCatalog();
        cat.setResolver(resolver);
        return cat;
    }
    
    /** Set the resolver object to callback. */
    public void setResolver(ApacheCatalogResolver resolver) {
        this.resolver = resolver;
    }
    
    /**
     * <p>This method overrides the superclass method of the same name
     * in order to add catalog entries back to the controlling
     * XMLCatalog instance.  In this way, we can add classpath lookup
     * for these entries.</p>
     *
     * <p>When we add an external catalog file, the entries inside it
     * get parsed by this method.  Therefore, we override it to add
     * each of them back to the controlling XMLCatalog instance.  This
     * is done by performing a callback to the ApacheCatalogResolver,
     * which in turn calls the XMLCatalog.</p>
     *
     * <p>XMLCatalog currently only understands <code>PUBLIC</code>
     * and <code>URI</code> entry types, so we ignore the other types.</p>
     *
     * @param entry The CatalogEntry to process.
     */
    public void addEntry(CatalogEntry entry) {

        int type = entry.getEntryType();

        if (type == PUBLIC) {

            String publicid = PublicId.normalize(entry.getEntryArg(0));
            String systemid = normalizeURI(entry.getEntryArg(1));

            if (resolver == null) {
                catalogManager.debug
                    .message(1, "Internal Error: null ApacheCatalogResolver");
            }
            else {
                resolver.addPublicEntry(publicid, systemid, base);
            }

        } else if (type == URI) {
            
            String uri = normalizeURI(entry.getEntryArg(0));
            String altURI = normalizeURI(entry.getEntryArg(1));

            if (resolver == null) {
                catalogManager.debug
                    .message(1, "Internal Error: null ApacheCatalogResolver");
            }
            else {
                resolver.addURIEntry(uri, altURI, base);
            }

        }
        
        super.addEntry(entry);
    }

} //- ApacheCatalog
