/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.manifest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.aut.manifest.Attribute;
import org.apache.aut.manifest.ManifestException;
import org.apache.aut.manifest.ManifestUtil;

/**
 * Class to represent an individual section in the Manifest. A section
 * consists of a set of attribute values, separated from other sections by a
 * blank line.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class Section
{
    private final ArrayList m_warnings = new ArrayList();

    /**
     * The section's name if any. The main section in a manifest is unnamed.
     */
    private String m_name;

    /**
     * The section's attributes.
     */
    private final Hashtable m_attributes = new Hashtable();

    /**
     * Set the Section's name
     *
     * @param name the section's name
     */
    public void setName( final String name )
    {
        m_name = name;
    }

    /**
     * Get the value of the attribute with the name given.
     *
     * @param attributeName the name of the attribute to be returned.
     * @return the attribute's value or null if the attribute does not exist
     *      in the section
     */
    public String getAttributeValue( final String attributeName )
    {
        final Object attributeObject = m_attributes.get( attributeName.toLowerCase() );
        if( null == attributeObject )
        {
            return null;
        }
        else if( attributeObject instanceof Attribute )
        {
            final Attribute attribute = (Attribute)attributeObject;
            return attribute.getValue();
        }
        else
        {
            String value = "";
            final ArrayList attributes = (ArrayList)attributeObject;
            Iterator e = attributes.iterator();
            while( e.hasNext() )
            {
                final Attribute classpathAttribute = (Attribute)e.next();
                value += classpathAttribute.getValue() + " ";
            }
            return value.trim();
        }
    }

    /**
     * Get the Section's name
     *
     * @return the section's name.
     */
    public String getName()
    {
        return m_name;
    }

    public Iterator getWarnings()
    {
        return m_warnings.iterator();
    }

    /**
     * Add an attribute to the section
     *
     * @param attribute the attribute to be added.
     * @return the value of the attribute if it is a name attribute - null
     *      other wise
     * @throws ManifestException if the attribute already exists in this
     *      section.
     */
    public String addAttributeAndCheck( Attribute attribute )
        throws ManifestException
    {
        if( attribute.getName() == null || attribute.getValue() == null )
        {
            throw new ManifestException( "Attributes must have name and value" );
        }
        if( attribute.getName().equalsIgnoreCase( ManifestUtil.ATTRIBUTE_NAME ) )
        {
            m_warnings.add( "\"" + ManifestUtil.ATTRIBUTE_NAME + "\" attributes should not occur in the " +
                          "main section and must be the first element in all " +
                          "other sections: \"" + attribute.getName() + ": " + attribute.getValue() + "\"" );
            return attribute.getValue();
        }

        if( attribute.getName().toLowerCase().startsWith( ManifestUtil.ATTRIBUTE_FROM.toLowerCase() ) )
        {
            m_warnings.add( "Manifest attributes should not start with \"" +
                          ManifestUtil.ATTRIBUTE_FROM + "\" in \"" + attribute.getName() + ": " + attribute.getValue() + "\"" );
        }
        else
        {
            // classpath attributes go into a vector
            String attributeName = attribute.getName().toLowerCase();
            if( attributeName.equals( ManifestUtil.ATTRIBUTE_CLASSPATH ) )
            {
                ArrayList classpathAttrs = (ArrayList)m_attributes.get( attributeName );
                if( classpathAttrs == null )
                {
                    classpathAttrs = new ArrayList();
                    m_attributes.put( attributeName, classpathAttrs );
                }
                classpathAttrs.add( attribute );
            }
            else if( m_attributes.containsKey( attributeName ) )
            {
                throw new ManifestException( "The attribute \"" + attribute.getName() + "\" may not " +
                                             "occur more than once in the same section" );
            }
            else
            {
                m_attributes.put( attributeName, attribute );
            }
        }
        return null;
    }

    public void addAttribute( final Attribute attribute )
        throws ManifestException
    {
        String check = addAttributeAndCheck( attribute );
        if( check != null )
        {
            throw new ManifestException( "Specify the section name using the \"name\" attribute of the <section> element rather " +
                                     "than using a \"Name\" manifest attribute" );
        }
    }

    public boolean equals( Object rhs )
    {
        if( !( rhs instanceof Section ) )
        {
            return false;
        }

        Section rhsSection = (Section)rhs;
        if( m_attributes.size() != rhsSection.m_attributes.size() )
        {
            return false;
        }

        for( Enumeration e = m_attributes.elements(); e.hasMoreElements(); )
        {
            Attribute attribute = (Attribute)e.nextElement();
            Attribute rshAttribute = (Attribute)rhsSection.m_attributes.get( attribute.getName().toLowerCase() );
            if( !attribute.equals( rshAttribute ) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Merge in another section
     *
     * @param section the section to be merged with this one.
     * @throws ManifestException if the sections cannot be merged.
     */
    public void merge( Section section )
        throws ManifestException
    {
        if( m_name == null && section.getName() != null ||
            m_name != null && !( m_name.equalsIgnoreCase( section.getName() ) ) )
        {
            throw new ManifestException( "Unable to merge sections with different names" );
        }

        for( Enumeration e = section.m_attributes.keys(); e.hasMoreElements(); )
        {
            String attributeName = (String)e.nextElement();
            if( attributeName.equals( ManifestUtil.ATTRIBUTE_CLASSPATH ) &&
                m_attributes.containsKey( attributeName ) )
            {
                // classpath entries are vetors which are merged
                ArrayList classpathAttrs = (ArrayList)section.m_attributes.get( attributeName );
                ArrayList ourClasspathAttrs = (ArrayList)m_attributes.get( attributeName );
                for( Iterator e2 = classpathAttrs.iterator(); e2.hasNext(); )
                {
                    ourClasspathAttrs.add( e2.next() );
                }
            }
            else
            {
                // the merge file always wins
                m_attributes.put( attributeName, section.m_attributes.get( attributeName ) );
            }
        }

        // add in the warnings
        for( Iterator e = section.m_warnings.iterator(); e.hasNext(); )
        {
            m_warnings.add( e.next() );
        }
    }

    /**
     * Read a section through a reader
     *
     * @param reader the reader from which the section is read
     * @return the name of the next section if it has been read as part of
     *      this section - This only happens if the Manifest is malformed.
     * @throws ManifestException if the section is not valid according to
     *      the JAR spec
     * @throws IOException if the section cannot be read from the reader.
     */
    public String read( BufferedReader reader )
        throws ManifestException, IOException
    {
        Attribute attribute = null;
        while( true )
        {
            String line = reader.readLine();
            if( line == null || line.length() == 0 )
            {
                return null;
            }
            if( line.charAt( 0 ) == ' ' )
            {
                // continuation line
                if( attribute == null )
                {
                    if( m_name != null )
                    {
                        // a continuation on the first line is a continuation of the name - concatenate
                        // this line and the name
                        m_name += line.substring( 1 );
                    }
                    else
                    {
                        throw new ManifestException( "Can't start an attribute with a continuation line " + line );
                    }
                }
                else
                {
                    attribute.addContinuation( line );
                }
            }
            else
            {
                attribute = ManifestUtil.buildAttribute( line );
                String nameReadAhead = addAttributeAndCheck( attribute );
                if( nameReadAhead != null )
                {
                    return nameReadAhead;
                }
            }
        }
    }

    /**
     * Remove tge given attribute from the section
     *
     * @param attributeName the name of the attribute to be removed.
     */
    public void removeAttribute( String attributeName )
    {
        m_attributes.remove( attributeName.toLowerCase() );
    }

    /**
     * Write the section out to a print writer.
     *
     * @param writer the Writer to which the section is written
     * @throws IOException if the section cannot be written
     */
    public void write( PrintWriter writer )
        throws IOException
    {
        if( m_name != null )
        {
            Attribute nameAttr = new Attribute( ManifestUtil.ATTRIBUTE_NAME, m_name );
            ManifestUtil.write( nameAttr, writer );
        }
        for( Enumeration e = m_attributes.elements(); e.hasMoreElements(); )
        {
            Object object = e.nextElement();
            if( object instanceof Attribute )
            {
                Attribute attribute = (Attribute)object;
                ManifestUtil.write( attribute, writer );
            }
            else
            {
                ArrayList attrList = (ArrayList)object;
                for( Iterator e2 = attrList.iterator(); e2.hasNext(); )
                {
                    Attribute attribute = (Attribute)e2.next();
                    ManifestUtil.write( attribute, writer );
                }
            }
        }
        writer.println();
    }
}
