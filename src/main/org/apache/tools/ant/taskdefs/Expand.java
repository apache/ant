/*
 * Copyright  2000-2004 The Apache Software Foundation
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

/**
 * Unzip a file.
 *
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

    private static final String NATIVE_ENCODING = "native-encoding";

    private String encoding = "UTF8";

    /**
     * Do the work.
     *
     * @exception BuildException Thrown in unrecoverable error.
     */
    public void execute() throws BuildException {
        if ("expand".equals(getTaskType())) {
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
            throw new BuildException("Dest must be a directory.", getLocation());
        }

        FileUtils fileUtils = FileUtils.newFileUtils();

        if (source != null) {
            if (source.isDirectory()) {
                throw new BuildException("Src must not be a directory."
                    + " Use nested filesets instead.", getLocation());
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
        ZipFile zf = null;
        try {
            zf = new ZipFile(srcF, encoding);
            Enumeration e = zf.getEntries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                extractFile(fileUtils, srcF, dir, zf.getInputStream(ze),
                            ze.getName(), new Date(ze.getTime()),
                            ze.isDirectory());
            }

            log("expand complete", Project.MSG_VERBOSE);
        } catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcF.getPath(),
                                     ioe);
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    protected void extractFile(FileUtils fileUtils, File srcF, File dir,
                               InputStream compressedInputStream,
                               String entryName,
                               Date entryDate, boolean isDirectory)
                               throws IOException {

        if (patternsets != null && patternsets.size() > 0) {
            String name = entryName.replace('/', File.separatorChar)
                .replace('\\', File.separatorChar);
            boolean included = false;
            for (int v = 0; v < patternsets.size(); v++) {
                PatternSet p = (PatternSet) patternsets.elementAt(v);
                String[] incls = p.getIncludePatterns(getProject());
                if (incls == null || incls.length == 0) {
                    // no include pattern implicitly means includes="**"
                    incls = new String[] {"**"};
                }

                for (int w = 0; w < incls.length; w++) {
                    String pattern = incls[w].replace('/', File.separatorChar)
                        .replace('\\', File.separatorChar);
                    if (pattern.endsWith(File.separator)) {
                        pattern += "**";
                    }

                    included = SelectorUtils.matchPath(pattern, name);
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
                        String pattern = excls[w]
                            .replace('/', File.separatorChar)
                            .replace('\\', File.separatorChar);
                        if (pattern.endsWith(File.separator)) {
                            pattern += "**";
                        }
                        included = !(SelectorUtils.matchPath(pattern, name));
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
            if (dirF != null) {
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
                        } catch (IOException e) {
                            // ignore
                        }
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

    /**
     * Sets the encoding to assume for file names and comments.
     *
     * <p>Set to <code>native-encoding</code> if you want your
     * platform's native encoding, defaults to UTF8.</p>
     *
     * @since Ant 1.6
     */
    public void setEncoding(String encoding) {
        if (NATIVE_ENCODING.equals(encoding)) {
            encoding = null;
        }
        this.encoding = encoding;
    }

}
