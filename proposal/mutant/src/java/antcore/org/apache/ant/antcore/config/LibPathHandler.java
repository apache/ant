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

import org.apache.ant.antcore.xml.ElementHandler;
import org.xml.sax.SAXParseException;

/**
 * An XML Handler for the libpath element in an Ant config file
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 20 January 2002
 */
public class LibPathHandler extends ElementHandler {
    /** The library identifier attribute name */
    public static final String LIBID_ATTR = "libid";
    /** The path attribute name */
    public static final String PATH_ATTR = "path";
    /** The path attribute name */
    public static final String URL_ATTR = "url";

    /**
     * Get the libraryId for which the additional path is being defined
     *
     * @return the library's unique id
     */
    public String getLibraryId() {
        return getAttribute(LIBID_ATTR);
    }

    /**
     * Get the additional path being defined fro the library
     *
     * @return the libraryPath value, may be null
     */
    public String getLibraryPath() {
        return getAttribute(PATH_ATTR);
    }

    /**
     * Get the URL (as a string) containing the additional path for the
     * library.
     *
     * @return the libraryURL value
     */
    public String getLibraryURL() {
        return getAttribute(URL_ATTR);
    }


    /**
     * Process the libpath element
     *
     * @param elementName the name of the element
     * @exception SAXParseException if there is a problem parsing the
     *      element
     */
    public void processElement(String elementName)
         throws SAXParseException {
        if (getLibraryId() == null
             || (getLibraryPath() == null && getLibraryURL() == null)
             || (getLibraryPath() != null && getLibraryURL() != null)) {
            throw new SAXParseException("The " + LIBID_ATTR
                 + " attribute and only one of "
                 + PATH_ATTR + " or " + URL_ATTR
                 + " attributes must be specified for a libpath element",
                getLocator());
        }
    }

    /**
     * Validate that the given attribute and value are valid.
     *
     * @param attributeName The name of the attributes
     * @param attributeValue The value of the attributes
     * @exception SAXParseException if the attribute is not allowed on the
     *      element.
     */
    protected void validateAttribute(String attributeName,
                                     String attributeValue)
         throws SAXParseException {
        if (!attributeName.equals(LIBID_ATTR) &&
            !attributeName.equals(PATH_ATTR) &&
            !attributeName.equals(URL_ATTR)) {
            throwInvalidAttribute(attributeName);
        }
    }
}


