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
package org.apache.tools.ant;


import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assume.assumeTrue;

public class FileUtilities {

    /**
     * Reads the contents of a file into a String.
     * @param project the project containing the base directory the file is in.
     * @param fileName the path to the file from the base directory.
     * @return the contents of the given file as a string.
     * @throws IOException on error reading the file (not existing, not readable etc)
     */
    public static String getFileContents(Project project, String fileName) throws IOException {
        return getFileContents(new File(project.getBaseDir(), fileName));
    }

    /**
     * Reads the contents of a file into a String.
     * @param file the file to read.
     * @return the contents of the given file as a string.
     * @throws IOException on error reading the file (not existing, not readable etc)
     */
    public static String getFileContents(File file) throws IOException {
        try (FileReader rdr = new FileReader(file)) {
            return FileUtils.readFully(rdr);
        }
    }

    /**
     * Modified the timestamp on a file so it's <code>seconds</code> earlier than it was before. Where <code>file</code>
     * is a directory, this function recurses into all child files (and directories) and reduces their modified
     * timestamps by the same range, rather than set all timestamps to the same time.
     * @param file the file to change, or the directory to change then recurse into
     * @param seconds how many seconds to roll the timestamp back by
     */
    public static void rollbackTimestamps(File file, long seconds) {
        if (null == file || !file.exists()) {
            return;
        }

        assumeTrue(file.setLastModified(file.lastModified() - seconds * 1000));

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            // only possible if exception occurs, or abstract path was not valid
            if (children == null) {
                return;
            }
            Arrays.stream(children).forEach(child -> rollbackTimestamps(child, seconds));
        }
    }

}
