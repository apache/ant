/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
 * @author Magesh Umasankar
 *
 * @since Ant 1.5
 *
 * @ant.task category="packaging"
 */

public class BUnzip2 extends Unpack {

    private static final String DEFAULT_EXTENSION = ".bz2";

    protected String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

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
                    throw new BuildException("Invalid bz2 file.", location);
                }
                b = bis.read();
                if (b != 'Z') {
                    throw new BuildException("Invalid bz2 file.", location);
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
                throw new BuildException(msg, ioe, location);
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException ioex) {}
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ioex) {}
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ioex) {}
                }
                if (zIn != null) {
                    try {
                        zIn.close();
                    } catch (IOException ioex) {}
                }
            }
        }
    }
}
