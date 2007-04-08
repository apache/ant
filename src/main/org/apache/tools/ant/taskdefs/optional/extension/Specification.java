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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.tools.ant.util.StringUtils;

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
public final class Specification {

    private static final String MISSING = "Missing ";

    /**
     * Manifest Attribute Name object for SPECIFICATION_TITLE.
     */
    public static final Attributes.Name SPECIFICATION_TITLE
        = Attributes.Name.SPECIFICATION_TITLE;

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
     * Manifest Attribute Name object for IMPLEMENTATION_TITLE.
     */
    public static final Attributes.Name IMPLEMENTATION_TITLE
        = Attributes.Name.IMPLEMENTATION_TITLE;

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
     * Enum indicating that extension is compatible with other Package
     * Specification.
     */
    public static final Compatibility COMPATIBLE =
        new Compatibility("COMPATIBLE");

    /**
     * Enum indicating that extension requires an upgrade
     * of specification to be compatible with other Package Specification.
     */
    public static final Compatibility REQUIRE_SPECIFICATION_UPGRADE =
        new Compatibility("REQUIRE_SPECIFICATION_UPGRADE");

    /**
     * Enum indicating that extension requires a vendor
     * switch to be compatible with other Package Specification.
     */
    public static final Compatibility REQUIRE_VENDOR_SWITCH =
        new Compatibility("REQUIRE_VENDOR_SWITCH");

    /**
     * Enum indicating that extension requires an upgrade
     * of implementation to be compatible with other Package Specification.
     */
    public static final Compatibility REQUIRE_IMPLEMENTATION_CHANGE =
        new Compatibility("REQUIRE_IMPLEMENTATION_CHANGE");

    /**
     * This enum indicates that an extension is incompatible with
     * other Package Specification in ways other than other enums
     * indicate. For example, the other Package Specification
     * may have a different ID.
     */
    public static final Compatibility INCOMPATIBLE =
        new Compatibility("INCOMPATIBLE");

    /**
     * The name of the Package Specification.
     */
    private String specificationTitle;

    /**
     * The version number (dotted decimal notation) of the specification
     * to which this optional package conforms.
     */
    private DeweyDecimal specificationVersion;

    /**
     * The name of the company or organization that originated the
     * specification to which this specification conforms.
     */
    private String specificationVendor;

    /**
     * The title of implementation.
     */
    private String implementationTitle;

    /**
     * The name of the company or organization that produced this
     * implementation of this specification.
     */
    private String implementationVendor;

    /**
     * The version string for implementation. The version string is
     * opaque.
     */
    private String implementationVersion;

    /**
     * The sections of jar that the specification applies to.
     */
    private String[] sections;

    /**
     * Return an array of <code>Package Specification</code> objects.
     * If there are no such optional packages, a zero-length array is returned.
     *
     * @param manifest Manifest to be parsed
     * @return the Package Specifications extensions in specified manifest
     * @throws ParseException if the attributes of the specifications cannot
     * be parsed according to their expected formats.
     */
    public static Specification[] getSpecifications(final Manifest manifest)
        throws ParseException {
        if (null == manifest) {
            return new Specification[ 0 ];
        }

        final ArrayList results = new ArrayList();

        final Map entries = manifest.getEntries();
        final Iterator keys = entries.keySet().iterator();
        while (keys.hasNext()) {
            final String key = (String) keys.next();
            final Attributes attributes = (Attributes) entries.get(key);
            final Specification specification
                = getSpecification(key, attributes);
            if (null != specification) {
                results.add(specification);
            }
        }

        final ArrayList trimmedResults = removeDuplicates(results);
        return (Specification[]) trimmedResults.toArray(new Specification[trimmedResults.size()]);
    }

    /**
     * The constructor to create Package Specification object.
     * Note that every component is allowed to be specified
     * but only the specificationTitle is mandatory.
     *
     * @param specificationTitle the name of specification.
     * @param specificationVersion the specification Version.
     * @param specificationVendor the specification Vendor.
     * @param implementationTitle the title of implementation.
     * @param implementationVersion the implementation Version.
     * @param implementationVendor the implementation Vendor.
     */
    public Specification(final String specificationTitle,
                          final String specificationVersion,
                          final String specificationVendor,
                          final String implementationTitle,
                          final String implementationVersion,
                          final String implementationVendor) {
        this(specificationTitle, specificationVersion, specificationVendor,
              implementationTitle, implementationVersion, implementationVendor,
              null);
    }

