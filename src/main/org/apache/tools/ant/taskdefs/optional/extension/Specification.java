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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
 * WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
 *  This file is from excalibur.extension package. Dont edit this file
 * directly as there is no unit tests to make sure it is operational
 * in ant. Edit file in excalibur and run tests there before changing
 * ants file.
 * WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class Specification
{
    /**
     * Manifest Attribute Name object for SPECIFICATION_TITLE.
     * @see Attributes.Name#SPECIFICATION_TITLE
     */
    public static final Attributes.Name SPECIFICATION_TITLE = Attributes.Name.SPECIFICATION_TITLE;

    /**
     * Manifest Attribute Name object for SPECIFICATION_VERSION.
     * @see Attributes.Name#SPECIFICATION_VERSION
     */
    public static final Attributes.Name SPECIFICATION_VERSION = Attributes.Name.SPECIFICATION_VERSION;

    /**
     * Manifest Attribute Name object for SPECIFICATION_VENDOR.
     * @see Attributes.Name#SPECIFICATION_VENDOR
     */
    public static final Attributes.Name SPECIFICATION_VENDOR = Attributes.Name.SPECIFICATION_VENDOR;

    /**
     * Manifest Attribute Name object for IMPLEMENTATION_TITLE.
     * @see Attributes.Name#IMPLEMENTATION_TITLE
     */
    public static final Attributes.Name IMPLEMENTATION_TITLE = Attributes.Name.IMPLEMENTATION_TITLE;

    /**
     * Manifest Attribute Name object for IMPLEMENTATION_VERSION.
     * @see Attributes.Name#IMPLEMENTATION_VERSION
     */
    public static final Attributes.Name IMPLEMENTATION_VERSION = Attributes.Name.IMPLEMENTATION_VERSION;

    /**
     * Manifest Attribute Name object for IMPLEMENTATION_VENDOR.
     * @see Attributes.Name#IMPLEMENTATION_VENDOR
     */
    public static final Attributes.Name IMPLEMENTATION_VENDOR = Attributes.Name.IMPLEMENTATION_VENDOR;

    /**
     * Enum indicating that extension is compatible with other Package
     * Specification.
     */
    public static final Compatibility COMPATIBLE =
        new Compatibility( "COMPATIBLE" );

    /**
     * Enum indicating that extension requires an upgrade
     * of specification to be compatible with other Package Specification.
     */
    public static final Compatibility REQUIRE_SPECIFICATION_UPGRADE =
        new Compatibility( "REQUIRE_SPECIFICATION_UPGRADE" );

    /**
     * Enum indicating that extension requires a vendor
     * switch to be compatible with other Package Specification.
     */
    public static final Compatibility REQUIRE_VENDOR_SWITCH =
        new Compatibility( "REQUIRE_VENDOR_SWITCH" );

    /**
     * Enum indicating that extension requires an upgrade
     * of implementation to be compatible with other Package Specification.
     */
    public static final Compatibility REQUIRE_IMPLEMENTATION_CHANGE =
        new Compatibility( "REQUIRE_IMPLEMENTATION_CHANGE" );

    /**
     * Enum indicating that extension is incompatible with
     * other Package Specification in ways other than other enums
     * indicate). ie For example the other Package Specification
     * may have a different ID.
     */
    public static final Compatibility INCOMPATIBLE =
        new Compatibility( "INCOMPATIBLE" );

    /**
     * The name of the Package Specification.
     */
    private String m_specificationTitle;

    /**
     * The version number (dotted decimal notation) of the specification
     * to which this optional package conforms.
     */
    private DeweyDecimal m_specificationVersion;

    /**
     * The name of the company or organization that originated the
     * specification to which this specification conforms.
     */
    private String m_specificationVendor;

    /**
     * The title of implementation.
     */
    private String m_implementationTitle;

    /**
     * The name of the company or organization that produced this
     * implementation of this specification.
     */
    private String m_implementationVendor;

    /**
     * The version string for implementation. The version string is
     * opaque.
     */
    private String m_implementationVersion;

    /**
     * The sections of jar that the specification applies to.
     */
    private String[] m_sections;

    /**
     * Return an array of <code>Package Specification</code> objects.
     * If there are no such optional packages, a zero-length array is returned.
     *
     * @param manifest Manifest to be parsed
     * @return the Package Specifications extensions in specified manifest
     */
    public static Specification[] getSpecifications( final Manifest manifest )
        throws ParseException
    {
        if( null == manifest )
        {
            return new Specification[ 0 ];
        }

        final ArrayList results = new ArrayList();

        final Map entries = manifest.getEntries();
        final Iterator keys = entries.keySet().iterator();
        while( keys.hasNext() )
        {
            final String key = (String)keys.next();
            final Attributes attributes = (Attributes)entries.get( key );
            final Specification specification = getSpecification( key, attributes );
            if( null != specification )
            {
                results.add( specification );
            }
        }

        final ArrayList trimmedResults = removeDuplicates( results );
        return (Specification[])trimmedResults.toArray( new Specification[ 0 ] );
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
    public Specification( final String specificationTitle,
                          final String specificationVersion,
                          final String specificationVendor,
                          final String implementationTitle,
                          final String implementationVersion,
                          final String implementationVendor )
    {
        this( specificationTitle, specificationVersion, specificationVendor,
              implementationTitle, implementationVersion, implementationVendor,
              null );
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
    public Specification( final String specificationTitle,
                          final String specificationVersion,
                          final String specificationVendor,
                          final String implementationTitle,
                          final String implementationVersion,
                          final String implementationVendor,
                          final String[] sections )
    {
        m_specificationTitle = specificationTitle;
        m_specificationVendor = specificationVendor;

        if( null != specificationVersion )
        {
            try
            {
                m_specificationVersion = new DeweyDecimal( specificationVersion );
            }
            catch( final NumberFormatException nfe )
            {
                final String error = "Bad specification version format '" + specificationVersion +
                    "' in '" + specificationTitle + "'. (Reason: " + nfe + ")";
                throw new IllegalArgumentException( error );
            }
        }

        m_implementationTitle = implementationTitle;
        m_implementationVendor = implementationVendor;
        m_implementationVersion = implementationVersion;

        if( null == m_specificationTitle )
        {
            throw new NullPointerException( "specificationTitle" );
        }

        String[] copy = null;
        if( null != sections )
        {
            copy = new String[ sections.length ];
            System.arraycopy( sections, 0, copy, 0, sections.length );
        }
        m_sections = copy;
    }

    /**
     * Get the title of the specification.
     *
     * @return the title of speciication
     */
    public String getSpecificationTitle()
    {
        return m_specificationTitle;
    }

    /**
     * Get the vendor of the specification.
     *
     * @return the vendor of the specification.
     */
    public String getSpecificationVendor()
    {
        return m_specificationVendor;
    }

    /**
     * Get the title of the specification.
     *
     * @return the title of the specification.
     */
    public String getImplementationTitle()
    {
        return m_implementationTitle;
    }

    /**
     * Get the version of the specification.
     *
     * @return the version of the specification.
     */
    public DeweyDecimal getSpecificationVersion()
    {
        return m_specificationVersion;
    }

    /**
     * Get the vendor of the extensions implementation.
     *
     * @return the vendor of the extensions implementation.
     */
    public String getImplementationVendor()
    {
        return m_implementationVendor;
    }

    /**
     * Get the version of the implementation.
     *
     * @return the version of the implementation.
     */
    public String getImplementationVersion()
    {
        return m_implementationVersion;
    }

    /**
     * Return an array containing sections to which specification applies
     * or null if relevent to no sections.
     *
     * @return an array containing sections to which specification applies
     *         or null if relevent to no sections.
     */
    public String[] getSections()
    {
        if( null == m_sections )
        {
            return null;
        }
        else
        {
            final String[] sections = new String[ m_sections.length ];
            System.arraycopy( m_sections, 0, sections, 0, m_sections.length );
            return sections;
        }
    }

    /**
     * Return a Compatibility enum indicating the relationship of this
     * <code>Package Specification</code> with the specified <code>Extension</code>.
     *
     * @param other the other specification
     * @return the enum indicating the compatibility (or lack thereof)
     *         of specifed Package Specification
     */
    public Compatibility getCompatibilityWith( final Specification other )
    {
        // Specification Name must match
        if( !m_specificationTitle.equals( other.getSpecificationTitle() ) )
        {
            return INCOMPATIBLE;
        }

        // Available specification version must be >= required
        final DeweyDecimal specificationVersion = other.getSpecificationVersion();
        if( null != specificationVersion )
        {
            if( null == m_specificationVersion ||
                !isCompatible( m_specificationVersion, specificationVersion ) )
            {
                return REQUIRE_SPECIFICATION_UPGRADE;
            }
        }

        // Implementation Vendor ID must match
        final String implementationVendor = other.getImplementationVendor();
        if( null != implementationVendor )
        {
            if( null == m_implementationVendor ||
                !m_implementationVendor.equals( implementationVendor ) )
            {
                return REQUIRE_VENDOR_SWITCH;
            }
        }

        // Implementation version must be >= required
        final String implementationVersion = other.getImplementationVersion();
        if( null != implementationVersion )
        {
            if( null == m_implementationVersion ||
                !m_implementationVersion.equals( implementationVersion ) )
            {
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
    public boolean isCompatibleWith( final Specification other )
    {
        return ( COMPATIBLE == getCompatibilityWith( other ) );
    }

    /**
     * Return a String representation of this object.
     *
     * @return string representation of object.
     */
    public String toString()
    {
        final String lineSeparator = System.getProperty( "line.separator" );
        final String brace = ": ";

        final StringBuffer sb = new StringBuffer( SPECIFICATION_TITLE.toString() );
        sb.append( brace );
        sb.append( m_specificationTitle );
        sb.append( lineSeparator );

        if( null != m_specificationVersion )
        {
            sb.append( SPECIFICATION_VERSION );
            sb.append( brace );
            sb.append( m_specificationVersion );
            sb.append( lineSeparator );
        }

        if( null != m_specificationVendor )
        {
            sb.append( SPECIFICATION_VENDOR );
            sb.append( brace );
            sb.append( m_specificationVendor );
            sb.append( lineSeparator );
        }

        if( null != m_implementationTitle )
        {
            sb.append( IMPLEMENTATION_TITLE );
            sb.append( brace );
            sb.append( m_implementationTitle );
            sb.append( lineSeparator );
        }

        if( null != m_implementationVersion )
        {
            sb.append( IMPLEMENTATION_VERSION );
            sb.append( brace );
            sb.append( m_implementationVersion );
            sb.append( lineSeparator );
        }

        if( null != m_implementationVendor )
        {
            sb.append( IMPLEMENTATION_VENDOR );
            sb.append( brace );
            sb.append( m_implementationVendor );
            sb.append( lineSeparator );
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
    private boolean isCompatible( final DeweyDecimal first, final DeweyDecimal second )
    {
        return first.isGreaterThanOrEqual( second );
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
    private static ArrayList removeDuplicates( final ArrayList list )
    {
        final ArrayList results = new ArrayList();
        final ArrayList sections = new ArrayList();
        while( list.size() > 0 )
        {
            final Specification specification = (Specification)list.remove( 0 );
            final Iterator iterator = list.iterator();
            while( iterator.hasNext() )
            {
                final Specification other = (Specification)iterator.next();
                if( isEqual( specification, other ) )
                {
                    final String[] otherSections = other.getSections();
                    if( null != sections )
                    {
                        sections.addAll( Arrays.asList( otherSections ) );
                    }
                    iterator.remove();
                }
            }

            final Specification merged =
                mergeInSections( specification, sections );
            results.add( merged );
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
    private static boolean isEqual( final Specification specification,
                                    final Specification other )
    {
        return
            specification.getSpecificationTitle().equals( other.getSpecificationTitle() ) &&
            specification.getSpecificationVersion().isEqual( other.getSpecificationVersion() ) &&
            specification.getSpecificationVendor().equals( other.getSpecificationVendor() ) &&
            specification.getImplementationTitle().equals( other.getImplementationTitle() ) &&
            specification.getImplementationVersion().equals( other.getImplementationVersion() ) &&
            specification.getImplementationVendor().equals( other.getImplementationVendor() );
    }

    /**
     * Merge the specified sections into specified section and return result.
     * If no sections to be added then just return original specification.
     *
     * @param specification the specification
     * @param sectionsToAdd the list of sections to merge
     * @return the merged specification
     */
    private static Specification mergeInSections( final Specification specification,
                                                  final ArrayList sectionsToAdd )
    {
        if( 0 == sectionsToAdd.size() )
        {
            return specification;
        }
        else
        {
            sectionsToAdd.addAll( Arrays.asList( specification.getSections() ) );

            final String[] sections =
                (String[])sectionsToAdd.toArray( new String[ sectionsToAdd.size() ] );

            return new Specification( specification.getSpecificationTitle(),
                                      specification.getSpecificationVersion().toString(),
                                      specification.getSpecificationVendor(),
                                      specification.getImplementationTitle(),
                                      specification.getImplementationVersion(),
                                      specification.getImplementationVendor(),
                                      sections );
        }
    }

    /**
     * Trim the supplied string if the string is non-null
     *
     * @param value the string to trim or null
     * @return the trimmed string or null
     */
    private static String getTrimmedString( final String value )
    {
        if( null == value )
        {
            return null;
        }
        else
        {
            return value.trim();
        }
    }

    /**
     * Extract an Package Specification from Attributes.
     *
     * @param attributes Attributes to searched
     * @return the new Specification object, or null
     */
    private static Specification getSpecification( final String section,
                                                   final Attributes attributes )
        throws ParseException
    {
        //WARNING: We trim the values of all the attributes because
        //Some extension declarations are badly defined (ie have spaces
        //after version or vendor)
        final String name = getTrimmedString( attributes.getValue( SPECIFICATION_TITLE ) );
        if( null == name )
        {
            return null;
        }

        final String specVendor = getTrimmedString( attributes.getValue( SPECIFICATION_VENDOR ) );
        if( null == specVendor )
        {
            throw new ParseException( "Missing " + SPECIFICATION_VENDOR, 0 );
        }

        final String specVersion = getTrimmedString( attributes.getValue( SPECIFICATION_VERSION ) );
        if( null == specVersion )
        {
            throw new ParseException( "Missing " + SPECIFICATION_VERSION, 0 );
        }

        final String impTitle = getTrimmedString( attributes.getValue( IMPLEMENTATION_TITLE ) );
        if( null == impTitle )
        {
            throw new ParseException( "Missing " + IMPLEMENTATION_TITLE, 0 );
        }

        final String impVersion = getTrimmedString( attributes.getValue( IMPLEMENTATION_VERSION ) );
        if( null == impVersion )
        {
            throw new ParseException( "Missing " + IMPLEMENTATION_VERSION, 0 );
        }

        final String impVendor = getTrimmedString( attributes.getValue( IMPLEMENTATION_VENDOR ) );
        if( null == impVendor )
        {
            throw new ParseException( "Missing " + IMPLEMENTATION_VENDOR, 0 );
        }

        return new Specification( name, specVersion, specVendor,
                                  impTitle, impVersion, impVendor,
                                  new String[]{section} );
    }
}