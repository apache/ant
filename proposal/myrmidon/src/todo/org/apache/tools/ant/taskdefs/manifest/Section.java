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
import org.apache.myrmidon.api.TaskException;

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
    private ArrayList warnings = new ArrayList();

    /**
     * The section's name if any. The main section in a manifest is unnamed.
     */
    private String name = null;

    /**
     * The section's attributes.
     */
    private Hashtable attributes = new Hashtable();

    /**
     * Set the Section's name
     *
     * @param name the section's name
     */
    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Get the value of the attribute with the name given.
     *
     * @param attributeName the name of the attribute to be returned.
     * @return the attribute's value or null if the attribute does not exist
     *      in the section
     */
    public String getAttributeValue( String attributeName )
    {
        Object attribute = attributes.get( attributeName.toLowerCase() );
        if( attribute == null )
        {
            return null;
        }
        if( attribute instanceof Attribute )
        {
            return ( (Attribute)attribute ).getValue();
        }
        else
        {
            String value = "";
            for( Iterator e = ( (ArrayList)attribute ).iterator(); e.hasNext(); )
            {
                Attribute classpathAttribute = (Attribute)e.next();
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
        return name;
    }

    public Iterator getWarnings()
    {
        return warnings.iterator();
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
        throws ManifestException, TaskException
    {
        if( attribute.getName() == null || attribute.getValue() == null )
        {
            throw new TaskException( "Attributes must have name and value" );
        }
        if( attribute.getName().equalsIgnoreCase( Manifest.ATTRIBUTE_NAME ) )
        {
            warnings.add( "\"" + Manifest.ATTRIBUTE_NAME + "\" attributes should not occur in the " +
                          "main section and must be the first element in all " +
                          "other sections: \"" + attribute.getName() + ": " + attribute.getValue() + "\"" );
            return attribute.getValue();
        }

        if( attribute.getName().toLowerCase().startsWith( Manifest.ATTRIBUTE_FROM.toLowerCase() ) )
        {
            warnings.add( "Manifest attributes should not start with \"" +
                          Manifest.ATTRIBUTE_FROM + "\" in \"" + attribute.getName() + ": " + attribute.getValue() + "\"" );
        }
        else
        {
            // classpath attributes go into a vector
            String attributeName = attribute.getName().toLowerCase();
            if( attributeName.equals( Manifest.ATTRIBUTE_CLASSPATH ) )
            {
                ArrayList classpathAttrs = (ArrayList)attributes.get( attributeName );
                if( classpathAttrs == null )
                {
                    classpathAttrs = new ArrayList();
                    attributes.put( attributeName, classpathAttrs );
                }
                classpathAttrs.add( attribute );
            }
            else if( attributes.containsKey( attributeName ) )
            {
                throw new ManifestException( "The attribute \"" + attribute.getName() + "\" may not " +
                                             "occur more than once in the same section" );
            }
            else
            {
                attributes.put( attributeName, attribute );
            }
        }
        return null;
    }

    public void addAttribute( Attribute attribute )
        throws ManifestException, TaskException
    {
        String check = addAttributeAndCheck( attribute );
        if( check != null )
        {
            throw new TaskException( "Specify the section name using the \"name\" attribute of the <section> element rather " +
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
        if( attributes.size() != rhsSection.attributes.size() )
        {
            return false;
        }

        for( Enumeration e = attributes.elements(); e.hasMoreElements(); )
        {
            Attribute attribute = (Attribute)e.nextElement();
            Attribute rshAttribute = (Attribute)rhsSection.attributes.get( attribute.getName().toLowerCase() );
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
        if( name == null && section.getName() != null ||
            name != null && !( name.equalsIgnoreCase( section.getName() ) ) )
        {
            throw new ManifestException( "Unable to merge sections with different names" );
        }

        for( Enumeration e = section.attributes.keys(); e.hasMoreElements(); )
        {
            String attributeName = (String)e.nextElement();
            if( attributeName.equals( Manifest.ATTRIBUTE_CLASSPATH ) &&
                attributes.containsKey( attributeName ) )
            {
                // classpath entries are vetors which are merged
                ArrayList classpathAttrs = (ArrayList)section.attributes.get( attributeName );
                ArrayList ourClasspathAttrs = (ArrayList)attributes.get( attributeName );
                for( Iterator e2 = classpathAttrs.iterator(); e2.hasNext(); )
                {
                    ourClasspathAttrs.add( e2.next() );
                }
            }
            else
            {
                // the merge file always wins
                attributes.put( attributeName, section.attributes.get( attributeName ) );
            }
        }

        // add in the warnings
        for( Iterator e = section.warnings.iterator(); e.hasNext(); )
        {
            warnings.add( e.next() );
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
        throws ManifestException, IOException, TaskException
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
                    if( name != null )
                    {
                        // a continuation on the first line is a continuation of the name - concatenate
                        // this line and the name
                        name += line.substring( 1 );
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
        attributes.remove( attributeName.toLowerCase() );
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
        if( name != null )
        {
            Attribute nameAttr = new Attribute( Manifest.ATTRIBUTE_NAME, name );
            ManifestUtil.write( nameAttr, writer );
        }
        for( Enumeration e = attributes.elements(); e.hasMoreElements(); )
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