    /**
     * The constructor to create Package Specification object.
     * Note that every component is allowed to be specified
     * but only the specificationTitle is mandatory.
     *
     * @param specificationTitle the name of specification.
     * @param specificationVersion the specification Version.
     * @param specificationVendor the specification Vendor.
     * @param implementationTitle the title of implementation.
     * @param implementationVersion the implementation Version.
     * @param implementationVendor the implementation Vendor.
     * @param sections the sections/packages that Specification applies to.
     */
    public Specification(final String specificationTitle,
                          final String specificationVersion,
                          final String specificationVendor,
                          final String implementationTitle,
                          final String implementationVersion,
                          final String implementationVendor,
                          final String[] sections) {
        this.specificationTitle = specificationTitle;
        this.specificationVendor = specificationVendor;

        if (null != specificationVersion) {
            try {
                this.specificationVersion
                    = new DeweyDecimal(specificationVersion);
            } catch (final NumberFormatException nfe) {
                final String error = "Bad specification version format '"
                    + specificationVersion + "' in '" + specificationTitle
                    + "'. (Reason: " + nfe + ")";
                throw new IllegalArgumentException(error);
            }
        }

        this.implementationTitle = implementationTitle;
        this.implementationVendor = implementationVendor;
        this.implementationVersion = implementationVersion;

        if (null == this.specificationTitle) {
            throw new NullPointerException("specificationTitle");
        }

        String[] copy = null;
        if (null != sections) {
            copy = new String[ sections.length ];
            System.arraycopy(sections, 0, copy, 0, sections.length);
        }
        this.sections = copy;
    }

    /**
     * Get the title of the specification.
     *
     * @return the title of speciication
     */
    public String getSpecificationTitle() {
        return specificationTitle;
    }

    /**
     * Get the vendor of the specification.
     *
     * @return the vendor of the specification.
     */
    public String getSpecificationVendor() {
        return specificationVendor;
    }

    /**
     * Get the title of the specification.
     *
     * @return the title of the specification.
     */
    public String getImplementationTitle() {
        return implementationTitle;
    }

