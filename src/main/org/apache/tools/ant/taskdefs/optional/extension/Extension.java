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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.apache.tools.ant.util.DeweyDecimal;

/**
 * <p>Utility class that represents either an available "Optional Package"
 * (formerly known as "Standard Extension") as described in the manifest
 * of a JAR file, or the requirement for such an optional package.</p>
 *
 * <p>For more information about optional packages, see the document
 * <em>Optional Package Versioning</em> in the documentation bundle for your
 * Java2 Standard Edition package, in file
 * <code>guide/extensions/versioning.html</code>.</p>
 *
 */
public final class Extension {
    /**
     * Manifest Attribute Name object for EXTENSION_LIST.
     */
    public static final Attributes.Name EXTENSION_LIST
        = new Attributes.Name("Extension-List");

    /**
     * <code>Name</code> object for <code>Optional-Extension-List</code>
     * manifest attribute used for declaring optional dependencies on
     * installed extensions. Note that the dependencies declared by this method
     * are not required for the library to operate but if present will be used.
     * It is NOT part of the official "Optional Package" specification.
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/extensions/spec.html#dependency">
     *      Installed extension dependency</a>
     */
    public static final Attributes.Name OPTIONAL_EXTENSION_LIST
        = new Attributes.Name("Optional-Extension-List");

    /**
     * Manifest Attribute Name object for EXTENSION_NAME.
     */
    public static final Attributes.Name EXTENSION_NAME =
        new Attributes.Name("Extension-Name");
    /**
     * Manifest Attribute Name object for SPECIFICATION_VERSION.
     */
    public static final Attributes.Name SPECIFICATION_VERSION
        = Attributes.Name.SPECIFICATION_VERSION;

    /**
     * Manifest Attribute Name object for SPECIFICATION_VENDOR.
     */
    public static final Attributes.Name SPECIFICATION_VENDOR
        = Attributes.Name.SPECIFICATION_VENDOR;

    /**
     * Manifest Attribute Name object for IMPLEMENTATION_VERSION.
     */
    public static final Attributes.Name IMPLEMENTATION_VERSION
        = Attributes.Name.IMPLEMENTATION_VERSION;

    /**
     * Manifest Attribute Name object for IMPLEMENTATION_VENDOR.
     */
    public static final Attributes.Name IMPLEMENTATION_VENDOR
        = Attributes.Name.IMPLEMENTATION_VENDOR;

    /**
     * Manifest Attribute Name object for IMPLEMENTATION_URL.
     */
    public static final Attributes.Name IMPLEMENTATION_URL
        = new Attributes.Name("Implementation-URL");

    /**
     * Manifest Attribute Name object for IMPLEMENTATION_VENDOR_ID.
     */
    public static final Attributes.Name IMPLEMENTATION_VENDOR_ID
        = new Attributes.Name("Implementation-Vendor-Id");

    /**
     * Enum indicating that extension is compatible with other extension.
     */
    public static final Compatibility COMPATIBLE
        = new Compatibility("COMPATIBLE");

    /**
     * Enum indicating that extension requires an upgrade
     * of specification to be compatible with other extension.
     */
    public static final Compatibility REQUIRE_SPECIFICATION_UPGRADE
        = new Compatibility("REQUIRE_SPECIFICATION_UPGRADE");

    /**
     * Enum indicating that extension requires a vendor
     * switch to be compatible with other extension.
     */
    public static final Compatibility REQUIRE_VENDOR_SWITCH
        = new Compatibility("REQUIRE_VENDOR_SWITCH");

    /**
     * Enum indicating that extension requires an upgrade
     * of implementation to be compatible with other extension.
     */
    public static final Compatibility REQUIRE_IMPLEMENTATION_UPGRADE
        = new Compatibility("REQUIRE_IMPLEMENTATION_UPGRADE");

    /**
     * Enum indicating that extension is incompatible with
     * other extension in ways other than other enums
     * indicate). For example the other extension may have
     * a different ID.
     */
    public static final Compatibility INCOMPATIBLE
        = new Compatibility("INCOMPATIBLE");

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
     * Return an array of <code>Extension</code> objects representing optional
     * packages that are available in the JAR file associated with the
     * specified <code>Manifest</code>.  If there are no such optional
     * packages, a zero-length array is returned.
     *
     * @param manifest Manifest to be parsed
     * @return the "available" extensions in specified manifest
     */
    public static Extension[] getAvailable(final Manifest manifest) {
        if (null == manifest) {
            return new Extension[0];
        }
        return Stream
            .concat(Optional.ofNullable(manifest.getMainAttributes())
                    .map(Stream::of).orElse(Stream.empty()),
                manifest.getEntries().values().stream())
            .map(attrs -> getExtension("", attrs)).filter(Objects::nonNull)
            .toArray(Extension[]::new);
    }

