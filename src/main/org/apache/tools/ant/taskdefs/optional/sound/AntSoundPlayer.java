/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.sound;

// ant includes
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



/**
 * This class is designed to be used by any AntTask that requires audio output.
 *
 * It implements the BuildListener interface to listen for BuildEvents and could
 * be easily extended to provide audio output upon any specific build events occuring.
 *
 * I have only tested this with .WAV and .AIFF sound file formats. Both seem to work fine.
 *
 * @author Nick Pellow
 * @version $Revision$, $Date$
 */

public class AntSoundPlayer implements LineListener, BuildListener {

    private File fileSuccess = null;
    private int loopsSuccess = 0;
    private Long durationSuccess = null;

    private File fileFail = null;
    private int loopsFail = 0;
    private Long durationFail = null;

    public AntSoundPlayer() {

    }

    /**
     * @param file the location of the audio file to be played when the
     *        build is successful
     * @param loops the number of times the file should be played when
     *        the build is successful
     * @param duration the number of milliseconds the file should be
     *        played when the build is successful
     */
    public void addBuildSuccessfulSound(File file, int loops, Long duration) {
        this.fileSuccess = file;
        this.loopsSuccess = loops;
        this.durationSuccess = duration;
    }


    /**
     * @param fileFail the location of the audio file to be played
     *        when the build fails
     * @param loopsFail the number of times the file should be played
     *        when the build is fails
     * @param durationFail the number of milliseconds the file should be
     *        played when the build fails
     */
    public void addBuildFailedSound(File fileFail, int loopsFail, Long durationFail) {
        this.fileFail = fileFail;
        this.loopsFail = loopsFail;
        this.durationFail = durationFail;
    }

    /**
     * Plays the file for duration milliseconds or loops.
     */
    private void play(Project project, File file, int loops, Long duration) {

        Clip audioClip = null;

        AudioInputStream audioInputStream = null;


        try {
            audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException uafe) {
            project.log("Audio format is not yet supported: "
                + uafe.getMessage());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if (audioInputStream != null) {
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info   info = new DataLine.Info(Clip.class, format,
                                             AudioSystem.NOT_SPECIFIED);
            try {
                audioClip = (Clip) AudioSystem.getLine(info);
                audioClip.addLineListener(this);
                audioClip.open(audioInputStream);
            } catch (LineUnavailableException e) {
                project.log("The sound device is currently unavailable");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (duration != null) {
                playClip(audioClip, duration.longValue());
            } else {
                playClip(audioClip, loops);
            }
            audioClip.drain();
            audioClip.close();
        } else {
            project.log("Can't get data from file " + file.getName());
        }
    }

    private void playClip(Clip clip, int loops) {

        clip.loop(loops);
        while (clip.isRunning()) {
        }
    }

    private void playClip(Clip clip, long duration) {
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
        }
    }

    /**
     * This is implemented to listen for any line events and closes the
     * clip if required.
     */
    public void update(LineEvent event) {
        if (event.getType().equals(LineEvent.Type.STOP)) {
            Line line = event.getLine();
            line.close();
        } else if (event.getType().equals(LineEvent.Type.CLOSE)) {
            /*
             *  There is a bug in JavaSound 0.90 (jdk1.3beta).
             *  It prevents correct termination of the VM.
             *  So we have to exit ourselves.
             */
            //System.exit(0);
        }
    }


    /**
     *  Fired before any targets are started.
     */
    public void buildStarted(BuildEvent event) {
    }

    /**
     *  Fired after the last target has finished. This event
     *  will still be thrown if an error occured during the build.
     *
     *  @see BuildEvent#getException()
     */
    public void buildFinished(BuildEvent event) {
        if (event.getException() == null && fileSuccess != null) {
            // build successfull!
            play(event.getProject(), fileSuccess, loopsSuccess, durationSuccess);
        } else if (event.getException() != null && fileFail != null) {
            play(event.getProject(), fileFail, loopsFail, durationFail);
        }
    }

    /**
     *  Fired when a target is started.
     *
     *  @see BuildEvent#getTarget()
     */
    public void targetStarted(BuildEvent event) {
    }

    /**
     *  Fired when a target has finished. This event will
     *  still be thrown if an error occured during the build.
     *
     *  @see BuildEvent#getException()
     */
    public void targetFinished(BuildEvent event) {
    }

    /**
     *  Fired when a task is started.
     *
     *  @see BuildEvent#getTask()
     */
    public void taskStarted(BuildEvent event) {
    }

    /**
     *  Fired when a task has finished. This event will still
     *  be throw if an error occured during the build.
     *
     *  @see BuildEvent#getException()
     */
    public void taskFinished(BuildEvent event) {
    }

    /**
     *  Fired whenever a message is logged.
     *
     *  @see BuildEvent#getMessage()
     *  @see BuildEvent#getPriority()
     */
    public void messageLogged(BuildEvent event) {
    }
}

