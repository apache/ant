/*
 * Copyright  2001-2002,2004-2005 The Apache Software Foundation
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


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.bzip2.CBZip2InputStream;

/**
 * Expands a file that has been compressed with the BZIP2
 * algorithm. Normally used to compress non-compressed archives such
 * as TAR files.
 *
 * @since Ant 1.5
 *
 * @ant.task category="packaging"
 */

public class BUnzip2 extends Unpack {

    private static final String DEFAULT_EXTENSION = ".bz2";

    /**
     * Get the default extension.
     * @return the string ".bz2"
     */
    protected String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    /**
     * Do the unbzipping.
     */
    protected void extract() {
        if (source.lastModified() > dest.lastModified()) {
            log("Expanding " + source.getAbsolutePath() + " to "
                + dest.getAbsolutePath());

            FileOutputStream out = null;
            CBZip2InputStream zIn = null;
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                out = new FileOutputStream(dest);
                fis = new FileInputStream(source);
                bis = new BufferedInputStream(fis);
                int b = bis.read();
                if (b != 'B') {
                    throw new BuildException("Invalid bz2 file.", getLocation());
                }
                b = bis.read();
                if (b != 'Z') {
                    throw new BuildException("Invalid bz2 file.", getLocation());
                }
                zIn = new CBZip2InputStream(bis);
                byte[] buffer = new byte[8 * 1024];
                int count = 0;
                do {
                    out.write(buffer, 0, count);
                    count = zIn.read(buffer, 0, buffer.length);
                } while (count != -1);
            } catch (IOException ioe) {
                String msg = "Problem expanding bzip2 " + ioe.getMessage();
                throw new BuildException(msg, ioe, getLocation());
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException ioex) {
                        // ignore
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ioex) {
                        // ignore
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ioex) {
                        // ignore
                    }
                }
                if (zIn != null) {
                    try {
                        zIn.close();
                    } catch (IOException ioex) {
                        // ignore
                    }
                }
            }
        }
    }
}
