/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.util.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * Unzip a file.
 *
 * @author costin@dnt.ro
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author Magesh Umasankar
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 *           name="unzip"
 *           name="unjar"
 *           name="unwar"
 */
public class Expand extends Task {
    private File dest; //req
    private File source; // req
    private boolean overwrite = true;
    private Vector patternsets = new Vector();
    private Vector filesets = new Vector();

    /**
     * Do the work.
     *
     * @exception BuildException Thrown in unrecoverable error.
     */
    public void execute() throws BuildException {
        if ("expand".equals(taskType)) {
            log("!! expand is deprecated. Use unzip instead. !!");
        }

        if (source == null && filesets.size() == 0) {
            throw new BuildException("src attribute and/or filesets must be "
                                     + "specified");
        }

        if (dest == null) {
            throw new BuildException(
                "Dest attribute must be specified");
        }

        if (dest.exists() && !dest.isDirectory()) {
            throw new BuildException("Dest must be a directory.", location);
        }

        FileUtils fileUtils = FileUtils.newFileUtils();

        if (source != null) {
            if (source.isDirectory()) {
                throw new BuildException("Src must not be a directory." +
                    " Use nested filesets instead.", location);
            } else {
                expandFile(fileUtils, source, dest);
            }
        }
        if (filesets.size() > 0) {
            for (int j = 0; j < filesets.size(); j++) {
                FileSet fs = (FileSet) filesets.elementAt(j);
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                File fromDir = fs.getDir(getProject());

                String[] files = ds.getIncludedFiles();
                for (int i = 0; i < files.length; ++i) {
                    File file = new File(fromDir, files[i]);
                    expandFile(fileUtils, file, dest);
                }
            }
        }
    }

    /*
     * This method is to be overridden by extending unarchival tasks.
     */
    protected void expandFile(FileUtils fileUtils, File srcF, File dir) {
        log("Expanding: " + srcF + " into " + dir, Project.MSG_INFO);
        ZipInputStream zis = null;
        try {
            // code from WarExpand
            zis = new ZipInputStream(new FileInputStream(srcF));
            ZipEntry ze = null;

            while ((ze = zis.getNextEntry()) != null) {
                extractFile(fileUtils, srcF, dir, zis,
                            ze.getName(), new Date(ze.getTime()),
                            ze.isDirectory());
            }

            log("expand complete", Project.MSG_VERBOSE);
        } catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcF.getPath(),
                                     ioe);
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {}
            }
        }
    }

    protected void extractFile(FileUtils fileUtils, File srcF, File dir,
                               InputStream compressedInputStream,
                               String entryName,
                               Date entryDate, boolean isDirectory)
                               throws IOException {

        if (patternsets != null && patternsets.size() > 0) {
            String name = entryName;
            boolean included = false;
            for (int v = 0; v < patternsets.size(); v++) {
                PatternSet p = (PatternSet) patternsets.elementAt(v);
                String[] incls = p.getIncludePatterns(getProject());
                if (incls == null || incls.length == 0) {
                    // no include pattern implicitly means includes="**"
                    incls = new String[] {"**"};
                }
                    
                for (int w = 0; w < incls.length; w++) {
                    included = DirectoryScanner.match(incls[w], name);
                    if (included) {
                        break;
                    }
                }
                
                if (!included) {
                    break;
                }
                

                String[] excls = p.getExcludePatterns(getProject());
                if (excls != null) {
                    for (int w = 0; w < excls.length; w++) {
                        included = !(DirectoryScanner.match(excls[w], name));
                        if (!included) {
                            break;
                        }
                    }
                }
            }
            if (!included) {
                //Do not process this file
                return;
            }
        }

        File f = fileUtils.resolveFile(dir, entryName);
        try {
            if (!overwrite && f.exists()
                && f.lastModified() >= entryDate.getTime()) {
                log("Skipping " + f + " as it is up-to-date",
                    Project.MSG_DEBUG);
                return;
            }

            log("expanding " + entryName + " to " + f,
                Project.MSG_VERBOSE);
            // create intermediary directories - sometimes zip don't add them
            File dirF = fileUtils.getParentFile(f);
            if ( dirF != null ) {
                dirF.mkdirs();
            }

            if (isDirectory) {
                f.mkdirs();
            } else {
                byte[] buffer = new byte[1024];
                int length = 0;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);

                    while ((length =
                            compressedInputStream.read(buffer)) >= 0) {
                        fos.write(buffer, 0, length);
                    }

                    fos.close();
                    fos = null;
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {}
                    }
                }
            }

            fileUtils.setFileLastModified(f, entryDate.getTime());
        } catch (FileNotFoundException ex) {
            log("Unable to expand to file " + f.getPath(), Project.MSG_WARN);
        }

    }

    /**
     * Set the destination directory. File will be unzipped into the
     * destination directory.
     *
     * @param d Path to the directory.
     */
    public void setDest(File d) {
        this.dest = d;
    }

    /**
     * Set the path to zip-file.
     *
     * @param s Path to zip-file.
     */
    public void setSrc(File s) {
        this.source = s;
    }

    /**
     * Should we overwrite files in dest, even if they are newer than
     * the corresponding entries in the archive?
     */
    public void setOverwrite(boolean b) {
        overwrite = b;
    }

    /**
     * Add a patternset
     */
    public void addPatternset(PatternSet set) {
        patternsets.addElement(set);
    }

    /**
     * Add a fileset
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

}
