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
package org.apache.tools.ant.types.resources;

import java.util.Iterator;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;

/**
 * Utility FileSet that includes directories for backwards-compatibility
 * with certain tasks e.g. Delete.
 * @since Ant 1.7
 */
public class BCFileSet extends FileSet {
    /**
     * Default constructor.
     */
    public BCFileSet() {
    }

    /**
     * Construct a new BCFileSet from the specified FileSet.
     * @param fs the FileSet from which to inherit config.
     */
    public BCFileSet(FileSet fs) {
        super(fs);
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return an Iterator of Resources.
     * @since Ant 1.7
     */
    @Override
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        FileResourceIterator result = new FileResourceIterator(getProject(), getDir());
        result.addFiles(getDirectoryScanner().getIncludedFiles());
        result.addFiles(getDirectoryScanner().getIncludedDirectories());
        return result;
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     * @since Ant 1.7
     */
    @Override
    public int size() {
        if (isReference()) {
            return getRef().size();
        }
        return getDirectoryScanner().getIncludedFilesCount()
            + getDirectoryScanner().getIncludedDirsCount();
    }

    private FileSet getRef() {
        return getCheckedRef(FileSet.class);
    }

}
