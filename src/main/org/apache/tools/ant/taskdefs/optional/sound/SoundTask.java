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

import java.io.File;
import java.util.Random;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Plays a sound file at the end of the build, according to whether the build failed or succeeded.
 *
 * There are three attributes to be set:
 *
 * <code>source</code>: the location of the audio file to be played
 * <code>duration</code>: play the sound file continuously until "duration" milliseconds has expired
 * <code>loops</code>: the number of times the sound file should be played until stopped
 *
 * I have only tested this with .WAV and .AIFF sound file formats. Both seem
 * to work fine.
 *
 * plans for the future:
 * - use the midi api to define sounds (or drum beat etc) in xml and have
 *   Ant play them back
 *
 * @author Nick Pellow
 * @version $Revision$, $Date$
 */

public class SoundTask extends Task {

    private BuildAlert success = null;
    private BuildAlert fail = null;

    /**
     * add a sound when the build succeeds
     */
    public BuildAlert createSuccess() {
        success = new BuildAlert();
        return success;
    }

    /**
     * add a sound when the build fails
     */
    public BuildAlert createFail() {
        fail = new BuildAlert();
        return fail;
     }

    public SoundTask() {
    }

    public void init() {
    }

    public void execute() {

        AntSoundPlayer soundPlayer = new AntSoundPlayer();

        if (success == null) {
            log("No nested success element found.", Project.MSG_WARN);
        } else {
            soundPlayer.addBuildSuccessfulSound(success.getSource(),
              success.getLoops(), success.getDuration());
        }

        if (fail == null) {
            log("No nested failure element found.", Project.MSG_WARN);
        } else {
            soundPlayer.addBuildFailedSound(fail.getSource(),
              fail.getLoops(), fail.getDuration());
        }

        getProject().addBuildListener(soundPlayer);

    }

    /**
     * A class to be extended by any BuildAlert's that require the output
     * of sound.
     */
    public class BuildAlert {
        private File source = null;
        private int loops = 0;
        private Long duration = null;

        /**
         * Sets the duration in milliseconds the file should be played; optional.
         */
        public void setDuration(Long duration) {
            this.duration = duration;
        }

        /**
         * Sets the location of the file to get the audio; required.
         *
         * @param source the name of a sound-file directory or of the audio file
         */
        public void setSource(File source) {
            this.source = source;
        }

        /**
         * Sets the number of times the source file should be played; optional.
         *
         * @param loops the number of loops to play the source file
         */
        public void setLoops(int loops) {
            this.loops = loops;
        }

        /**
         * Gets the location of the file to get the audio.
         */
        public File getSource() {
            File nofile = null ;
            // Check if source is a directory
            if (source.exists()) {
                if (source.isDirectory()) {
                    // get the list of files in the dir
                    String[] entries = source.list() ;
                    Vector files = new Vector() ;
                    for (int i = 0 ; i < entries.length ; i++) {
                        File f = new File(source, entries[i]) ;
                        if (f.isFile()) {
                            files.addElement(f) ;
                        }
                    }
                    if (files.size() < 1) {
                        throw new BuildException("No files found in directory " + source);
                    }
                    int numfiles = files.size() ;
                    // get a random number between 0 and the number of files
                    Random rn = new Random() ;
                    int x = rn.nextInt(numfiles) ;
                    // set the source to the file at that location
                    this.source = (File) files.elementAt(x);
                }
            } else {
                log(source + ": invalid path.", Project.MSG_WARN) ;
                this.source = nofile ;
            }
            return this.source ;
        }

        /**
         * Sets the number of times the source file should be played.
         *
         * @return the number of loops to play the source file
         */
        public int getLoops() {
            return this.loops;
        }

        /**
         * Gets the duration in milliseconds the file should be played.
         */
        public Long getDuration() {
            return this.duration;
        }
    }
}