    /**
     * Get the version of the specification.
     *
     * @return the version of the specification.
     */
    public DeweyDecimal getSpecificationVersion() {
        return specificationVersion;
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
     * Get the version of the implementation.
     *
     * @return the version of the implementation.
     */
    public String getImplementationVersion() {
        return implementationVersion;
    }

    /**
     * Return an array containing sections to which specification applies
     * or null if relevent to no sections.
     *
     * @return an array containing sections to which specification applies
     *         or null if relevent to no sections.
     */
    public String[] getSections() {
        if (null == sections) {
            return null;
        }
        final String[] newSections = new String[ sections.length ];
        System.arraycopy(sections, 0, newSections, 0, sections.length);
        return newSections;
    }

    /**
     * Return a Compatibility enum indicating the relationship of this
     * <code>Package Specification</code> with the specified
     * <code>Extension</code>.
     *
     * @param other the other specification
     * @return the enum indicating the compatibility (or lack thereof)
     *         of specifed Package Specification
     */
    public Compatibility getCompatibilityWith(final Specification other) {
        // Specification Name must match
        if (!specificationTitle.equals(other.getSpecificationTitle())) {
            return INCOMPATIBLE;
        }

        // Available specification version must be >= required
        final DeweyDecimal otherSpecificationVersion
            = other.getSpecificationVersion();
        if (null != specificationVersion) {
            if (null == otherSpecificationVersion
                || !isCompatible(specificationVersion, otherSpecificationVersion)) {
                return REQUIRE_SPECIFICATION_UPGRADE;
            }
        }

        // Implementation Vendor ID must match
        final String otherImplementationVendor
            = other.getImplementationVendor();
        if (null != implementationVendor) {
            if (null == otherImplementationVendor
                || !implementationVendor.equals(otherImplementationVendor)) {
                return REQUIRE_VENDOR_SWITCH;
            }
        }

        // Implementation version must be >= required
        final String otherImplementationVersion
            = other.getImplementationVersion();
        if (null != implementationVersion) {
            if (null == otherImplementationVersion
                || !implementationVersion.equals(otherImplementationVersion)) {
                return REQUIRE_IMPLEMENTATION_CHANGE;
            }
        }

        // This available optional package satisfies the requirements
        return COMPATIBLE;
    }

    /**
     * Return <code>true</code> if the specified <code>package</code>
     * is satisfied by this <code>Specification</code>. Otherwise, return
     * <code>false</code>.
     *
     * @param other the specification
     * @return true if the specification is compatible with this specification
     */
    public boolean isCompatibleWith(final Specification other) {
        return (COMPATIBLE == getCompatibilityWith(other));
    }

    /**
     * Return a String representation of this object.
     *
     * @return string representation of object.
     */
    public String toString() {
        final String brace = ": ";

        final StringBuffer sb
            = new StringBuffer(SPECIFICATION_TITLE.toString());
        sb.append(brace);
        sb.append(specificationTitle);
        sb.append(StringUtils.LINE_SEP);

        if (null != specificationVersion) {
            sb.append(SPECIFICATION_VERSION);
            sb.append(brace);
            sb.append(specificationVersion);
            sb.append(StringUtils.LINE_SEP);
        }

        if (null != specificationVendor) {
            sb.append(SPECIFICATION_VENDOR);
            sb.append(brace);
            sb.append(specificationVendor);
            sb.append(StringUtils.LINE_SEP);
        }

        if (null != implementationTitle) {
            sb.append(IMPLEMENTATION_TITLE);
            sb.append(brace);
            sb.append(implementationTitle);
            sb.append(StringUtils.LINE_SEP);
        }

        if (null != implementationVersion) {
            sb.append(IMPLEMENTATION_VERSION);
            sb.append(brace);
            sb.append(implementationVersion);
            sb.append(StringUtils.LINE_SEP);
        }

        if (null != implementationVendor) {
            sb.append(IMPLEMENTATION_VENDOR);
            sb.append(brace);
            sb.append(implementationVendor);
            sb.append(StringUtils.LINE_SEP);
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
     * Combine all specifications objects that are identical except
     * for the sections.
     *
     * <p>Note this is very inefficent and should probably be fixed
     * in the future.</p>
     *
     * @param list the array of results to trim
     * @return an array list with all duplicates removed
     */
    private static ArrayList removeDuplicates(final ArrayList list) {
        final ArrayList results = new ArrayList();
        final ArrayList sections = new ArrayList();
        while (list.size() > 0) {
            final Specification specification = (Specification) list.remove(0);
            final Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                final Specification other = (Specification) iterator.next();
                if (isEqual(specification, other)) {
                    final String[] otherSections = other.getSections();
                    if (null != otherSections) {
                        sections.addAll(Arrays.asList(otherSections));
                    }
                    iterator.remove();
                }
            }

            final Specification merged =
                mergeInSections(specification, sections);
            results.add(merged);
            //Reset list of sections
            sections.clear();
        }

        return results;
    }

    /**
     * Test if two specifications are equal except for their sections.
     *
     * @param specification one specificaiton
     * @param other the ohter specification
     * @return true if two specifications are equal except for their
     *         sections, else false
     */
    private static boolean isEqual(final Specification specification,
                                    final Specification other) {
        return
            specification.getSpecificationTitle().equals(other.getSpecificationTitle())
            && specification.getSpecificationVersion().isEqual(other.getSpecificationVersion())
            && specification.getSpecificationVendor().equals(other.getSpecificationVendor())
            && specification.getImplementationTitle().equals(other.getImplementationTitle())
            && specification.getImplementationVersion().equals(other.getImplementationVersion())
            && specification.getImplementationVendor().equals(other.getImplementationVendor());
    }

    /**
     * Merge the specified sections into specified section and return result.
     * If no sections to be added then just return original specification.
     *
     * @param specification the specification
     * @param sectionsToAdd the list of sections to merge
     * @return the merged specification
     */
    private static Specification mergeInSections(final Specification specification,
                                              final ArrayList sectionsToAdd) {
        if (0 == sectionsToAdd.size()) {
            return specification;
        }
        sectionsToAdd.addAll(Arrays.asList(specification.getSections()));

        final String[] sections =
            (String[]) sectionsToAdd.toArray(new String[sectionsToAdd.size()]);

        return new Specification(specification.getSpecificationTitle(),
                specification.getSpecificationVersion().toString(),
                specification.getSpecificationVendor(),
                specification.getImplementationTitle(),
                specification.getImplementationVersion(),
                specification.getImplementationVendor(),
                sections);
    }

    /**
     * Trim the supplied string if the string is non-null
     *
     * @param value the string to trim or null
     * @return the trimmed string or null
     */
    private static String getTrimmedString(final String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Extract an Package Specification from Attributes.
     *
     * @param attributes Attributes to searched
     * @return the new Specification object, or null
     */
    private static Specification getSpecification(final String section,
                                                   final Attributes attributes)
        throws ParseException {
        //WARNING: We trim the values of all the attributes because
        //Some extension declarations are badly defined (ie have spaces
        //after version or vendor)
        final String name
            = getTrimmedString(attributes.getValue(SPECIFICATION_TITLE));
        if (null == name) {
            return null;
        }

        final String specVendor
            = getTrimmedString(attributes.getValue(SPECIFICATION_VENDOR));
        if (null == specVendor) {
            throw new ParseException(MISSING + SPECIFICATION_VENDOR, 0);
        }

        final String specVersion
            = getTrimmedString(attributes.getValue(SPECIFICATION_VERSION));
        if (null == specVersion) {
            throw new ParseException(MISSING + SPECIFICATION_VERSION, 0);
        }

        final String impTitle
            = getTrimmedString(attributes.getValue(IMPLEMENTATION_TITLE));
        if (null == impTitle) {
            throw new ParseException(MISSING + IMPLEMENTATION_TITLE, 0);
        }

        final String impVersion
            = getTrimmedString(attributes.getValue(IMPLEMENTATION_VERSION));
        if (null == impVersion) {
            throw new ParseException(MISSING + IMPLEMENTATION_VERSION, 0);
        }

        final String impVendor
            = getTrimmedString(attributes.getValue(IMPLEMENTATION_VENDOR));
        if (null == impVendor) {
            throw new ParseException(MISSING + IMPLEMENTATION_VENDOR, 0);
        }

        return new Specification(name, specVersion, specVendor,
                                  impTitle, impVersion, impVendor,
                                  new String[]{section});
    }
}
