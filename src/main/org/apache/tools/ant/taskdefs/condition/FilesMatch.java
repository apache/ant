/*
 * Copyright  2002-2004 The Apache Software Foundation
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
 package org.apache.tools.ant.taskdefs.condition;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 * Compares two files for bitwise equality based on size and
 * content. Timestamps are not at all looked at.
 *
 * @version $Revision$
 * @since Ant 1.5
 */

public class FilesMatch implements Condition {

    /**
     * files to compare
     */
    private File file1, file2;

    /**
     * Helper that provides the file comparison method.
     */
    private FileUtils fu = FileUtils.newFileUtils();

    /**
     * Sets the File1 attribute
     *
     * @param file1 The new File1 value
     */
    public void setFile1(File file1) {
        this.file1 = file1;
    }


    /**
     * Sets the File2 attribute
     *
     * @param file2 The new File2 value
     */
    public void setFile2(File file2) {
        this.file2 = file2;
    }

    /**
     * comparison method of the interface
     *
     * @return true if the files are equal
     * @exception BuildException if it all went pear-shaped
     */
    public boolean eval()
        throws BuildException {

        //validate
        if (file1 == null || file2 == null) {
            throw new BuildException("both file1 and file2 are required in "
                                     + "filesmatch");
        }

        //#now match the files
        boolean matches = false;
        try {
            matches = fu.contentEquals(file1, file2);
        } catch (IOException ioe) {
            throw new BuildException("when comparing files: "
                + ioe.getMessage(), ioe);
        }
        return matches;
    }
}

