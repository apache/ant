/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.util;

/**
 * XML Parser constants, all kept in one place for ease of reuse
 * @see <a href="http://xml.apache.org/xerces-j/features.html">Xerces features</a>
 * @see <a href="http://xml.apache.org/xerces-j/properties.html">Xerces properties</a>
 * @see <a href="http://www.saxproject.org/apidoc/org/xml/sax/package-summary.html#package_description">SAX.</a>
 */

public class XmlConstants {
    public static final String PROPERTY_SCHEMA_LOCATION =
            "http://apache.org/xml/properties/schema/external-schemaLocation";
    public static final String PROPERTY_NO_NAMESPACE_SCHEMA_LOCATION =
            "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";
    public static final String FEATURE_XSD_FULL_VALIDATION =
            "http://apache.org/xml/features/validation/schema-full-checking";
    public static final String FEATURE_XSD = "http://apache.org/xml/features/validation/schema";

    public static final String FEATURE_VALIDATION = "http://xml.org/sax/features/validation";
    public static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";
    public static final String FEATURE_JAXP12_SCHEMA_LANGUAGE =
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String FEATURE_JAXP12_SCHEMA_SOURCE =
            "http://java.sun.com/xml/jaxp/properties/schemaSource";
    public static final String URI_XSD =
            "http://www.w3.org/2001/XMLSchema";
    public static final String FEATURE_EXTERNAL_ENTITIES = 
            "http://xml.org/sax/features/external-general-entities";
    public static final String FEATURE_DISALLOW_DTD =
            "http://apache.org/xml/features/disallow-doctype-decl";
}
