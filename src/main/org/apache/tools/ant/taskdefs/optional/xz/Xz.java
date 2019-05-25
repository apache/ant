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

package org.apache.tools.ant.taskdefs.optional.xz;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Pack;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 * Compresses a file with the XZ algorithm. Normally used to compress
 * non-compressed archives such as TAR files.
 *
 * @since Ant 1.10.1
 *
 * @ant.task category="packaging"
 */

public class Xz extends Pack {
    /**
     * Compress the zipFile.
     */
    @Override
    protected void pack() {
        try (XZOutputStream zOut = new XZOutputStream(
            Files.newOutputStream(zipFile.toPath()), new LZMA2Options())) {
            zipResource(getSrcResource(), zOut);
        } catch (IOException ioe) {
            String msg = "Problem creating xz " + ioe.getMessage();
            throw new BuildException(msg, ioe, getLocation());
        }
    }

    /**
     * Whether this task can deal with non-file resources.
     *
     * <p>This implementation always returns true only.</p>
     * @return true
     */
    @Override
    protected boolean supportsNonFileResources() {
        return true;
    }
}
