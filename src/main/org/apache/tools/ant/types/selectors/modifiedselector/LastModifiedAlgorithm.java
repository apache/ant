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

/**
 * Computes a 'timestamp' of file based on the lastModified time of that file.
 *
 * @version 2018-07-25
 * @since  Ant 1.10.6 and 1.9.14
 */
public class LastModifiedAlgorithm implements Algorithm {

    /**
     * This algorithm doesn't need any configuration.
     * Therefore it's always valid.
     * @return always true
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Computes a 'timestamp' for a file based on the lastModified time.
     * @param file  The file for which the value should be computed
     * @return the timestamp or <i>null</i> if the timestamp couldn't be computed
     */
    public String getValue(File file) {
        long lastModified = file.lastModified();
        if (lastModified == 0L) {
            return null;
        }

        return Long.toString(lastModified);
    }
}
