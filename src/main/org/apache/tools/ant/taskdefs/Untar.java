/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Untar a file.
 *
 * Heavily based on the Expand task.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class Untar extends Expand {
    private final static int S_IFMT   = 0170000;
    private final static int S_IFSOCK = 0140000;
    private final static int S_IFLNK  = 0120000;
    private final static int S_IFREG  = 0100000;
    private final static int S_IFBLK  = 0060000;
    private final static int S_IFDIR  = 0040000;
    private final static int S_IFCHR  = 0020000;
    private final static int S_IFIFO  = 0010000;
    private final static int S_ISUID  = 0004000;
    private final static int S_ISGID  = 0002000;
    private final static int S_ISVTX  = 0001000;
    private final static int S_IRWXU  = 00700;
    private final static int S_IRUSR  = 00400;
    private final static int S_IWUSR  = 00200;
    private final static int S_IXUSR  = 00100;
    private final static int S_IRWXG  = 00070;
    private final static int S_IRGRP  = 00040;
    private final static int S_IWGRP  = 00020;
    private final static int S_IXGRP  = 00010;
    private final static int S_IRWXO  = 00007;
    private final static int S_IROTH  = 00004;
    private final static int S_IWOTH  = 00002;
    private final static int S_IXOTH  = 00001;

    protected void expandFile(Touch touch, File srcF, File dir) {
        TarInputStream tis = null;
        try {
            if (dest != null) {
                log("Expanding: " + srcF + " into " + dir, Project.MSG_INFO);
            }

            tis = new TarInputStream(new FileInputStream(srcF));
            TarEntry te = null;

            while ((te = tis.getNextEntry()) != null) {
                extractFile(touch, srcF, dir, tis,
                            te.getName(), te.getSize(),
                            te.getModTime(), te.isDirectory(),
                            mode2str(te.getMode()),
                            te.getUserId() + "/" + te.getGroupId());
            }
            if (dest != null) {
                log("expand complete", Project.MSG_VERBOSE );
            }

        } catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcF.getPath(),
                                     ioe, location);
        } finally {
            if (tis != null) {
                try {
                    tis.close();
                }
                catch (IOException e) {}
            }
        }
    }

    private String mode2str(int mode) {
        StringBuffer sb = new StringBuffer("----------");
        if ((mode & S_IFREG ) == 0) {
            if ((mode & S_IFDIR ) != 0) {
                sb.setCharAt(0, 'd');
            } else if ((mode & S_IFLNK)  != 0) {
                sb.setCharAt(0, 'l');
            } else if ((mode & S_IFIFO)  != 0) {
                sb.setCharAt(0, 'p');
            } else if ((mode & S_IFCHR)  != 0) {
                sb.setCharAt(0, 'c');
            } else if ((mode & S_IFBLK)  != 0) {
                sb.setCharAt(0, 'b');
            } else if ((mode & S_IFSOCK) != 0) {
                sb.setCharAt(0, 's');
            } else if ((mode & S_IFIFO)  != 0) {
                sb.setCharAt(0, 'p');
            }
        }

        if ((mode & S_IRUSR ) != 0) {
            sb.setCharAt(1, 'r');
        }
        if ((mode & S_IWUSR ) != 0) {
            sb.setCharAt(2, 'w');
        }
        if ((mode & S_IXUSR ) != 0) {
            sb.setCharAt(3, 'x');
        }

        if ((mode & S_IRGRP ) != 0) {
            sb.setCharAt(4, 'r');
        }
        if ((mode & S_IWGRP ) != 0) {
            sb.setCharAt(5, 'w');
        }
        if ((mode & S_IXGRP ) != 0) {
            sb.setCharAt(6, 'x');
        }

        if ((mode & S_IROTH ) != 0) {
            sb.setCharAt(7, 'r');
        }
        if ((mode & S_IWOTH ) != 0) {
            sb.setCharAt(8, 'w');
        }
        if ((mode & S_IXOTH ) != 0) {
            sb.setCharAt(9, 'x');
        }
        return new String(sb);
    }
}
