/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.sound;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.workspace.Workspace;

/**
 * This is an example of an AntTask that makes of use of the AntSoundPlayer.
 * There are three attributes to be set: <code>source</code>: the location of
 * the audio file to be played <code>duration</code>: play the sound file
 * continuously until "duration" milliseconds has expired <code>loops</code>:
 * the number of times the sound file should be played until stopped I have only
 * tested this with .WAV and .AIFF sound file formats. Both seem to work fine.
 * plans for the future: - use the midi api to define sounds (or drum beat etc)
 * in xml and have Ant play them back
 *
 * @ant.task name="sound-listener"
 * @author Nick Pellow
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$, $Date$
 */
public class SoundTask
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( SoundTask.class );

    private BuildAlert m_success;
    private BuildAlert m_fail;

    public void addFail( final BuildAlert fail )
    {
        m_fail = fail;
    }

    public void addSuccess( final BuildAlert success )
    {
        m_success = success;
    }

    public void execute()
        throws TaskException
    {
        final AntSoundPlayer soundPlayer = new AntSoundPlayer();
        if( null == m_success )
        {
            final String message = REZ.getString( "sound.missing-success.error" );
            getContext().warn( message );
        }
        else
        {
            final File source = getRandomSource( m_success );
            soundPlayer.addBuildSuccessfulSound( source,
                                                 m_success.getLoops(),
                                                 m_success.getDuration() );
        }

        if( null == m_fail )
        {
            final String message = REZ.getString( "sound.missing-failure.error" );
            getContext().warn( message );
        }
        else
        {
            final File source = getRandomSource( m_fail );
            soundPlayer.addBuildFailedSound( source,
                                             m_fail.getLoops(),
                                             m_fail.getDuration() );
        }

        final Workspace workspace = (Workspace)getContext().getService( Workspace.class );
        workspace.addProjectListener( soundPlayer );
    }

    /**
     * Gets the location of the file to get the audio.
     */
    private File getRandomSource( final BuildAlert alert )
        throws TaskException
    {
        final File source = alert.getSource();
        // Check if source is a directory
        if( source.exists() )
        {
            if( source.isDirectory() )
            {
                // get the list of files in the dir
                final String[] entries = source.list();
                final ArrayList files = new ArrayList();
                for( int i = 0; i < entries.length; i++ )
                {
                    final File file = new File( source, entries[ i ] );
                    if( file.isFile() )
                    {
                        files.add( file );
                    }
                }
                if( files.size() < 1 )
                {
                    final String message = REZ.getString( "sound.empty.dir.error", source );
                    throw new TaskException( message );
                }
                final int numfiles = files.size();
                // get a random number between 0 and the number of files
                final Random random = new Random();
                final int x = random.nextInt( numfiles );
                // set the source to the file at that location
                return (File)files.get( x );
            }
            else
            {
                return null;
            }
        }
        else
        {
            final String message = REZ.getString( "sound.invalid-path.error", source );
            getContext().warn( message );
            return null;
        }
    }
}
