/* 
 * Copyright  2000-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 */

package org.apache.tools.ant.types.selectors;

import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.IOException;

/**
 * This selector selects files against a mapped set of target files, selecting
 * all those files which are different. A byte-by-byte comparision is performed
 * on equal length files; files with different lengths are deemed different
 * automatically; files with identical timestamps are viewed as matching by
 * default, unless you specify otherwise.
 * <p>
 * This is a useful selector to work with programs and tasks that don't handle
 * dependency checking properly; Even if a predecessor task always creates its
 * output files, followup tasks can be driven off copies made with a different
 * selector, so their dependencies are driven on the absolute state of the
 * files, not a timestamp.
 * <p>
 * Clearly, however, bulk file comparisons is inefficient; anything that can
 * use timestamps is to be preferred. If this selector must be used, use it
 * over as few files as possible, perhaps following it with an &lt;uptodate;&gt
 * to keep the descendent routines conditional.
 *
 * @author not specified
 */
public class DifferentSelector extends MappingSelector {

    private FileUtils fileUtils = FileUtils.newFileUtils();

    private boolean ignoreFileTimes = true;


    /**
     * This flag tells the selector to ignore file times in the comparison
     * @param ignoreFileTimes if true ignore file times
     */
    public void setIgnoreFileTimes(boolean ignoreFileTimes) {
        this.ignoreFileTimes = ignoreFileTimes;
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

        //here do a bulk comparison
        try {
            return !fileUtils.contentEquals(srcfile, destfile);
        } catch (IOException e) {
            throw new BuildException("while comparing " + srcfile + " and "
                    + destfile, e);
        }
    }
}
