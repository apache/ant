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

import java.io.File;

/**
 * Implementation of FileNameMapper that always returns the source
 * file name without any leading directory information.
 *
 * <p>This is the default FileNameMapper for the copy and move
 * tasks if the flatten attribute has been set.</p>
 *
 */
public class FlatFileNameMapper implements FileNameMapper {

    /**
     * Ignored.
     * @param from ignored.
     */
    @Override
    public void setFrom(String from) {
    }

    /**
     * Ignored.
     * @param to ignored.
     */
    @Override
    public void setTo(String to) {
    }

    /**
     * Returns an one-element array containing the source file name
     * without any leading directory information.
     * @param sourceFileName the name to map.
     * @return the file name in a one-element array.
     */
    @Override
    public String[] mapFileName(String sourceFileName) {
        return sourceFileName == null ? null
            : new String[] {new File(sourceFileName).getName()};
    }
}
