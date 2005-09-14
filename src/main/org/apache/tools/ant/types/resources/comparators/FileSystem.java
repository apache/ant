/*
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.tools.ant.types.resources.comparators;

import java.io.File;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;

/**
 * Compares filesystem Resources.
 * @since Ant 1.7
 */
public class FileSystem extends ResourceComparator {
    private static FileUtils fileUtils = FileUtils.getFileUtils();

    /**
     * Compare two Resources.
     * @param foo the first Resource.
     * @param bar the second Resource.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     * @throws ClassCastException if either resource is not an instance of FileResource.
     */
    protected int resourceCompare(Resource foo, Resource bar) {
        File foofile = ((FileResource) foo).getFile();
        File barfile = ((FileResource) bar).getFile();
        return foofile.equals(barfile) ? 0
            : fileUtils.isLeadingPath(foofile, barfile) ? -1
            : fileUtils.normalize(foofile.getAbsolutePath()).compareTo(
                fileUtils.normalize(barfile.getAbsolutePath()));
    }

}
