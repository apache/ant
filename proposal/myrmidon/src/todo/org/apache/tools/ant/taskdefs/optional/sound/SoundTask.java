/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.sound;

import java.io.File;
import java.util.Random;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
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

public class SoundTask extends Task
{

    private BuildAlert success = null;
    private BuildAlert fail = null;

    public SoundTask()
    {
    }

    public BuildAlert createFail()
    {
        fail = new BuildAlert();
        return fail;
    }

    public BuildAlert createSuccess()
    {
        success = new BuildAlert();
        return success;
    }

    public void execute()
    {

        AntSoundPlayer soundPlayer = new AntSoundPlayer();

        if( success == null )
        {
            log( "No nested success element found.", Project.MSG_WARN );
        }
        else
        {
            soundPlayer.addBuildSuccessfulSound( success.getSource(),
                                                 success.getLoops(), success.getDuration() );
        }

        if( fail == null )
        {
            log( "No nested failure element found.", Project.MSG_WARN );
        }
        else
        {
            soundPlayer.addBuildFailedSound( fail.getSource(),
                                             fail.getLoops(), fail.getDuration() );
        }

        getProject().addBuildListener( soundPlayer );

    }

    public void init()
    {
    }

    /**
     * A class to be extended by any BuildAlert's that require the output of
     * sound.
     *
     * @author RT
     */
    public class BuildAlert
    {
        private File source = null;
        private int loops = 0;
        private Long duration = null;

        /**
         * Sets the duration in milliseconds the file should be played.
         *
         * @param duration The new Duration value
         */
        public void setDuration( Long duration )
        {
            this.duration = duration;
        }

        /**
         * Sets the number of times the source file should be played.
         *
         * @param loops the number of loops to play the source file
         */
        public void setLoops( int loops )
        {
            this.loops = loops;
        }

        /**
         * Sets the location of the file to get the audio.
         *
         * @param source the name of a sound-file directory or of the audio file
         */
        public void setSource( File source )
        {
            this.source = source;
        }

        /**
         * Gets the duration in milliseconds the file should be played.
         *
         * @return The Duration value
         */
        public Long getDuration()
        {
            return this.duration;
        }

        /**
         * Sets the number of times the source file should be played.
         *
         * @return the number of loops to play the source file
         */
        public int getLoops()
        {
            return this.loops;
        }

        /**
         * Gets the location of the file to get the audio.
         *
         * @return The Source value
         */
        public File getSource()
        {
            File nofile = null;
            // Check if source is a directory
            if( source.exists() )
            {
                if( source.isDirectory() )
                {
                    // get the list of files in the dir
                    String[] entries = source.list();
                    Vector files = new Vector();
                    for( int i = 0; i < entries.length; i++ )
                    {
                        File f = new File( source, entries[ i ] );
                        if( f.isFile() )
                        {
                            files.addElement( f );
                        }
                    }
                    if( files.size() < 1 )
                    {
                        throw new TaskException( "No files found in directory " + source );
                    }
                    int numfiles = files.size();
                    // get a random number between 0 and the number of files
                    Random rn = new Random();
                    int x = rn.nextInt( numfiles );
                    // set the source to the file at that location
                    this.source = (File)files.elementAt( x );
                }
            }
            else
            {
                log( source + ": invalid path.", Project.MSG_WARN );
                this.source = nofile;
            }
            return this.source;
        }
    }
}

