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
import java.util.stream.Stream;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * Utility class that collects the functionality of the various
 * scanDir methods that have been scattered in several tasks before.
 *
 * <p>The only method returns an array of source files. The array is a
 * subset of the files given as a parameter and holds only those that
 * are newer than their corresponding target files.</p>
 *
 */
public class SourceFileScanner implements ResourceFactory {

    // CheckStyle:VisibilityModifier OFF - bc
    protected Task task;
    // CheckStyle:VisibilityModifier ON

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private File destDir;     // base directory of the fileset

    /**
     * Construct a new SourceFileScanner.
     * @param task The task we should log messages through.
     */
    public SourceFileScanner(Task task) {
        this.task = task;
    }

    /**
     * Restrict the given set of files to those that are newer than
     * their corresponding target files.
     *
     * @param files   the original set of files.
     * @param srcDir  all files are relative to this directory.
     * @param destDir target files live here. if null file names
     *                returned by the mapper are assumed to be absolute.
     * @param mapper  knows how to construct a target file names from
     *                source file names.
     * @return an array of filenames.
     */
    public String[] restrict(String[] files, File srcDir, File destDir,
                             FileNameMapper mapper) {
        return restrict(files, srcDir, destDir, mapper,
                        FILE_UTILS.getFileTimestampGranularity());
    }

    /**
     * Restrict the given set of files to those that are newer than
     * their corresponding target files.
     *
     * @param files   the original set of files.
     * @param srcDir  all files are relative to this directory.
     * @param destDir target files live here. If null file names
     *                returned by the mapper are assumed to be absolute.
     * @param mapper  knows how to construct a target file names from
     *                source file names.
     * @param granularity The number of milliseconds leeway to give
     *                    before deciding a target is out of date.
     * @return an array of filenames.
     *
     * @since Ant 1.6.2
     */
    public String[] restrict(String[] files, File srcDir, File destDir,
                             FileNameMapper mapper, long granularity) {
        // record destdir for later use in getResource
        this.destDir = destDir;

        Resource[] sourceResources =
            Stream.of(files).map(f -> new FileResource(srcDir, f) {
                @Override
                public String getName() {
                    return f;
                }
            }).toArray(Resource[]::new);

        // build the list of sources which are out of date with
        // respect to the target
        return Stream
            .of(ResourceUtils.selectOutOfDateSources(task, sourceResources,
                mapper, this, granularity))
            .map(Resource::getName).toArray(String[]::new);
    }

    /**
     * Convenience layer on top of restrict that returns the source
     * files as File objects (containing absolute paths if srcDir is
     * absolute).
     * @param files   the original set of files.
     * @param srcDir  all files are relative to this directory.
     * @param destDir target files live here. If null file names
     *                returned by the mapper are assumed to be absolute.
     * @param mapper  knows how to construct a target file names from
     *                source file names.
     * @return an array of files.
     */
    public File[] restrictAsFiles(String[] files, File srcDir, File destDir,
                                  FileNameMapper mapper) {
        return restrictAsFiles(files, srcDir, destDir, mapper,
                               FILE_UTILS.getFileTimestampGranularity());
    }

    /**
     * Convenience layer on top of restrict that returns the source
     * files as File objects (containing absolute paths if srcDir is
     * absolute).
     *
     * @param files   the original set of files.
     * @param srcDir  all files are relative to this directory.
     * @param destDir target files live here. If null file names
     *                returned by the mapper are assumed to be absolute.
     * @param mapper  knows how to construct a target file names from
     *                source file names.
     * @param granularity The number of milliseconds leeway to give
     *                    before deciding a target is out of date.
     * @return an array of files.
     * @since Ant 1.6.2
     */
    public File[] restrictAsFiles(String[] files, File srcDir, File destDir,
                                  FileNameMapper mapper, long granularity) {
        return Stream.of(restrict(files, srcDir, destDir, mapper, granularity))
            .map(name -> new File(srcDir, name)).toArray(File[]::new);
    }

    /**
     * Returns resource information for a file at destination.
     * @param name relative path of file at destination.
     * @return data concerning a file whose relative path to destDir is name.
     *
     * @since Ant 1.5.2
     */
    @Override
    public Resource getResource(String name) {
        return new FileResource(destDir, name);
    }

}