    /**
     * Return the set of <code>Extension</code> objects representing optional
     * packages that are required by the application contained in the JAR
     * file associated with the specified <code>Manifest</code>.  If there
     * are no such optional packages, a zero-length list is returned.
     *
     * @param manifest Manifest to be parsed
     * @return the dependencies that are specified in manifest
     */
    public static Extension[] getRequired(final Manifest manifest) {
        return getListed(manifest, Attributes.Name.EXTENSION_LIST);
    }

    /**
     * Return the set of <code>Extension</code> objects representing "Optional
     * Packages" that the application declares they will use if present. If
     * there are no such optional packages, a zero-length list is returned.
     *
     * @param manifest Manifest to be parsed
     * @return the optional dependencies that are specified in manifest
     */
    public static Extension[] getOptions(final Manifest manifest) {
        return getListed(manifest, OPTIONAL_EXTENSION_LIST);
    }

    /**
     * Add Extension to the specified manifest Attributes.
     *
     * @param attributes the attributes of manifest to add to
     * @param extension the extension
     */
    public static void addExtension(final Extension extension,
                                     final Attributes attributes) {
        addExtension(extension, "", attributes);
    }

    /**
     * Add Extension to the specified manifest Attributes.
     * Use the specified prefix so that dependencies can added
     * with a prefix such as "java3d-" etc.
     *
     * @param attributes the attributes of manifest to add to
     * @param extension the extension
     * @param prefix the name to prefix to extension
     */
    public static void addExtension(final Extension extension,
                                     final String prefix,
                                     final Attributes attributes) {
        attributes.putValue(prefix + EXTENSION_NAME,
                             extension.getExtensionName());

        final String specificationVendor = extension.getSpecificationVendor();
        if (null != specificationVendor) {
            attributes.putValue(prefix + SPECIFICATION_VENDOR,
                                 specificationVendor);
        }

        final DeweyDecimal specificationVersion
            = extension.getSpecificationVersion();
        if (null != specificationVersion) {
            attributes.putValue(prefix + SPECIFICATION_VERSION,
                                 specificationVersion.toString());
        }

        final String implementationVendorID
            = extension.getImplementationVendorID();
        if (null != implementationVendorID) {
            attributes.putValue(prefix + IMPLEMENTATION_VENDOR_ID,
                                 implementationVendorID);
        }

        final String implementationVendor = extension.getImplementationVendor();
        if (null != implementationVendor) {
            attributes.putValue(prefix + IMPLEMENTATION_VENDOR,
                                 implementationVendor);
        }

        final DeweyDecimal implementationVersion
            = extension.getImplementationVersion();
        if (null != implementationVersion) {
            attributes.putValue(prefix + IMPLEMENTATION_VERSION,
                                 implementationVersion.toString());
        }

        final String implementationURL = extension.getImplementationURL();
        if (null != implementationURL) {
            attributes.putValue(prefix + IMPLEMENTATION_URL,
                                 implementationURL);
        }
    }

    /**
     * The constructor to create Extension object.
     * Note that every component is allowed to be specified
     * but only the extensionName is mandatory.
     *
     * @param extensionName the name of extension.
     * @param specificationVersion the specification Version of extension.
     * @param specificationVendor the specification Vendor of extension.
     * @param implementationVersion the implementation Version of extension.
     * @param implementationVendor the implementation Vendor of extension.
     * @param implementationVendorId the implementation VendorId of extension.
     * @param implementationURL the implementation URL of extension.
     */
    public Extension(final String extensionName,
                      final String specificationVersion,
                      final String specificationVendor,
                      final String implementationVersion,
                      final String implementationVendor,
                      final String implementationVendorId,
                      final String implementationURL) {
        this.extensionName = extensionName;
        this.specificationVendor = specificationVendor;

        if (null != specificationVersion) {
            try {
                this.specificationVersion
                    = new DeweyDecimal(specificationVersion);
            } catch (final NumberFormatException nfe) {
                final String error = "Bad specification version format '"
                    + specificationVersion + "' in '" + extensionName
                    + "'. (Reason: " + nfe + ")";
                throw new IllegalArgumentException(error);
            }
        }

        this.implementationURL = implementationURL;
        this.implementationVendor = implementationVendor;
        this.implementationVendorID = implementationVendorId;

        if (null != implementationVersion) {
            try {
                this.implementationVersion
                    = new DeweyDecimal(implementationVersion);
            } catch (final NumberFormatException nfe) {
                final String error = "Bad implementation version format '"
                    + implementationVersion + "' in '" + extensionName
                    + "'. (Reason: " + nfe + ")";
                throw new IllegalArgumentException(error);
            }
        }

        if (null == this.extensionName) {
            throw new NullPointerException("extensionName property is null");
        }
    }

