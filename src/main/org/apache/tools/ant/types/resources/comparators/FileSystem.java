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
package org.apache.tools.ant.types.resources.comparators;

import java.io.File;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.util.FileUtils;

/**
 * Compares filesystem Resources.
 * @since Ant 1.7
 */
public class FileSystem extends ResourceComparator {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Compare two Resources.
     * @param foo the first Resource.
     * @param bar the second Resource.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     * @throws ClassCastException if either resource is not capable of
     *         exposing a {@link FileProvider}
     */
    protected int resourceCompare(Resource foo, Resource bar) {
        return compare(file(foo), file(bar));
    }

    private File file(Resource r) {
        return r.asOptional(FileProvider.class)
            .orElseThrow(() -> new ClassCastException(
                r.getClass() + " doesn't provide files"))
            .getFile();
    }

    private int compare(File f1, File f2) {
        if (Objects.equals(f1, f2)) {
            return 0;
        }
        if (FILE_UTILS.isLeadingPath(f1, f2)) {
            return -1;
        }
        if (FILE_UTILS.isLeadingPath(f2, f1)) {
            return 1;
        }
        return Comparator
            .comparing(((Function<File, String>) File::getAbsolutePath)
                .andThen(FILE_UTILS::normalize))
            .compare(f1, f2);
    }
}
