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

package org.apache.tools.ant.types;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.ZipResource;
import org.apache.tools.ant.util.StreamUtils;
import org.apache.tools.zip.ZipFile;

/**
 * Scans zip archives for resources.
 */
public class ZipScanner extends ArchiveScanner {

    /**
     * Fills the file and directory maps with resources read from the
     * archive.
     *
     * @param src the archive to scan.
     * @param encoding encoding used to encode file names inside the archive.
     * @param fileEntries Map (name to resource) of non-directory
     * resources found inside the archive.
     * @param matchFileEntries Map (name to resource) of non-directory
     * resources found inside the archive that matched all include
     * patterns and didn't match any exclude patterns.
     * @param dirEntries Map (name to resource) of directory
     * resources found inside the archive.
     * @param matchDirEntries Map (name to resource) of directory
     * resources found inside the archive that matched all include
     * patterns and didn't match any exclude patterns.
     */
    @Override
    protected void fillMapsFromArchive(Resource src, String encoding,
            Map<String, Resource> fileEntries, Map<String, Resource> matchFileEntries,
            Map<String, Resource> dirEntries, Map<String, Resource> matchDirEntries) {

        File srcFile = src.asOptional(FileProvider.class)
            .map(FileProvider::getFile).orElseThrow(() -> new BuildException(
                "Only file provider resources are supported"));

        try (ZipFile zf = new ZipFile(srcFile, encoding)) {
            StreamUtils.enumerationAsStream(zf.getEntries()).forEach(entry -> {
                Resource r = new ZipResource(srcFile, encoding, entry);
                String name = entry.getName();
                if (entry.isDirectory()) {
                    name = trimSeparator(name);
                    dirEntries.put(name, r);
                    if (match(name)) {
                        matchDirEntries.put(name, r);
                    }
                } else {
                    fileEntries.put(name, r);
                    if (match(name)) {
                        matchFileEntries.put(name, r);
                    }
                }
            });
        } catch (ZipException ex) {
            throw new BuildException("Problem reading " + srcFile, ex);
        } catch (IOException ex) {
            throw new BuildException("Problem opening " + srcFile, ex);
        }
    }
}
