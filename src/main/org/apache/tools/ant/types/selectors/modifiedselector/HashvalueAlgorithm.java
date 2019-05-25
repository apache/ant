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

package org.apache.tools.ant.types.selectors.modifiedselector;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import org.apache.tools.ant.util.FileUtils;

/**
 * Computes a 'hashvalue' for the content of file using String.hashValue().
 * Use of this algorithm doesn't require any additional nested &lt;param&gt;s and
 * doesn't support any.
 *
 * @version 2003-09-13
 * @since  Ant 1.6
 */
public class HashvalueAlgorithm implements Algorithm {

    /**
     * This algorithm doesn't need any configuration.
     * Therefore it's always valid.
     * @return always true
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Computes a 'hashvalue' for a file content.
     * It reads the content of a file, convert that to String and use the
     * String.hashCode() method.
     * @param file  The file for which the value should be computed
     * @return the hashvalue or <i>null</i> if the file couldn't be read
     */
     // Because the content is only read the file will not be damaged. I tested
     // with JPG, ZIP and PDF as binary files.
    public String getValue(File file) {
        if (!file.canRead()) {
            return null;
        }
        try (Reader r = new FileReader(file)) {
            int hash = FileUtils.readFully(r).hashCode();
            return Integer.toString(hash);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Override Object.toString().
     * @return information about this comparator
     */
    public String toString() {
        return "HashvalueAlgorithm";
    }

}
