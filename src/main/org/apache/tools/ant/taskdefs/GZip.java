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

package org.apache.tools.ant.taskdefs;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.ant.BuildException;

/**
 * Compresses a file with the GZIP algorithm. Normally used to compress
 * non-compressed archives such as TAR files.
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */

public class GZip extends Pack {
    /**
     * perform the GZip compression operation.
     */
    @Override
    protected void pack() {
        try (GZIPOutputStream zOut =
            new GZIPOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            zipResource(getSrcResource(), zOut);
        } catch (IOException ioe) {
            String msg = "Problem creating gzip " + ioe.getMessage();
            throw new BuildException(msg, ioe, getLocation());
        }
    }

    /**
     * Whether this task can deal with non-file resources.
     *
     * <p>This implementation returns true only if this task is
     * &lt;gzip&gt;.  Any subclass of this class that also wants to
     * support non-file resources needs to override this method.  We
     * need to do so for backwards compatibility reasons since we
     * can't expect subclasses to support resources.</p>
     * @return true if this case supports non file resources.
     * @since Ant 1.7
     */
    @Override
    protected boolean supportsNonFileResources() {
        return getClass().equals(GZip.class);
    }
}
