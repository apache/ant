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
package org.apache.tools.ant.util;

/**
 * XML Parser constants, all kept in one place for ease of reuse
 * @see <a href="https://xml.apache.org/xerces-j/features.html">Xerces features</a>
 * @see <a href="https://xml.apache.org/xerces-j/properties.html">Xerces properties</a>
 * @see <a href=
 * "http://www.saxproject.org/apidoc/org/xml/sax/package-summary.html#package_description"
 * >SAX.</a>
 */

public class XmlConstants {

    private XmlConstants() {
    }

    /** property for location of xml schema */
    public static final String PROPERTY_SCHEMA_LOCATION =
            "http://apache.org/xml/properties/schema/external-schemaLocation";
    /** property for location of no-name schema */
    public static final String PROPERTY_NO_NAMESPACE_SCHEMA_LOCATION =
            "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";
    /** property for full validation */
    public static final String FEATURE_XSD_FULL_VALIDATION =
            "http://apache.org/xml/features/validation/schema-full-checking";
    /** property for xsd */
    public static final String FEATURE_XSD = "http://apache.org/xml/features/validation/schema";

    /** property for validation */
    public static final String FEATURE_VALIDATION = "http://xml.org/sax/features/validation";
    /** property for namespace support */
    public static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";
    /** property for schema language */
    public static final String FEATURE_JAXP12_SCHEMA_LANGUAGE =
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    /** property for schema source */
    public static final String FEATURE_JAXP12_SCHEMA_SOURCE =
            "http://java.sun.com/xml/jaxp/properties/schemaSource";
    /** the namespace for XML schema */
    public static final String URI_XSD =
            "http://www.w3.org/2001/XMLSchema";
    /** the sax external entities feature */
    public static final String FEATURE_EXTERNAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";
    /** the apache.org/xml disallow doctype decl feature */
    public static final String FEATURE_DISALLOW_DTD =
            "http://apache.org/xml/features/disallow-doctype-decl";
}
