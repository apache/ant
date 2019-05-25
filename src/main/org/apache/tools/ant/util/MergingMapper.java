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

package org.apache.tools.ant.util;

/**
 * Implementation of FileNameMapper that always returns the same
 * target file name.
 *
 * <p>This is the default FileNameMapper for the archiving tasks and
 * uptodate.</p>
 *
 */
public class MergingMapper implements FileNameMapper {
    // CheckStyle:VisibilityModifier OFF - bc
    protected String[] mergedFile = null;
    // CheckStyle:VisibilityModifier ON

    public MergingMapper() {
    }

    /**
     * @param to String
     * @since Ant 1.8.0
     */
    public MergingMapper(String to) {
        setTo(to);
    }

    /**
     * Ignored.
     * @param from ignored.
     */
    @Override
    public void setFrom(String from) {
    }

    /**
     * Sets the name of the merged file.
     * @param to the name of the merged file.
     */
    @Override
    public void setTo(String to) {
        mergedFile = new String[] {to};
    }

    /**
     * Returns an one-element array containing the file name set via setTo.
     * @param sourceFileName ignored.
     * @return a one-element array containing the merged filename.
     */
    @Override
    public String[] mapFileName(String sourceFileName) {
        return mergedFile;
    }

}
