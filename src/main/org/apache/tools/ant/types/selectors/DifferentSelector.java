/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2003 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "Ant" and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */

package org.apache.tools.ant.types.selectors;

import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.IOException;

/**
 * This selector selects files against a mapped set of target files, selecting all those
 * files which are different. A byte-by-byte comparision is performed on equal length files;
 * files with different lengths are deemed different automatically; files with identical timestamps
 * are viewed as matching by default, unless you specify otherwise.
 * <p>
 * This is a useful selector to work with programs and tasks that don't handle
 * dependency checking properly; Even if a predecessor task always creates its
 * output files, followup tasks can be driven off copies made with a different selector,
 * so their dependencies are driven on the absolute state of the files, not a timestamp.
 * <p>
 * Clearly, however, bulk file comparisons is inefficient; anything that can use
 * timestamps is to be preferred. If this selector must be used, use it over as few files
 * as possible, perhaps following it with an &lt;uptodate;&gt to keep the descendent
 * routines conditional.
 *
 */
public class DifferentSelector extends MappingSelector {

    private FileUtils fileUtils= FileUtils.newFileUtils();

    private boolean ignoreFileTimes=true;


    /**
     * This flag tells the selector to ignore file times in the comparison
     * @param ignoreFileTimes
     */
    public void setIgnoreFileTimes(boolean ignoreFileTimes) {
        this.ignoreFileTimes = ignoreFileTimes;
    }

    /**
     * this test is our selection test that compared the file with the destfile
     * @param srcfile
     * @param destfile
     * @return
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
            if(!sameDate) {
                return true;
            }
        }

        //here do a bulk comparison
        try {
            return !fileUtils.contentEquals(srcfile,destfile);
        } catch (IOException e) {
            throw new BuildException("while comparing "+srcfile+" and "+destfile,e);
        }
    }
}