    /**
     * Get the name of the extension.
     *
     * @return the name of the extension
     */
    public String getExtensionName() {
        return extensionName;
    }

    /**
     * Get the vendor of the extensions specification.
     *
     * @return the vendor of the extensions specification.
     */
    public String getSpecificationVendor() {
        return specificationVendor;
    }

    /**
     * Get the version of the extensions specification.
     *
     * @return the version of the extensions specification.
     */
    public DeweyDecimal getSpecificationVersion() {
        return specificationVersion;
    }

    /**
     * Get the url of the extensions implementation.
     *
     * @return the url of the extensions implementation.
     */
    public String getImplementationURL() {
        return implementationURL;
    }

    /**
     * Get the vendor of the extensions implementation.
     *
     * @return the vendor of the extensions implementation.
     */
    public String getImplementationVendor() {
        return implementationVendor;
    }

    /**
     * Get the vendorID of the extensions implementation.
     *
     * @return the vendorID of the extensions implementation.
     */
    public String getImplementationVendorID() {
        return implementationVendorID;
    }

    /**
     * Get the version of the extensions implementation.
     *
     * @return the version of the extensions implementation.
     */
    public DeweyDecimal getImplementationVersion() {
        return implementationVersion;
    }

    /**
     * Return a Compatibility enum indicating the relationship of this
     * <code>Extension</code> with the specified <code>Extension</code>.
     *
     * @param required Description of the required optional package
     * @return the enum indicating the compatibility (or lack thereof)
     *         of specified extension
     */
    public Compatibility getCompatibilityWith(final Extension required) {
        // Extension Name must match
        if (!extensionName.equals(required.getExtensionName())) {
            return INCOMPATIBLE;
        }

        // Available specification version must be >= required
        final DeweyDecimal requiredSpecificationVersion
            = required.getSpecificationVersion();
        if (null != requiredSpecificationVersion) {
            if (null == specificationVersion
                || !isCompatible(specificationVersion, requiredSpecificationVersion)) {
                return REQUIRE_SPECIFICATION_UPGRADE;
            }
        }

        // Implementation Vendor ID must match
        final String requiredImplementationVendorID
            = required.getImplementationVendorID();
        if (null != requiredImplementationVendorID) {
            if (null == implementationVendorID
                || !implementationVendorID.equals(requiredImplementationVendorID)) {
                return REQUIRE_VENDOR_SWITCH;
            }
        }

        // Implementation version must be >= required
        final DeweyDecimal requiredImplementationVersion
            = required.getImplementationVersion();
        if (null != requiredImplementationVersion) {
            if (null == implementationVersion
                || !isCompatible(implementationVersion, requiredImplementationVersion)) {
                return REQUIRE_IMPLEMENTATION_UPGRADE;
            }
        }

        // This available optional package satisfies the requirements
        return COMPATIBLE;
    }

    /**
     * Return <code>true</code> if the specified <code>Extension</code>
     * (which represents an optional package required by an application)
     * is satisfied by this <code>Extension</code> (which represents an
     * optional package that is already installed.  Otherwise, return
     * <code>false</code>.
     *
     * @param required Description of the required optional package
     * @return true if the specified extension is compatible with this extension
     */
    public boolean isCompatibleWith(final Extension required) {
        return (COMPATIBLE == getCompatibilityWith(required));
    }

    /**
     * Return a String representation of this object.
     *
     * @return string representation of object.
     */
    @Override
    public String toString() {
        final String format = "%s: %s%n";

        final StringBuilder sb = new StringBuilder(String.format(format,
                EXTENSION_NAME, extensionName));

        if (null != specificationVersion) {
            sb.append(String.format(format, SPECIFICATION_VERSION, specificationVersion));
        }

        if (null != specificationVendor) {
            sb.append(String.format(format, SPECIFICATION_VENDOR, specificationVendor));
        }

        if (null != implementationVersion) {
            sb.append(String.format(format, IMPLEMENTATION_VERSION, implementationVersion));
        }

        if (null != implementationVendorID) {
            sb.append(String.format(format, IMPLEMENTATION_VENDOR_ID, implementationVendorID));
        }

        if (null != implementationVendor) {
            sb.append(String.format(format, IMPLEMENTATION_VENDOR, implementationVendor));
        }

        if (null != implementationURL) {
            sb.append(String.format(format, IMPLEMENTATION_URL, implementationURL));
        }

        return sb.toString();
    }

