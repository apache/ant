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
package org.apache.ant.antcore.config;
import org.apache.ant.antcore.util.ConfigException;
import org.apache.ant.antcore.xml.ElementHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

/**
 * XML Element Handler for Ant config files
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 20 January 2002
 */
public class AntConfigHandler extends ElementHandler {
    /**
     * The config object which is contructed from the XML representation of
     * the config
     */
    private AntConfig config;

    /**
     * Get the Ant Config read in by this handler
     *
     * @return the AntConfig instance
     */
    public AntConfig getAntConfig() {
        return config;
    }

    /**
     * Process the antlib element
     *
     * @param elementName the name of the element
     * @exception SAXParseException if there is a problem parsing the
     *      element
     */
    public void processElement(String elementName)
         throws SAXParseException {
        config = new AntConfig();
    }

    /**
     * Start a new element in the ant config.
     *
     * @param uri The Namespace URI.
     * @param localName The local name (without prefix).
     * @param qualifiedName The qualified name (with prefix)
     * @param attributes The attributes attached to the element.
     * @throws SAXParseException if there is a parsing problem.
     */
    public void startElement(String uri, String localName, String qualifiedName,
                             Attributes attributes)
         throws SAXParseException {

        try {
            if (qualifiedName.equals("loadlib")) {
                LoadLibHandler loadlibHandler
                     = new LoadLibHandler();
                loadlibHandler.start(getParseContext(), getXMLReader(),
                    this, getLocator(), attributes, getElementSource(),
                    qualifiedName);
                config.addAntLibraryLocation(loadlibHandler.getLibLocation());
            } else if (qualifiedName.equals("libpath")) {
                LibPathHandler libPathHandler
                     = new LibPathHandler();
                libPathHandler.start(getParseContext(), getXMLReader(),
                    this, getLocator(), attributes, getElementSource(),
                    qualifiedName);

                if (libPathHandler.getLibraryPath() != null) {
                    config.addLibPath(libPathHandler.getLibraryId(),
                        libPathHandler.getLibraryPath());
                } else {
                    config.addLibURL(libPathHandler.getLibraryId(),
                        libPathHandler.getLibraryURL());
                }

            } else {
                super.startElement(uri, localName, qualifiedName, attributes);
            }
        } catch (ConfigException e) {
            throw new SAXParseException("Unable to process config",
                getLocator(), e);
        }
    }

}


