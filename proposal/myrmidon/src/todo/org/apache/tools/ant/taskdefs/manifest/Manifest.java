/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.manifest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.jar.Attributes;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Class to manage Manifest information
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$ $Date$
 */
public class Manifest
    extends AbstractTask
{
    /**
     * The standard Signature Version header
     */
    public final static String ATTRIBUTE_SIGNATURE_VERSION = Attributes.Name.SIGNATURE_VERSION.toString();

    /**
     * The Name Attribute is the first in a named section
     */
    public final static String ATTRIBUTE_NAME = "Name";

    /**
     * The From Header is disallowed in a Manifest
     */
    public final static String ATTRIBUTE_FROM = "From";

    /**
     * The Class-Path Header is special - it can be duplicated
     */
    public final static String ATTRIBUTE_CLASSPATH = Attributes.Name.CLASS_PATH.toString();

    /**
     * Default Manifest version if one is not specified
     */
    public final static String DEFAULT_MANIFEST_VERSION = "1.0";

    /**
     * The max length of a line in a Manifest
     */
    public final static int MAX_LINE_LENGTH = 70;

    /**
     * The version of this manifest
     */
    private String m_manifestVersion = DEFAULT_MANIFEST_VERSION;

    /**
     * The main section of this manifest
     */
    private Section m_mainSection = new Section();

    /**
     * The named sections of this manifest
     */
    private Hashtable m_sections = new Hashtable();

    private File m_manifestFile;

    private ManifestMode m_mode;

    /**
     * Construct an empty manifest
     */
    public Manifest()
        throws TaskException
    {
        m_mode = new ManifestMode();
        m_mode.setValue( "replace" );
        m_manifestVersion = null;
    }

    /**
     * Read a manifest file from the given reader
     *
     * @param r Description of Parameter
     * @exception ManifestException Description of Exception
     * @exception IOException Description of Exception
     * @throws ManifestException if the manifest is not valid according to the
     *      JAR spec
     * @throws IOException if the manifest cannot be read from the reader.
     */
    public Manifest( Reader r )
        throws ManifestException, TaskException, IOException
    {
        BufferedReader reader = new BufferedReader( r );
        // This should be the manifest version
        String nextSectionName = m_mainSection.read( reader );
        String readManifestVersion = m_mainSection.getAttributeValue( Attributes.Name.MANIFEST_VERSION.toString() );
        if( readManifestVersion != null )
        {
            m_manifestVersion = readManifestVersion;
            m_mainSection.removeAttribute( Attributes.Name.MANIFEST_VERSION.toString() );
        }

        String line = null;
        while( ( line = reader.readLine() ) != null )
        {
            if( line.length() == 0 )
            {
                continue;
            }

            Section section = new Section();
            if( nextSectionName == null )
            {
                Attribute sectionName = ManifestUtil.buildAttribute( line );
                if( !sectionName.getName().equalsIgnoreCase( ATTRIBUTE_NAME ) )
                {
                    throw new ManifestException( "Manifest sections should start with a \"" + ATTRIBUTE_NAME +
                                                 "\" attribute and not \"" + sectionName.getName() + "\"" );
                }
                nextSectionName = sectionName.getValue();
            }
            else
            {
                // we have already started reading this section
                // this line is the first attribute. set it and then let the normal
                // read handle the rest
                Attribute firstAttribute = ManifestUtil.buildAttribute( line );
                section.addAttributeAndCheck( firstAttribute );
            }

            section.setName( nextSectionName );
            nextSectionName = section.read( reader );
            addSection( section );
        }
    }

    /**
     * Construct a manifest from Ant's default manifest file.
     *
     * @return The DefaultManifest value
     * @exception TaskException Description of Exception
     */
    public static Manifest getDefaultManifest()
        throws TaskException
    {
        try
        {
            String s = "/org/apache/tools/ant/defaultManifest.mf";
            InputStream in = Manifest.class.getResourceAsStream( s );
            if( in == null )
            {
                throw new TaskException( "Could not find default manifest: " + s );
            }
            try
            {
                return new Manifest( new InputStreamReader( in, "ASCII" ) );
            }
            catch( UnsupportedEncodingException e )
            {
                return new Manifest( new InputStreamReader( in ) );
            }
        }
        catch( ManifestException e )
        {
            throw new TaskException( "Default manifest is invalid !!" );
        }
        catch( IOException e )
        {
            throw new TaskException( "Unable to read default manifest", e );
        }
    }

    /**
     * The name of the manifest file to write (if used as a task).
     *
     * @param f The new File value
     */
    public void setFile( File f )
    {
        m_manifestFile = f;
    }

    /**
     * Shall we update or replace an existing manifest?
     *
     * @param m The new ManifestMode value
     */
    public void setMode( ManifestMode m )
    {
        m_mode = m;
    }

    /**
     * Get the warnings for this manifest.
     *
     * @return an enumeration of warning strings
     */
    public Iterator getWarnings()
    {
        ArrayList warnings = new ArrayList();

        for( Iterator e2 = m_mainSection.getWarnings(); e2.hasNext(); )
        {
            warnings.add( e2.next() );
        }

        // create a vector and add in the warnings for all the sections
        for( Enumeration e = m_sections.elements(); e.hasMoreElements(); )
        {
            Section section = (Section)e.nextElement();
            for( Iterator e2 = section.getWarnings(); e2.hasNext(); )
            {
                warnings.add( e2.next() );
            }
        }

        return warnings.iterator();
    }

    public void addAttribute( final Attribute attribute )
        throws ManifestException, TaskException
    {
        m_mainSection.addAttribute( attribute );
    }

    public void addSection( final Section section )
        throws ManifestException, TaskException
    {
        if( section.getName() == null )
        {
            throw new TaskException( "Sections must have a name" );
        }
        m_sections.put( section.getName().toLowerCase(), section );
    }

    public boolean equals( Object rhs )
    {
        if( !( rhs instanceof Manifest ) )
        {
            return false;
        }

        Manifest rhsManifest = (Manifest)rhs;
        if( m_manifestVersion == null )
        {
            if( rhsManifest.m_manifestVersion != null )
            {
                return false;
            }
        }
        else if( !m_manifestVersion.equals( rhsManifest.m_manifestVersion ) )
        {
            return false;
        }
        if( m_sections.size() != rhsManifest.m_sections.size() )
        {
            return false;
        }

        if( !m_mainSection.equals( rhsManifest.m_mainSection ) )
        {
            return false;
        }

        for( Enumeration e = m_sections.elements(); e.hasMoreElements(); )
        {
            Section section = (Section)e.nextElement();
            Section rhsSection = (Section)rhsManifest.m_sections.get( section.getName().toLowerCase() );
            if( !section.equals( rhsSection ) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Create or update the Manifest when used as a task.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        if( m_manifestFile == null )
        {
            throw new TaskException( "the file attribute is required" );
        }

        Manifest toWrite = getDefaultManifest();

        if( m_mode.getValue().equals( "update" ) && m_manifestFile.exists() )
        {
            FileReader f = null;
            try
            {
                f = new FileReader( m_manifestFile );
                toWrite.merge( new Manifest( f ) );
            }
            catch( ManifestException m )
            {
                throw new TaskException( "Existing manifest " + m_manifestFile
                                         + " is invalid", m );
            }
            catch( IOException e )
            {
                throw new
                    TaskException( "Failed to read " + m_manifestFile, e );
            }
            finally
            {
                if( f != null )
                {
                    try
                    {
                        f.close();
                    }
                    catch( IOException e )
                    {
                    }
                }
            }
        }

        try
        {
            toWrite.merge( this );
        }
        catch( ManifestException m )
        {
            throw new TaskException( "Manifest is invalid", m );
        }

        PrintWriter w = null;
        try
        {
            w = new PrintWriter( new FileWriter( m_manifestFile ) );
            toWrite.write( w );
        }
        catch( IOException e )
        {
            throw new TaskException( "Failed to write " + m_manifestFile, e );
        }
        finally
        {
            if( w != null )
            {
                w.close();
            }
        }
    }

    /**
     * Merge the contents of the given manifest into this manifest
     *
     * @param other the Manifest to be merged with this one.
     * @throws ManifestException if there is a problem merging the manfest
     *      according to the Manifest spec.
     */
    public void merge( Manifest other )
        throws ManifestException
    {
        if( other.m_manifestVersion != null )
        {
            m_manifestVersion = other.m_manifestVersion;
        }
        m_mainSection.merge( other.m_mainSection );
        for( Enumeration e = other.m_sections.keys(); e.hasMoreElements(); )
        {
            String sectionName = (String)e.nextElement();
            Section ourSection = (Section)m_sections.get( sectionName );
            Section otherSection = (Section)other.m_sections.get( sectionName );
            if( ourSection == null )
            {
                m_sections.put( sectionName.toLowerCase(), otherSection );
            }
            else
            {
                ourSection.merge( otherSection );
            }
        }

    }

    /**
     * Convert the manifest to its string representation
     *
     * @return a multiline string with the Manifest as it appears in a Manifest
     *      file.
     */
    public String toString()
    {
        StringWriter sw = new StringWriter();
        try
        {
            write( new PrintWriter( sw ) );
        }
        catch( Exception e )
        {
            return null;
        }
        return sw.toString();
    }

    /**
     * Write the manifest out to a print writer.
     *
     * @param writer the Writer to which the manifest is written
     * @throws IOException if the manifest cannot be written
     */
    public void write( PrintWriter writer )
        throws IOException, TaskException
    {
        writer.println( Attributes.Name.MANIFEST_VERSION + ": " + m_manifestVersion );
        String signatureVersion = m_mainSection.getAttributeValue( ATTRIBUTE_SIGNATURE_VERSION );
        if( signatureVersion != null )
        {
            writer.println( ATTRIBUTE_SIGNATURE_VERSION + ": " + signatureVersion );
            m_mainSection.removeAttribute( ATTRIBUTE_SIGNATURE_VERSION );
        }
        m_mainSection.write( writer );
        if( signatureVersion != null )
        {
            try
            {
                m_mainSection.addAttribute( new Attribute( ATTRIBUTE_SIGNATURE_VERSION, signatureVersion ) );
            }
            catch( ManifestException e )
            {
                // shouldn't happen - ignore
            }
        }

        for( Enumeration e = m_sections.elements(); e.hasMoreElements(); )
        {
            Section section = (Section)e.nextElement();
            section.write( writer );
        }
    }

}
