/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.sound;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;

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
 * @author Nick Pellow
 * @version $Revision$, $Date$
 */
public class SoundTask
    extends Task
{
    private BuildAlert m_success;
    private BuildAlert m_fail;

    public BuildAlert createFail()
    {
        m_fail = new BuildAlert();
        return m_fail;
    }

    public BuildAlert createSuccess()
    {
        m_success = new BuildAlert();
        return m_success;
    }

    public void execute()
    {
        final AntSoundPlayer soundPlayer = new AntSoundPlayer();
        if( null == m_success )
        {
            getLogger().warn( "No nested success element found." );
        }
        else
        {
            soundPlayer.addBuildSuccessfulSound( getRandomSource( m_success ),
                                                 m_success.getLoops(), m_success.getDuration() );
        }

        if( null == m_fail )
        {
            getLogger().warn( "No nested failure element found." );
        }
        else
        {
            soundPlayer.addBuildFailedSound( getRandomSource( m_fail ),
                                             m_fail.getLoops(), m_fail.getDuration() );
        }

        getProject().addProjectListener( soundPlayer );
    }

    /**
     * Gets the location of the file to get the audio.
     *
     * @return The Source value
     */
    private File getRandomSource( final BuildAlert alert )
    {
        final File source = alert.getSource();
        // Check if source is a directory
        if( source.exists() )
        {
            if( source.isDirectory() )
            {
                // get the list of files in the dir
                final String[] entries = source.list();
                ArrayList files = new ArrayList();
                for( int i = 0; i < entries.length; i++ )
                {
                    File f = new File( source, entries[ i ] );
                    if( f.isFile() )
                    {
                        files.add( f );
                    }
                }
                if( files.size() < 1 )
                {
                    throw new TaskException( "No files found in directory " + source );
                }
                final int numfiles = files.size();
                // get a random number between 0 and the number of files
                final Random random = new Random();
                final int x = random.nextInt( numfiles );
                // set the source to the file at that location
                source = (File)files.get( x );
            }
        }
        else
        {
            getLogger().warn( source + ": invalid path." );
            source = null;
        }
        return source;
    }
}

