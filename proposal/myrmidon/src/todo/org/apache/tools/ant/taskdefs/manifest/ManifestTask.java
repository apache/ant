/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.manifest;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.aut.manifest.Attribute;
import org.apache.aut.manifest.ManifestException;
import org.apache.aut.manifest.ManifestUtil;

/**
 * Class to manage Manifest information
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$ $Date$
 */
public class ManifestTask
    extends AbstractTask
{
    private File m_destFile;
    private ManifestMode m_mode;
    private Manifest m_manifest = new Manifest();

    /**
     * Construct an empty manifest
     */
    public ManifestTask()
        throws TaskException
    {
        m_mode = new ManifestMode();
        m_mode.setValue( "replace" );
    }

    /**
     * The name of the manifest file to write.
     */
    public void setDestFile( final File destFile )
    {
        m_destFile = destFile;
    }

    /**
     * Shall we update or replace an existing manifest?
     */
    public void setMode( final ManifestMode mode )
    {
        m_mode = mode;
    }

    public void setManifestVersion( String manifestVersion )
    {
        m_manifest.setManifestVersion( manifestVersion );
    }

    public void addMainSection( Section mainSection )
        throws Exception
    {
        m_manifest.setMainSection( mainSection );
    }

    /**
     * Get the warnings for this manifest.
     *
     * @return an enumeration of warning strings
     */
    public Iterator getWarnings()
    {
        ArrayList warnings = new ArrayList();

        for( Iterator e2 = m_manifest.getMainSection().getWarnings(); e2.hasNext(); )
        {
            warnings.add( e2.next() );
        }

        final Section[] sections = m_manifest.getSections();
        for( int i = 0; i < sections.length; i++ )
        {
            final Section section = sections[ i ];
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
        m_manifest.addAttribute( attribute );
    }

    public void addSection( final Section section )
        throws TaskException
    {
        try
        {
            m_manifest.addSection( section );
        }
        catch( final ManifestException me )
        {
            throw new TaskException( me.getMessage(), me );
        }
    }

    /**
     * Create or update the Manifest when used as a task.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        if( null == m_destFile )
        {
            throw new TaskException( "the file attribute is required" );
        }

        Manifest toWrite = getDefaultManifest();

        if( m_mode.getValue().equals( "update" ) && m_destFile.exists() )
        {
            FileReader f = null;
            try
            {
                f = new FileReader( m_destFile );
                final Manifest other = ManifestUtil.buildManifest( f );
                toWrite.merge( other );
            }
            catch( ManifestException m )
            {
                throw new TaskException( "Existing manifest " + m_destFile
                                         + " is invalid", m );
            }
            catch( IOException e )
            {
                throw new
                    TaskException( "Failed to read " + m_destFile, e );
            }
            finally
            {
                IOUtil.shutdownReader( f );
            }
        }

        try
        {
            toWrite.merge( m_manifest );
        }
        catch( ManifestException m )
        {
            throw new TaskException( "Manifest is invalid", m );
        }

        PrintWriter w = null;
        try
        {
            w = new PrintWriter( new FileWriter( m_destFile ) );
            ManifestUtil.write( toWrite, w );
        }
        catch( IOException e )
        {
            throw new TaskException( "Failed to write " + m_destFile, e );
        }
        finally
        {
            IOUtil.shutdownWriter( w );
        }
    }

    private Manifest getDefaultManifest()
        throws TaskException
    {
        try
        {
            return ManifestUtil.getDefaultManifest();
        }
        catch( final ManifestException me )
        {
            throw new TaskException( me.getMessage(), me );
        }
    }
}
