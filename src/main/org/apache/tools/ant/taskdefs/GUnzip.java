/*
 * Copyright  2000-2002,2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.apache.tools.ant.BuildException;

/**
 * Expands a file that has been compressed with the GZIP
 * algorithm. Normally used to compress non-compressed archives such
 * as TAR files.
 *
 * @author Stefan Bodewig
 * @author Magesh Umasankar
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */

public class GUnzip extends Unpack {

    private static final String DEFAULT_EXTENSION = ".gz";

    protected String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    protected void extract() {
        if (source.lastModified() > dest.lastModified()) {
            log("Expanding " + source.getAbsolutePath() + " to "
                        + dest.getAbsolutePath());

            FileOutputStream out = null;
            GZIPInputStream zIn = null;
            FileInputStream fis = null;
            try {
                out = new FileOutputStream(dest);
                fis = new FileInputStream(source);
                zIn = new GZIPInputStream(fis);
                byte[] buffer = new byte[8 * 1024];
                int count = 0;
                do {
                    out.write(buffer, 0, count);
                    count = zIn.read(buffer, 0, buffer.length);
                } while (count != -1);
            } catch (IOException ioe) {
                String msg = "Problem expanding gzip " + ioe.getMessage();
                throw new BuildException(msg, ioe, getLocation());
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ioex) {
                        //ignore
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ioex) {
                        //ignore
                    }
                }
                if (zIn != null) {
                    try {
                        zIn.close();
                    } catch (IOException ioex) {
                        //ignore
                    }
                }
            }
        }
    }
}
