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
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 * This selector selects files against a mapped set of target files, selecting
 * all those files which are different.
 * Files with different lengths are deemed different
 * automatically
 * Files with identical timestamps are viewed as matching by
 * default, unless you specify otherwise.
 * Contents are compared if the lengths are the same
 * and the timestamps are ignored or the same,
 * except if you decide to ignore contents to gain speed.
 * <p>
 * This is a useful selector to work with programs and tasks that don't handle
 * dependency checking properly; Even if a predecessor task always creates its
 * output files, followup tasks can be driven off copies made with a different
 * selector, so their dependencies are driven on the absolute state of the
 * files, not a timestamp.
 * </p>
 * <p>
 * Clearly, however, bulk file comparisons is inefficient; anything that can
 * use timestamps is to be preferred. If this selector must be used, use it
 * over as few files as possible, perhaps following it with an &lt;uptodate&gt;
 * to keep the descendant routines conditional.
 * </p>
 */
public class DifferentSelector extends MappingSelector {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private boolean ignoreFileTimes = true;
    private boolean ignoreContents = false;

    /**
     * This flag tells the selector to ignore file times in the comparison
     * @param ignoreFileTimes if true ignore file times
     */
    public void setIgnoreFileTimes(boolean ignoreFileTimes) {
        this.ignoreFileTimes = ignoreFileTimes;
    }

    /**
     * This flag tells the selector to ignore contents
     * @param ignoreContents if true ignore contents
     * @since ant 1.6.3
     */
    public void setIgnoreContents(boolean ignoreContents) {
        this.ignoreContents = ignoreContents;
    }

    /**
     * this test is our selection test that compared the file with the destfile
     * @param srcfile the source file
     * @param destfile the destination file
     * @return true if the files are different
     */
    protected boolean selectionTest(File srcfile, File destfile) {

        //if either of them is missing, they are different
        if (srcfile.exists() != destfile.exists()) {
            return true;
        }

        if (srcfile.length() != destfile.length()) {
            // different size =>different files
            return true;
        }

        if (!ignoreFileTimes) {
            //same date if dest timestamp is within granularity of the srcfile
            boolean sameDate;
            sameDate = destfile.lastModified() >= srcfile.lastModified() - granularity
                    && destfile.lastModified() <= srcfile.lastModified() + granularity;

            // different dates => different files
            if (!sameDate) {
                return true;
            }
        }
        if (ignoreContents) {
            return false;
        }
        //here do a bulk comparison
        try {
            return !FILE_UTILS.contentEquals(srcfile, destfile);
        } catch (IOException e) {
            throw new BuildException(
                "while comparing " + srcfile + " and " + destfile, e);
        }
    }
}
