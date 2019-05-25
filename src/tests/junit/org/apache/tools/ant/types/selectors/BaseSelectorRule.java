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

package org.apache.tools.ant.types.selectors;

import java.io.File;

import org.apache.tools.ant.BuildFileRule;


/**
 * Base test case for Selectors. Provides a shared test as well as
 * a test bed for selecting on, and a helper method for determining
 * whether selections are correct.
 *
 */
public class BaseSelectorRule extends BuildFileRule {

    private File beddir;
    private File mirrordir;
    private final String[] filenames = {".", "asf-logo.gif.md5", "asf-logo.gif.bz2",
            "asf-logo.gif.gz", "copy.filterset.filtered", "zip/asf-logo.gif.zip",
            "tar/asf-logo.gif.tar", "tar/asf-logo-huge.tar.gz",
            "tar/gz/asf-logo.gif.tar.gz", "tar/bz2/asf-logo.gif.tar.bz2",
            "tar/bz2/asf-logo-huge.tar.bz2", "tar/bz2"};
    private File[] files = new File[filenames.length];
    private File[] mirrorfiles = new File[filenames.length];

    @Override
    public void before() throws Throwable {
        super.before();
        configureProject("src/etc/testcases/types/selectors.xml");
        executeTarget("setUp");

        executeTarget("setupfiles");
        executeTarget("mirrorfiles");

        beddir = new File(super.getProject().getProperty("test.dir"));
        mirrordir = new File(super.getProject().getProperty("mirror.dir"));

        for (int x = 0; x < files.length; x++) {
            files[x] = new File(beddir, filenames[x]);
            mirrorfiles[x] = new File(mirrordir, filenames[x]);
        }
    }

    @Override
    public void after() {
        super.after();
        executeTarget("tearDown");
    }

    public File getBeddir() {
        return beddir;
    }

    public File[] getMirrorFiles() {
        return mirrorfiles;
    }

    public File[] getFiles() {
        return files;
    }

    public String[] getFilenames() {
        return filenames;
    }


    /**
     * This is a helper method that takes a selector and calls its
     * isSelected() method on each file in the testbed. It returns
     * a string of "T"s amd "F"s
     *
     * @param selector FileSelector
     */
    public String selectionString(FileSelector selector) {
        return selectionString(beddir, files, selector);
    }

    /**
     * This is a helper method that takes a selector and calls its
     * isSelected() method on each file in the mirror testbed. This
     * variation is used for dependency checks and to get around the
     * limitations in the touch task when running JDK 1.1. It returns
     * a string of "T"s amd "F"s.
     *
     * @param selector FileSelector
     */
    public String mirrorSelectionString(FileSelector selector) {
        return selectionString(mirrordir, mirrorfiles, selector);
    }

    /**
     * Worker method for the two convenience methods above. Applies a
     * selector on a set of files passed in and returns a string of
     * "T"s and "F"s from applying the selector to each file.
     *
     * @param basedir File
     * @param files File[]
     * @param selector FileSelector
     */
    public String selectionString(File basedir, File[] files, FileSelector selector) {
        StringBuilder buf = new StringBuilder();
        for (int x = 0; x < files.length; x++) {
            if (selector.isSelected(basedir, filenames[x], files[x])) {
                buf.append('T');
            } else {
                buf.append('F');
            }
        }
        return buf.toString();
    }


}
