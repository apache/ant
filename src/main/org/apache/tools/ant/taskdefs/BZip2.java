/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
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


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.bzip2.CBZip2OutputStream;

/**
 * Compresses a file with the BZIP2 algorithm. Normally used to compress
 * non-compressed archives such as TAR files.
 *
 *
 * @since Ant 1.5
 *
 * @ant.task category="packaging"
 */

public class BZip2 extends Pack {
    protected void pack() {
        CBZip2OutputStream zOut = null;
        try {
            BufferedOutputStream bos =
                new BufferedOutputStream(new FileOutputStream(zipFile));
            bos.write('B');
            bos.write('Z');
            zOut = new CBZip2OutputStream(bos);
            zipFile(source, zOut);
        } catch (IOException ioe) {
            String msg = "Problem creating bzip2 " + ioe.getMessage();
            throw new BuildException(msg, ioe, getLocation());
        } finally {
            if (zOut != null) {
                try {
                    // close up
                    zOut.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }
}
