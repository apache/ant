/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.manifest;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import org.apache.myrmidon.api.TaskException;

/**
 * Class to manage Manifest information
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author Conor MacNeill
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$ $Date$
 */
public class Manifest
{
    /**
     * The version of this manifest
     */
    private String m_manifestVersion = ManifestUtil.DEFAULT_MANIFEST_VERSION;

    /**
     * The main section of this manifest
     */
    private Section m_mainSection = new Section();

    /**
     * The named sections of this manifest
     */
    private Hashtable m_sections = new Hashtable();

    public void setManifestVersion( final String manifestVersion )
    {
        m_manifestVersion = manifestVersion;
    }

    public void setMainSection( final Section mainSection )
    {
        m_mainSection = mainSection;
    }

    public void addAttribute( final Attribute attribute )
        throws ManifestException
    {
        m_mainSection.addAttribute( attribute );
    }

    public void addSection( final Section section )
        throws ManifestException
    {
        if( section.getName() == null )
        {
            final String message = "Sections must have a name";
            throw new ManifestException( message );
        }
        m_sections.put( section.getName().toLowerCase(), section );
    }

    public String[] getSectionNames( final Manifest other )
    {
        final Set keys = other.m_sections.keySet();
        return (String[])keys.toArray( new String[ keys.size() ] );
    }

    public String getManifestVersion()
    {
        return m_manifestVersion;
    }

    public Section getMainSection()
    {
        return m_mainSection;
    }

    public Section getSection( final String name )
    {
        return (Section)m_sections.get( name );
    }

    public Section[] getSections()
    {
        final Collection sections = m_sections.values();
        return (Section[])sections.toArray( new Section[ sections.size() ] );
    }

    /**
     * Merge the contents of the given manifest into this manifest
     *
     * @param other the Manifest to be merged with this one.
     * @throws org.apache.tools.todo.taskdefs.manifest.ManifestException if there is a problem merging the manfest
     *      according to the Manifest spec.
     */
    public void merge( final Manifest other )
        throws ManifestException
    {
        if( other.m_manifestVersion != null )
        {
            m_manifestVersion = other.m_manifestVersion;
        }
        m_mainSection.merge( other.m_mainSection );

        mergeSections( other );
    }

    public boolean equals( final Object object )
    {
        if( !( object instanceof Manifest ) )
        {
            return false;
        }

        final Manifest other = (Manifest)object;
        if( m_manifestVersion == null && other.m_manifestVersion != null )
        {
            return false;
        }
        else if( !m_manifestVersion.equals( other.m_manifestVersion ) )
        {
            return false;
        }
        if( m_sections.size() != other.m_sections.size() )
        {
            return false;
        }

        if( !m_mainSection.equals( other.m_mainSection ) )
        {
            return false;
        }

        final Iterator e = m_sections.values().iterator();
        while( e.hasNext() )
        {
            final Section section = (Section)e.next();
            final String key = section.getName().toLowerCase();
            final Section otherSection = (Section)other.m_sections.get( key );
            if( !section.equals( otherSection ) )
            {
                return false;
            }
        }

        return true;
    }

    private void mergeSections( final Manifest other )
        throws ManifestException
    {
        final String[] sections = getSectionNames( other );
        for( int i = 0; i < sections.length; i++ )
        {
            final String sectionName = sections[ i ];
            final Section section = getSection( sectionName );
            final Section otherSection = other.getSection( sectionName );
            if( section == null )
            {
                m_sections.put( sectionName.toLowerCase(), otherSection );
            }
            else
            {
                section.merge( otherSection );
            }
        }
    }
}
