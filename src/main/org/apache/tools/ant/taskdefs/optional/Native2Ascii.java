/* 
 * Copyright  2000-2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.SourceFileScanner;

/**
 * Converts files from native encodings to ASCII.
 *
 * @author <a href="asudell@acm.org">Drew Sudell</a>
 * @author Stefan Bodewig
 * @since Ant 1.2
 */
public class Native2Ascii extends MatchingTask {

    private boolean reverse = false;  // convert from ascii back to native
    private String encoding = null;   // encoding to convert to/from
    private File srcDir = null;       // Where to find input files
    private File destDir = null;      // Where to put output files
    private String extension = null;  // Extension of output files if different

    private Mapper mapper;

    /**
     * Flag the conversion to run in the reverse sense,
     * that is Ascii to Native encoding.
     *
     * @param reverse True if the conversion is to be reversed,
     *                otherwise false;
     */
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    /**
     * Set the encoding to translate to/from.
     * If unset, the default encoding for the JVM is used.
     *
     * @param encoding String containing the name of the Native
     *                 encoding to convert from or to.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Set the source directory in which to find files to convert.
     *
     * @param srcDir directory to find input file in.
     */
    public void setSrc(File srcDir) {
        this.srcDir = srcDir;
    }


    /**
     * Set the destination directory to place converted files into.
     *
     * @param destDir directory to place output file into.
     */
    public void setDest(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Set the extension which converted files should have.
     * If unset, files will not be renamed.
     *
     * @param ext File extension to use for converted files.
     */
    public void setExt(String ext) {
        this.extension = ext;
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     *
     * @return the mapper to use for file name translations.
     *
     * @throws BuildException if more than one mapper is defined.
     */
    public Mapper createMapper() throws BuildException {
        if (mapper != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     getLocation());
        }
        mapper = new Mapper(getProject());
        return mapper;
    }

    /**
     * Execute the task
     *
     * @throws BuildException is there is a problem in the task execution.
     */
    public void execute() throws BuildException {

        DirectoryScanner scanner = null; // Scanner to find our inputs
        String[] files;                  // list of files to process

        // default srcDir to basedir
        if (srcDir == null) {
            srcDir = getProject().resolveFile(".");
        }

        // Require destDir
        if (destDir == null) {
            throw new BuildException("The dest attribute must be set.");
        }

        // if src and dest dirs are the same, require the extension
        // to be set, so we don't stomp every file.  One could still
        // include a file with the same extension, but ....
        if (srcDir.equals(destDir) && extension == null && mapper == null) {
            throw new BuildException("The ext attribute or a mapper must be set if"
                                     + " src and dest dirs are the same.");
        }

        FileNameMapper m = null;
        if (mapper == null) {
            if (extension == null) {
                m = new IdentityMapper();
            } else {
                m = new ExtMapper();
            }
        } else {
            m = mapper.getImplementation();
        }

        scanner = getDirectoryScanner(srcDir);
        files = scanner.getIncludedFiles();
        SourceFileScanner sfs = new SourceFileScanner(this);
        files = sfs.restrict(files, srcDir, destDir, m);
        int count = files.length;
        if (count == 0) {
            return;
        }
        String message = "Converting " + count + " file"
            + (count != 1 ? "s" : "") + " from ";
        log(message + srcDir + " to " + destDir);
        for (int i = 0; i < files.length; i++) {
            convert(files[i], m.mapFileName(files[i])[0]);
        }
    }

    /**
     * Convert a single file.
     *
     * @param srcName name of the input file.
     * @param destName name of the input file.
     */
    private void convert(String srcName, String destName) throws BuildException {

        Commandline cmd = new Commandline();  // Command line to run
        File srcFile;                         // File to convert
        File destFile;                        // where to put the results

        // Set up the basic args (this could be done once, but
        // it's cleaner here)
        if (reverse) {
            cmd.createArgument().setValue("-reverse");
        }

        if (encoding != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(encoding);
        }

        // Build the full file names
        srcFile = new File(srcDir, srcName);
        destFile = new File(destDir, destName);

        cmd.createArgument().setFile(srcFile);
        cmd.createArgument().setFile(destFile);
        // Make sure we're not about to clobber something
        if (srcFile.equals(destFile)) {
            throw new BuildException("file " + srcFile
                                     + " would overwrite its self");
        }

        // Make intermediate directories if needed
        // XXX JDK 1.1 doesn't have File.getParentFile,
        String parentName = destFile.getParent();
        if (parentName != null) {
            File parentFile = new File(parentName);

            if ((!parentFile.exists()) && (!parentFile.mkdirs())) {
                throw new BuildException("cannot create parent directory "
                                         + parentName);
            }
        }

        log("converting " + srcName, Project.MSG_VERBOSE);
        sun.tools.native2ascii.Main n2a
            = new sun.tools.native2ascii.Main();
        if (!n2a.convert(cmd.getArguments())) {
            throw new BuildException("conversion failed");
        }
    }

    private class ExtMapper implements FileNameMapper {

        public void setFrom(String s) {
        }
        public void setTo(String s) {
        }

        public String[] mapFileName(String fileName) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot >= 0) {
                return new String[] {fileName.substring(0, lastDot)
                                         + extension};
            } else {
                return new String[] {fileName + extension};
            }
        }
    }
}
