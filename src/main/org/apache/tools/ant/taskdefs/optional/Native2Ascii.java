/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.tools.ant.taskdefs.optional.native2ascii.Native2AsciiAdapter;
import org.apache.tools.ant.taskdefs.optional.native2ascii.Native2AsciiAdapterFactory;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.ant.util.facade.FacadeTaskHelper;
import org.apache.tools.ant.util.facade.ImplementationSpecificArgument;

/**
 * Converts files from native encodings to ASCII.
 *
 * @since Ant 1.2
 */
public class Native2Ascii extends MatchingTask {

    private boolean reverse = false;  // convert from ascii back to native
    private String encoding = null;   // encoding to convert to/from
    private File srcDir = null;       // Where to find input files
    private File destDir = null;      // Where to put output files
    private String extension = null;  // Extension of output files if different

    private Mapper mapper;
    private FacadeTaskHelper facade = null;
    private Native2AsciiAdapter nestedAdapter = null;

    /** No args constructor */
    public Native2Ascii() {
        facade = new FacadeTaskHelper(Native2AsciiAdapterFactory.getDefault());
    }

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
     * The value of the reverse attribute.
     *
     * @return the reverse attribute.
     * @since Ant 1.6.3
     */
    public boolean getReverse() {
        return reverse;
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
     * The value of the encoding attribute.
     *
     * @return the encoding attribute.
     * @since Ant 1.6.3
     */
    public String getEncoding() {
        return encoding;
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
     * Choose the implementation for this particular task.
     *
     * @param impl the name of the implementation
     * @since Ant 1.6.3
     */
    public void setImplementation(String impl) {
        if ("default".equals(impl)) {
            facade.setImplementation(Native2AsciiAdapterFactory.getDefault());
        } else {
            facade.setImplementation(impl);
        }
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     *
     * @return the mapper to use for file name translations.
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
     * A nested filenamemapper
     *
     * @param fileNameMapper the mapper to add
     * @since Ant 1.6.3
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    /**
     * Adds an implementation specific command-line argument.
     *
     * @return a ImplementationSpecificArgument to be configured
     * @since Ant 1.6.3
     */
    public ImplementationSpecificArgument createArg() {
        ImplementationSpecificArgument arg =
            new ImplementationSpecificArgument();
        facade.addImplementationArgument(arg);
        return arg;
    }

    /**
     * The classpath to use when loading the native2ascii
     * implementation if it is not a built-in one.
     *
     * @return Path
     * @since Ant 1.8.0
     */
    public Path createImplementationClasspath() {
        return facade.getImplementationClasspath(getProject());
    }

    /**
     * Set the adapter explicitly.
     *
     * @param adapter Native2AsciiAdapter
     * @since Ant 1.8.0
     */
    public void add(Native2AsciiAdapter adapter) {
        if (nestedAdapter != null) {
            throw new BuildException(
                "Can't have more than one native2ascii adapter");
        }
        nestedAdapter = adapter;
    }

    /**
     * Execute the task
     *
     * @throws BuildException is there is a problem in the task execution.
     */
    @Override
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
            throw new BuildException(
                "The ext attribute or a mapper must be set if src and dest dirs are the same.");
        }

        FileNameMapper m;
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
        for (String file : files) {
            String[] dest = m.mapFileName(file);
            if (dest != null && dest.length > 0) {
                convert(file, dest[0]);
            }
        }
    }

    /**
     * Convert a single file.
     *
     * @param srcName name of the input file.
     * @param destName name of the input file.
     */
    private void convert(String srcName, String destName)
        throws BuildException {
        File srcFile;                         // File to convert
        File destFile;                        // where to put the results

        // Build the full file names
        srcFile = new File(srcDir, srcName);
        destFile = new File(destDir, destName);

        // Make sure we're not about to clobber something
        if (srcFile.equals(destFile)) {
            throw new BuildException("file %s would overwrite itself", srcFile);
        }

        // Make intermediate directories if needed
        // TODO JDK 1.1 doesn't have File.getParentFile,
        String parentName = destFile.getParent();
        if (parentName != null) {
            File parentFile = new File(parentName);

            if (!parentFile.exists()
                && !(parentFile.mkdirs() || parentFile.isDirectory())) {
                throw new BuildException("cannot create parent directory %s",
                    parentName);
            }
        }

        log("converting " + srcName, Project.MSG_VERBOSE);
        Native2AsciiAdapter ad = nestedAdapter != null ? nestedAdapter
                : Native2AsciiAdapterFactory.getAdapter(facade.getImplementation(), this,
                        createImplementationClasspath());
        if (!ad.convert(this, srcFile, destFile)) {
            throw new BuildException("conversion failed");
        }
    }

    /**
     * Returns the (implementation specific) settings given as nested
     * arg elements.
     * @return the arguments.
     * @since Ant 1.6.3
     */
    public String[] getCurrentArgs() {
        return facade.getArgs();
    }

    private class ExtMapper implements FileNameMapper {

        @Override
        public void setFrom(String s) {
        }

        @Override
        public void setTo(String s) {
        }

        @Override
        public String[] mapFileName(String fileName) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot >= 0) {
                return new String[] {fileName.substring(0, lastDot) + extension};
            }
            return new String[] {fileName + extension};
        }
    }
}
