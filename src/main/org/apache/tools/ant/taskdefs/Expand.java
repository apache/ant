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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Vector;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * Unzip a file.
 *
 * @author costin@dnt.ro
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class Expand extends MatchingTask {
    protected File dest;
    protected File source; // req
    protected File outFile;
    protected boolean overwrite = true;
    protected boolean verbose;
    protected PrintWriter pw = null;
    protected BufferedWriter bw = null;
    protected FileWriter fw = null;
    protected Vector patternsets = new Vector();
    protected Vector filesets = new Vector();

    /**
     * Do the work.
     *
     * @exception BuildException Thrown in unrecoverable error.
     */
    // XXX move it to util or tools
    public void execute() throws BuildException {
        if ("expand".equals(taskType)) {
            log("!! expand is deprecated. Use unzip instead. !!");
        }

        if (source == null && filesets.size() == 0) {
            throw new BuildException("src attribute and/or filesets must be specified");
        }

        if (dest == null && outFile == null) {
            throw new BuildException(
                "Dest and/or the OutFile attribute " +
                "must be specified");
        }

        if (dest != null && dest.exists() && !dest.isDirectory()) {
            throw new BuildException("Dest must be a directory.", location);
        }

        if (verbose && outFile == null) {
            throw new BuildException(
                "Verbose can be set only when OutFile is " +
                "specified");
        }

        Touch touch = (Touch) project.createTask("touch");
        touch.setOwningTarget(target);
        touch.setTaskName(getTaskName());
        touch.setLocation(getLocation());

        try {
            if (outFile != null) {
                if (outFile.isDirectory()) {
                    throw new BuildException("Outfile " + outFile
                        + " must not be a directory.");
                }
                if (!outFile.exists()) {
                    File parent = new File(outFile.getParent());
                    if (!parent.exists()) {
                        if (!parent.mkdirs()) {
                            throw new BuildException("Unable to create "
                                + outFile);
                        }
                    }
                }
                fw = new FileWriter(outFile);
                bw = new BufferedWriter(fw);
                pw = new PrintWriter(bw, true);
            }
        } catch (IOException ioe) {
            throw new BuildException(ioe.getMessage(), location);
        }
        if (source != null) {
            if (source.isDirectory()) {
                // get all the files in the descriptor directory
                DirectoryScanner ds = super.getDirectoryScanner(source);

                String[] files = ds.getIncludedFiles();
                for (int i = 0; i < files.length; ++i) {
                    File file = new File(source, files[i]);
                    expandFile(touch, file, dest);
                }
            }
            else {
                expandFile(touch, source, dest);
            }
        }
        if (filesets.size() > 0) {
            for (int j=0; j < filesets.size(); j++) {
                FileSet fs = (FileSet) filesets.elementAt(j);
                DirectoryScanner ds = fs.getDirectoryScanner(project);
                File fromDir = fs.getDir(project);

                String[] files = ds.getIncludedFiles();
                for (int i = 0; i < files.length; ++i) {
                    File file = new File(fromDir, files[i]);
                    expandFile(touch, file, dest);
                }
            }
        }
        try {
            if (pw != null) {
                pw.close();
            }
            if (bw != null) {
                bw.close();
            }
            if (fw != null) {
                fw.close();
            }
        } catch (IOException ioe1) {
            //Oh, well!  We did our best
        }
    }

    /*
     * This method is to be overridden by extending unarchival tasks.
     */
    protected void expandFile(Touch touch, File srcF, File dir) {
        ZipInputStream zis = null;
        try {
            // code from WarExpand
            zis = new ZipInputStream(new FileInputStream(srcF));
            ZipEntry ze = null;

            while ((ze = zis.getNextEntry()) != null) {
                extractFile(touch, srcF, dir, zis,
                            ze.getName(), ze.getSize(),
                            new Date(ze.getTime()),
                            ze.isDirectory());
            }

            if (dest != null) {
                log("expand complete", Project.MSG_VERBOSE );
            }

        } catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcF.getPath(), ioe);
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                }
                catch (IOException e) {}
            }
        }
    }

    protected void extractFile(Touch touch, File srcF, File dir,
                               InputStream compressedInputStream,
                               String entryName, long entrySize,
                               Date entryDate, boolean isDirectory)
                               throws IOException {
        extractFile(touch, srcF, dir, compressedInputStream,
                    entryName, entrySize, entryDate, isDirectory,
                    null, null);

    }

    protected void extractFile(Touch touch, File srcF, File dir,
                               InputStream compressedInputStream,
                               String entryName, long entrySize,
                               Date entryDate, boolean isDirectory,
                               String modeStr, String userGroup)
                               throws IOException {

        if (patternsets != null && patternsets.size() > 0) {
            String name = entryName;
            boolean included = false;
            for (int v = 0; v < patternsets.size(); v++) {
                PatternSet p = (PatternSet) patternsets.elementAt(v);
                String[] incls = p.getIncludePatterns(project);
                if (incls != null) {
                    for (int w = 0; w < incls.length; w++) {
                        boolean isIncl = DirectoryScanner.match(incls[w], name);
                        if (isIncl) {
                            included = true;
                            break;
                        }
                    }
                }
                String[] excls = p.getExcludePatterns(project);
                if (excls != null) {
                    for (int w = 0; w < excls.length; w++) {
                        boolean isExcl = DirectoryScanner.match(excls[w], name);
                        if (isExcl) {
                            included = false;
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

        if (dest != null) {
            log("Expanding: " + srcF + " into " + dir, Project.MSG_INFO);
        }

        if (outFile != null) {
            if (verbose) {
                StringBuffer sb = new StringBuffer();
                if (modeStr != null) {
                    sb.append(modeStr);
                    sb.append(' ');
                }
                if (userGroup != null) {
                    sb.append(userGroup);
                    sb.append(' ');
                }
                String s = Long.toString(entrySize);
                int len = s.length();
                for(int i = 6 - len; i > 0; i--) {
                    sb.append(' ');
                }
                sb.append(s)
                  .append(' ')
                  .append(entryDate.toString());
                sb.append(' ')
                  .append(entryName);
                pw.println(sb);
            } else {
                pw.println(entryName);
            }
        }
        if (dest != null) {
            File f = new File(dir, project.translatePath(entryName));
            try {
                if (!overwrite && f.exists()
                    && f.lastModified() >= entryDate.getTime()) {
                    log("Skipping " + f + " as it is up-to-date",
                        Project.MSG_DEBUG);
                    return;
                }

                log("expanding " + entryName + " to "+ f,
                    Project.MSG_VERBOSE);
                // create intermediary directories - sometimes zip don't add them
                File dirF=new File(f.getParent());
                dirF.mkdirs();

                if (isDirectory) {
                    f.mkdirs();
                } else {
                    byte[] buffer = new byte[1024];
                    int length = 0;
                    FileOutputStream fos = new FileOutputStream(f);

                    while ((length =
                                compressedInputStream.read(buffer)) >= 0) {
                        fos.write(buffer, 0, length);
                    }

                    fos.close();
                }

                if (project.getJavaVersion() != Project.JAVA_1_1) {
                    touch.setFile(f);
                    touch.setMillis(entryDate.getTime());
                    touch.touch();
                }

            } catch( FileNotFoundException ex ) {
                log("Unable to expand to file " + f.getPath(), Project.MSG_WARN);
            }
        }

    }

    /**
     * Set the destination directory. File will be unzipped into the
     * destination directory.
     *
     * @param d Path to the directory.
     */
    public void setDest(File d) {
        this.dest=d;
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
     * Set the output file to be used to store the list of the
     * archive's contents.
     *
     * @param outFile the output file to be used.
     */
    public void setOutfile(File outFile) {
        this.outFile = outFile;
    }

    /**
     * Set the verbose mode for the contents-list file.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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
