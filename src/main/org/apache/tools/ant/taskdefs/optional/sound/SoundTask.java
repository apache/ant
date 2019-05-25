/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
 */

public class SoundTask extends Task {

    private BuildAlert success = null;
    private BuildAlert fail = null;

    /**
     * add a sound when the build succeeds
     * @return a BuildAlert to be configured
     */
    public BuildAlert createSuccess() {
        success = new BuildAlert();
        return success;
    }

    /**
     * add a sound when the build fails
     * @return a BuildAlert to be configured
     */
    public BuildAlert createFail() {
        fail = new BuildAlert();
        return fail;
     }

    /** Constructor for SoundTask. */
    public SoundTask() {
    }

    /**
     * Initialize the task.
     */
    public void init() {
    }

    /**
     * Execute the task.
     */
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
         * @param duration the duration in milliseconds
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
         * @return the file location
         */
        public File getSource() {
            File nofile = null;
            // Check if source is a directory
            if (source.exists()) {
                if (source.isDirectory()) {
                    // get the list of files in the dir
                    Vector<File> files = new Vector<>();
                    for (String file : source.list()) {
                        File f = new File(source, file);
                        if (f.isFile()) {
                            files.addElement(f);
                        }
                    }
                    if (files.size() < 1) {
                        throw new BuildException("No files found in directory " + source);
                    }
                    int numfiles = files.size();
                    // get a random number between 0 and the number of files
                    Random rn = new Random();
                    int x = rn.nextInt(numfiles);
                    // set the source to the file at that location
                    this.source = files.elementAt(x);
                }
            } else {
                log(source + ": invalid path.", Project.MSG_WARN);
                this.source = nofile;
            }
            return this.source;
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
         * @return the duration in milliseconds
         */
        public Long getDuration() {
            return this.duration;
        }
    }
}

