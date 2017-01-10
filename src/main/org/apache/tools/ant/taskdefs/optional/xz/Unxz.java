/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.optional.xz;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Unpack;
import org.apache.tools.ant.util.FileUtils;
import org.tukaani.xz.XZInputStream;

/**
 * Expands a file that has been compressed with the XZ
 * algorithm. Normally used to compress non-compressed archives such
 * as TAR files.
 *
 * @since Ant 1.10.1
 *
 * @ant.task category="packaging"
 */

public class Unxz extends Unpack {
    private static final int BUFFER_SIZE = 8 * 1024;
    private static final String DEFAULT_EXTENSION = ".xz";

    /**
     * Get the default extension.
     * @return the value ".xz"
     */
    protected String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    /**
     * Implement the gunzipping.
     */
    protected void extract() {
        if (srcResource.getLastModified() > dest.lastModified()) {
            log("Expanding " + srcResource.getName() + " to "
                        + dest.getAbsolutePath());

            FileOutputStream out = null;
            XZInputStream zIn = null;
            InputStream fis = null;
            try {
                out = new FileOutputStream(dest);
                fis = srcResource.getInputStream();
                zIn = new XZInputStream(fis);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count = 0;
                do {
                    out.write(buffer, 0, count);
                    count = zIn.read(buffer, 0, buffer.length);
                } while (count != -1);
            } catch (IOException ioe) {
                String msg = "Problem expanding xz " + ioe.getMessage();
                throw new BuildException(msg, ioe, getLocation());
            } finally {
                FileUtils.close(fis);
                FileUtils.close(out);
                FileUtils.close(zIn);
            }
        }
    }

    /**
     * Whether this task can deal with non-file resources.
     *
     * <p>This implementation returns true only.</p>
     * @return true
     */
    protected boolean supportsNonFileResources() {
        return true;
    }
}
