/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional.extension;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;

/**
 * Simple class that represents an Extension and conforms to Ants
 * patterns.
 *
 * @ant.datatype name="extension"
 */
public class ExtensionAdapter extends DataType {
    /**
     * The name of the optional package being made available, or required.
     */
    private String extensionName;

    /**
     * The version number (dotted decimal notation) of the specification
     * to which this optional package conforms.
     */
    private DeweyDecimal specificationVersion;

    /**
     * The name of the company or organization that originated the
     * specification to which this optional package conforms.
     */
    private String specificationVendor;

    /**
     * The unique identifier of the company that produced the optional
     * package contained in this JAR file.
     */
    private String implementationVendorID;

    /**
     * The name of the company or organization that produced this
     * implementation of this optional package.
     */
    private String implementationVendor;

    /**
     * The version number (dotted decimal notation) for this implementation
     * of the optional package.
     */
    private DeweyDecimal implementationVersion;

    /**
     * The URL from which the most recent version of this optional package
     * can be obtained if it is not already installed.
     */
    private String implementationURL;

    /**
     * Set the name of extension.
     *
     * @param extensionName the name of extension
     */
    public void setExtensionName(final String extensionName) {
        verifyNotAReference();
        this.extensionName = extensionName;
    }

    /**
     * Set the specificationVersion of extension.
     *
     * @param specificationVersion the specificationVersion of extension
     */
    public void setSpecificationVersion(final String specificationVersion) {
        verifyNotAReference();
        this.specificationVersion = new DeweyDecimal(specificationVersion);
    }

    /**
     * Set the specificationVendor of extension.
     *
     * @param specificationVendor the specificationVendor of extension
     */
    public void setSpecificationVendor(final String specificationVendor) {
        verifyNotAReference();
        this.specificationVendor = specificationVendor;
    }

    /**
     * Set the implementationVendorID of extension.
     *
     * @param implementationVendorID the implementationVendorID of extension
     */
    public void setImplementationVendorId(final String implementationVendorID) {
        verifyNotAReference();
        this.implementationVendorID = implementationVendorID;
    }

    /**
     * Set the implementationVendor of extension.
     *
     * @param implementationVendor the implementationVendor of extension
     */
    public void setImplementationVendor(final String implementationVendor) {
        verifyNotAReference();
        this.implementationVendor = implementationVendor;
    }

    /**
     * Set the implementationVersion of extension.
     *
     * @param implementationVersion the implementationVersion of extension
     */
    public void setImplementationVersion(final String implementationVersion) {
        verifyNotAReference();
        this.implementationVersion = new DeweyDecimal(implementationVersion);
    }

    /**
     * Set the implementationURL of extension.
     *
     * @param implementationURL the implementationURL of extension
     */
    public void setImplementationUrl(final String implementationURL) {
        verifyNotAReference();
        this.implementationURL = implementationURL;
    }

    /**
     * Makes this instance in effect a reference to another ExtensionAdapter
     * instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     *
     * @param reference the reference to which this instance is associated
     * @exception BuildException if this instance already has been configured.
     */
    public void setRefid(final Reference reference)
        throws BuildException {
        if (null != extensionName
            || null != specificationVersion
            || null != specificationVendor
            || null != implementationVersion
            || null != implementationVendorID
            || null != implementationVendor
            || null != implementationURL) {
            throw tooManyAttributes();
        }
        // change this to get the objects from the other reference
        Object o = reference.getReferencedObject(getProject());
        if (o instanceof ExtensionAdapter) {
            final ExtensionAdapter other = (ExtensionAdapter) o;
            extensionName = other.extensionName;
            specificationVersion = other.specificationVersion;
            specificationVendor = other.specificationVendor;
            implementationVersion = other.implementationVersion;
            implementationVendorID = other.implementationVendorID;
            implementationVendor = other.implementationVendor;
            implementationURL = other.implementationURL;
        } else {
            final String message =
                reference.getRefId() + " doesn\'t refer to a Extension";
            throw new BuildException(message);
        }

        super.setRefid(reference);
    }

    private void verifyNotAReference()
        throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
    }

    /**
     * Convert this adpater object into an extension object.
     *
     * @return the extension object
     */
    Extension toExtension()
        throws BuildException {
        if (null == extensionName) {
            final String message = "Extension is missing name.";
            throw new BuildException(message);
        }

        String specificationVersionString = null;
        if (null != specificationVersion) {
            specificationVersionString = specificationVersion.toString();
        }
        String implementationVersionString = null;
        if (null != implementationVersion) {
            implementationVersionString = implementationVersion.toString();
        }
        return new Extension(extensionName,
                              specificationVersionString,
                              specificationVendor,
                              implementationVersionString,
                              implementationVendor,
                              implementationVendorID,
                              implementationURL);
    }

    /**
     * a debug toString method.
     * @return the extension in a string.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "{" + toExtension().toString() + "}";
    }
}
