/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.sound;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * This class is designed to be used by any AntTask that requires audio output.
 * It implements the BuildListener interface to listen for BuildEvents and could
 * be easily extended to provide audio output upon any specific build events
 * occuring. I have only tested this with .WAV and .AIFF sound file formats.
 * Both seem to work fine.
 *
 * @author Nick Pellow
 * @version $Revision$, $Date$
 */
public class AntSoundPlayer
    extends AbstractLogEnabled
    implements LineListener, BuildListener
{
    private File m_fileSuccess;
    private int m_loopsSuccess;
    private Long m_durationSuccess;

    private File m_fileFail;
    private int m_loopsFail;
    private Long m_durationFail;

    public AntSoundPlayer()
    {
    }

    /**
     * @param fileFail The feature to be added to the BuildFailedSound attribute
     * @param loopsFail The feature to be added to the BuildFailedSound
     *      attribute
     * @param durationFail The feature to be added to the BuildFailedSound
     *      attribute
     */
    public void addBuildFailedSound( File fileFail, int loopsFail, Long durationFail )
    {
        m_fileFail = fileFail;
        m_loopsFail = loopsFail;
        m_durationFail = durationFail;
    }

    /**
     * @param loops the number of times the file should be played when the build
     *      is successful
     * @param duration the number of milliseconds the file should be played when
     *      the build is successful
     * @param file The feature to be added to the BuildSuccessfulSound attribute
     */
    public void addBuildSuccessfulSound( File file, int loops, Long duration )
    {
        m_fileSuccess = file;
        m_loopsSuccess = loops;
        m_durationSuccess = duration;
    }

    /**
     * Fired after the last target has finished. This event will still be thrown
     * if an error occured during the build.
     *
     * @see BuildEvent#getException()
     */
    public void buildFinished( BuildEvent event )
    {
        if( event.getException() == null && m_fileSuccess != null )
        {
            // build successfull!
            play( m_fileSuccess, m_loopsSuccess, m_durationSuccess );
        }
        else if( event.getException() != null && m_fileFail != null )
        {
            play( m_fileFail, m_loopsFail, m_durationFail );
        }
    }

    /**
     * Fired before any targets are started.
     */
    public void buildStarted( BuildEvent event )
    {
    }

    /**
     * Fired whenever a message is logged.
     *
     * @see BuildEvent#getMessage()
     * @see BuildEvent#getPriority()
     */
    public void messageLogged( BuildEvent event )
    {
    }

    /**
     * Fired when a target has finished. This event will still be thrown if an
     * error occured during the build.
     *
     * @see BuildEvent#getException()
     */
    public void targetFinished( BuildEvent event )
    {
    }

    /**
     * Fired when a target is started.
     *
     * @see BuildEvent#getTarget()
     */
    public void targetStarted( BuildEvent event )
    {
    }

    /**
     * Fired when a task has finished. This event will still be throw if an
     * error occured during the build.
     *
     * @see BuildEvent#getException()
     */
    public void taskFinished( BuildEvent event )
    {
    }

    /**
     * Fired when a task is started.
     *
     * @see BuildEvent#getTask()
     */
    public void taskStarted( BuildEvent event )
    {
    }

    /**
     * This is implemented to listen for any line events and closes the clip if
     * required.
     */
    public void update( LineEvent event )
    {
        if( event.getType().equals( LineEvent.Type.STOP ) )
        {
            Line line = event.getLine();
            line.close();
        }
        else if( event.getType().equals( LineEvent.Type.CLOSE ) )
        {
            /*
             * There is a bug in JavaSound 0.90 (jdk1.3beta).
             * It prevents correct termination of the VM.
             * So we have to exit ourselves.
             */
            //System.exit(0);
        }
    }

    /**
     * Plays the file for duration milliseconds or loops.
     */
    private void play( File file, int loops, Long duration )
    {
        Clip audioClip = null;

        AudioInputStream audioInputStream = null;

        try
        {
            audioInputStream = AudioSystem.getAudioInputStream( file );
        }
        catch( UnsupportedAudioFileException uafe )
        {
            final String message = "Audio format is not yet supported: " + uafe.getMessage();
            getLogger().info( message );
        }
        catch( IOException ioe )
        {
            ioe.printStackTrace();
        }

        if( audioInputStream != null )
        {
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info( Clip.class, format,
                                                    AudioSystem.NOT_SPECIFIED );
            try
            {
                audioClip = (Clip)AudioSystem.getLine( info );
                audioClip.addLineListener( this );
                audioClip.open( audioInputStream );
            }
            catch( LineUnavailableException e )
            {
                final String message = "The sound device is currently unavailable";
                getLogger().info( message );
                return;
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }

            if( duration != null )
            {
                playClip( audioClip, duration.longValue() );
            }
            else
            {
                playClip( audioClip, loops );
            }
            audioClip.drain();
            audioClip.close();
        }
        else
        {
            final String message = "Can't get data from file " + file.getName();
            getLogger().info( message );
        }
    }

    private void playClip( Clip clip, int loops )
    {

        clip.loop( loops );
        while( clip.isRunning() )
        {
        }
    }

    private void playClip( Clip clip, long duration )
    {

        long currentTime = System.currentTimeMillis();
        clip.loop( Clip.LOOP_CONTINUOUSLY );
        try
        {
            Thread.sleep( duration );
        }
        catch( InterruptedException e )
        {
        }
    }
}

