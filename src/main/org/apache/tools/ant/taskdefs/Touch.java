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
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Touch a file and/or fileset(s); corresponds to the Unix touch command.
 *
 * <p>If the file to touch doesn't exist, an empty one is
 * created. </p>
 *
 * <p>Note: Setting the modification time of files is not supported in
 * JDK 1.1.</p>
 *
 *
 * @since Ant 1.1
 *
 * @ant.task category="filesystem"
 */
public class Touch extends Task {

    private File file;
    private long millis = -1;
    private String dateTime;
    private Vector filesets = new Vector();
    private FileUtils fileUtils;

    public Touch() {
        fileUtils = FileUtils.newFileUtils();
    }

    /**
     * Sets a single source file to touch.  If the file does not exist
     * an empty file will be created.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * the new modification time of the file
     * in milliseconds since midnight Jan 1 1970.
     * Optional, default=now
     */
    public void setMillis(long millis) {
        this.millis = millis;
    }

    /**
     * the new modification time of the file
     * in the format &quot;MM/DD/YYYY HH:MM AM <i>or</i> PM&quot;
     * or &quot;MM/DD/YYYY HH:MM:SS AM <i>or</i> PM&quot;.
     * Optional, default=now
     */
    public void setDatetime(String dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Add a set of files to touch
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Execute the touch operation.
     */
    public void execute() throws BuildException {
        long savedMillis = millis;

        if (file == null && filesets.size() == 0) {
            throw
                new BuildException("Specify at least one source - a file or "
                                   + "a fileset.");
        }

        if (file != null && file.exists() && file.isDirectory()) {
            throw new BuildException("Use a fileset to touch directories.");
        }

        try {
            if (dateTime != null) {
                /*
                 * The initial version used DateFormat.SHORT for the
                 * time format, which ignores seconds.  If we want
                 * seconds as well, we need DateFormat.MEDIUM, which
                 * in turn would break all old build files.
                 *
                 * First try to parse with DateFormat.SHORT and if
                 * that fails with MEDIUM - throw an exception if both
                 * fail.
                 */
                DateFormat df =
                    DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                   DateFormat.SHORT,
                                                   Locale.US);
                try {
                    setMillis(df.parse(dateTime).getTime());
                } catch (ParseException pe) {
                    df =
                        DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                       DateFormat.MEDIUM,
                                                       Locale.US);
                    try {
                        setMillis(df.parse(dateTime).getTime());
                    } catch (ParseException pe2) {
                        throw new BuildException(pe2.getMessage(), pe,
                                                 getLocation());
                    }
                }

                if (millis < 0) {
                    throw new BuildException("Date of " + dateTime
                                             + " results in negative "
                                             + "milliseconds value "
                                             + "relative to epoch "
                                             + "(January 1, 1970, "
                                             + "00:00:00 GMT).");
                }
            }

            touch();
        } finally {
            millis = savedMillis;
        }
    }

    /**
     * Does the actual work. Entry point for Untar and Expand as well.
     */
    protected void touch() throws BuildException {
        if (file != null) {
            if (!file.exists()) {
                log("Creating " + file, Project.MSG_INFO);
                try {
                    fileUtils.createNewFile(file);
                } catch (IOException ioe) {
                    throw new BuildException("Could not create " + file, ioe,
                                             getLocation());
                }
            }
        }

        if (millis >= 0 && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            log("modification time of files cannot be set in JDK 1.1",
                Project.MSG_WARN);
            return;
        }

        boolean resetMillis = false;
        if (millis < 0) {
            resetMillis = true;
            millis = System.currentTimeMillis();
        }

        if (file != null) {
            touch(file);
        }

        // deal with the filesets
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());

            String[] srcFiles = ds.getIncludedFiles();
            String[] srcDirs = ds.getIncludedDirectories();

            for (int j = 0; j < srcFiles.length; j++) {
                touch(new File(fromDir, srcFiles[j]));
            }

            for (int j = 0; j < srcDirs.length; j++) {
                touch(new File(fromDir, srcDirs[j]));
            }
        }

        if (resetMillis) {
            millis = -1;
        }
    }

    protected void touch(File file) throws BuildException {
        if (!file.canWrite()) {
            throw new BuildException("Can not change modification date of "
                                     + "read-only file " + file);
        }

        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            return;
        }

        fileUtils.setFileLastModified(file, millis);
    }

}