    /**
     * Return <code>true</code> if the first version number is greater than
     * or equal to the second; otherwise return <code>false</code>.
     *
     * @param first First version number (dotted decimal)
     * @param second Second version number (dotted decimal)
     */
    private boolean isCompatible(final DeweyDecimal first,
                                 final DeweyDecimal second) {
        return first.isGreaterThanOrEqual(second);
    }

    /**
     * Retrieve all the extensions listed under a particular key
     * (Usually EXTENSION_LIST or OPTIONAL_EXTENSION_LIST).
     *
     * @param manifest the manifest to extract extensions from
     * @param listKey the key used to get list (Usually
     *        EXTENSION_LIST or OPTIONAL_EXTENSION_LIST)
     * @return the list of listed extensions
     */
    private static Extension[] getListed(final Manifest manifest,
                                          final Attributes.Name listKey) {
        final List<Extension> results = new ArrayList<>();
        final Attributes mainAttributes = manifest.getMainAttributes();

        if (null != mainAttributes) {
            getExtension(mainAttributes, results, listKey);
        }

        manifest.getEntries().values()
            .forEach(attributes -> getExtension(attributes, results, listKey));

        return results.toArray(new Extension[0]);
    }

    /**
     * Add required optional packages defined in the specified
     * attributes entry, if any.
     *
     * @param attributes Attributes to be parsed
     * @param required list to add required optional packages to
     * @param listKey the key to use to lookup list, usually EXTENSION_LIST
     *    or OPTIONAL_EXTENSION_LIST
     */
    private static void getExtension(final Attributes attributes,
                                     final List<Extension> required,
                                     final Attributes.Name listKey) {
        final String names = attributes.getValue(listKey);
        if (null == names) {
            return;
        }
        for (final String prefix : split(names, " ")) {
            final Extension extension = getExtension(prefix + "-", attributes);
            if (null != extension) {
                required.add(extension);
            }
        }
    }

    /**
     * Splits the string on every token into an array of strings.
     *
     * @param string the string
     * @param onToken the token
     * @return the resultant array
     */
    private static String[] split(final String string,
                                        final String onToken) {
        final StringTokenizer tokenizer = new StringTokenizer(string, onToken);
        final String[] result = new String[tokenizer.countTokens()];

        for (int i = 0; i < result.length; i++) {
            result[i] = tokenizer.nextToken();
        }

        return result;
    }

    /**
     * Extract an Extension from Attributes.
     * Prefix indicates the prefix checked for each string.
     * Usually the prefix is <em>"&lt;extension&gt;-"</em> if looking for a
     * <b>Required</b> extension. If you are looking for an
     * <b>Available</b> extension
     * then the prefix is <em>""</em>.
     *
     * @param prefix the prefix for each attribute name
     * @param attributes Attributes to searched
     * @return the new Extension object, or null
     */
    private static Extension getExtension(final String prefix,
                                          final Attributes attributes) {
        //WARNING: We trim the values of all the attributes because
        //Some extension declarations are badly defined (ie have spaces
        //after version or vendorID)
        final String nameKey = prefix + EXTENSION_NAME;
        final String name = getTrimmedString(attributes.getValue(nameKey));
        if (null == name) {
            return null;
        }

        final String specVendorKey = prefix + SPECIFICATION_VENDOR;
        final String specVendor
            = getTrimmedString(attributes.getValue(specVendorKey));
        final String specVersionKey = prefix + SPECIFICATION_VERSION;
        final String specVersion
            = getTrimmedString(attributes.getValue(specVersionKey));

        final String impVersionKey = prefix + IMPLEMENTATION_VERSION;
        final String impVersion
            = getTrimmedString(attributes.getValue(impVersionKey));
        final String impVendorKey = prefix + IMPLEMENTATION_VENDOR;
        final String impVendor
            = getTrimmedString(attributes.getValue(impVendorKey));
        final String impVendorIDKey = prefix + IMPLEMENTATION_VENDOR_ID;
        final String impVendorId
            = getTrimmedString(attributes.getValue(impVendorIDKey));
        final String impURLKey = prefix + IMPLEMENTATION_URL;
        final String impURL = getTrimmedString(attributes.getValue(impURLKey));

        return new Extension(name, specVersion, specVendor, impVersion,
                              impVendor, impVendorId, impURL);
    }

    /**
     * Trim the supplied string if the string is non-null
     *
     * @param value the string to trim or null
     * @return the trimmed string or null
     */
    private static String getTrimmedString(final String value) {
        return null == value ? null : value.trim();
    }
}
