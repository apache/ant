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
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.io.FilenameFilter;

/**
 * A filename filter for inner class files of a particular class.
 */
public class InnerClassFilenameFilter implements FilenameFilter {
    private String baseClassName;

    /**
     * Constructor of filter.
     * @param baseclass the class to filter inner classes on.
     */
    InnerClassFilenameFilter(String baseclass) {
        int extidx = baseclass.lastIndexOf(".class");
        if (extidx == -1) {
            extidx = baseclass.length() - 1;
        }
        baseClassName = baseclass.substring(0, extidx);
    }

    /**
     * Check if the file name passes the filter.
     * @param dir not used.
     * @param filename the filename to filter on.
     * @return true if the filename is an inner class of the base class.
     */
    @Override
    public boolean accept(File dir, String filename) {
        return filename.lastIndexOf('.') == filename.lastIndexOf(".class")
            && filename.indexOf(baseClassName + "$") == 0;
    }
}
