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

/**
 * Selector that filters files based on whether they are newer than
 * a matching file in another directory tree. It can contain a mapper
 * element, so isn't available as an ExtendSelector (since those
 * parameters can't hold other elements).
 *
 * @since 1.5
 */
public class DependSelector extends MappingSelector {

    /**
     * @return a string describing this object
     */
    public String toString() {
        StringBuilder buf = new StringBuilder("{dependselector targetdir: ");
        if (targetdir == null) {
            buf.append("NOT YET SET");
        } else {
            buf.append(targetdir.getName());
        }
        buf.append(" granularity: ").append(granularity);
        if (map != null) {
            buf.append(" mapper: ");
            buf.append(map.toString());
        } else if (mapperElement != null) {
            buf.append(" mapper: ");
            buf.append(mapperElement.toString());
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * this test is our selection test that compared the file with the destfile
     * @param srcfile the source file
     * @param destfile the destination file
     * @return true if destination is out of date
     */
    public boolean selectionTest(File srcfile, File destfile) {
        return SelectorUtils.isOutOfDate(srcfile, destfile, granularity);
    }

}

