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
package org.apache.tools.ant.types;

import java.net.URL;

/**
 * <p>Helper class to handle the <code>&lt;dtd&gt;</code> and
 * <code>&lt;entity&gt;</code> nested elements.  These correspond to
 * the <code>PUBLIC</code> and <code>URI</code> catalog entry types,
 * respectively, as defined in the <a
 * href="https://oasis-open.org/committees/entity/spec-2001-08-06.html">
 * OASIS "Open Catalog" standard</a>.</p>
 *
 * <p>Possible Future Enhancements:</p>
 * <ul>
 * <li>Bring the Ant element names into conformance with the OASIS standard</li>
 * <li>Add support for additional OASIS catalog entry types</li>
 * </ul>
 *
 * @see org.apache.xml.resolver.Catalog
 * @since Ant 1.6
 */
public class ResourceLocation {

    //-- Fields ----------------------------------------------------------------
    /** publicId of the dtd/entity. */
    private String publicId = null;

    /** location of the dtd/entity - a file/resource/URL. */
    private String location = null;

    /**
     * base URL of the dtd/entity, or null. If null, the Ant project
     * basedir is assumed.  If the location specifies a relative
     * URL/pathname, it is resolved using the base.  The default base
     * for an external catalog file is the directory in which it is
     * located.
     */
    private URL base = null;

    //-- Methods ---------------------------------------------------------------

    /**
     * @param publicId uniquely identifies the resource.
     */
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    /**
     * @param location the location of the resource associated with the
     *      publicId.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @param base the base URL of the resource associated with the
     * publicId.  If the location specifies a relative URL/pathname,
     * it is resolved using the base.  The default base for an
     * external catalog file is the directory in which it is located.
     */
    public void setBase(URL base) {
        this.base = base;
    }

    /**
     * @return the publicId of the resource.
     */
    public String getPublicId() {
        return publicId;
    }

    /**
     * @return the location of the resource identified by the publicId.
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return the base of the resource identified by the publicId.
     */
    public URL getBase() {
        return base;
    }

}
