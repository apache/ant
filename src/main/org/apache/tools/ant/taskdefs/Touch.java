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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Touchable;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.DateUtils;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;

/**
 * Touch a file and/or fileset(s) and/or filelist(s);
 * corresponds to the Unix touch command.
 *
 * <p>If the file to touch doesn't exist, an empty one is created.</p>
 *
 * @since Ant 1.1
 *
 * @ant.task category="filesystem"
 */
public class Touch extends Task {

    public interface DateFormatFactory {
        DateFormat getPrimaryFormat();
        DateFormat getFallbackFormat();
    }

    /**
     * Provides access to DateUtils.EN_US_DATE_FORMAT_MIN (primary) and
     * DateUtils.EN_US_DATE_FORMAT_SEC (fallback).
     */
    public static final DateFormatFactory DEFAULT_DF_FACTORY
        = new DateFormatFactory() {

        @Override
        public DateFormat getPrimaryFormat() {
            return DateUtils.EN_US_DATE_FORMAT_MIN.get();
        }
        @Override
        public DateFormat getFallbackFormat() {
            return DateUtils.EN_US_DATE_FORMAT_SEC.get();
        }
    };
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private File file;
    private long millis = -1;
    private String dateTime;
    private List<FileSet> filesets = new Vector<>();
    private Union resources;
    private boolean dateTimeConfigured;
    private boolean mkdirs;
    private boolean verbose = true;
    private FileNameMapper fileNameMapper = null;
    private DateFormatFactory dfFactory = DEFAULT_DF_FACTORY;

    /**
     * Sets a single source file to touch.  If the file does not exist
     * an empty file will be created.
     * @param file the <code>File</code> to touch.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Set the new modification time of file(s) touched
     * in milliseconds since midnight Jan 1 1970. Optional, default=now.
     * @param millis the <code>long</code> timestamp to use.
     */
    public void setMillis(long millis) {
        this.millis = millis;
    }

    /**
     * Set the new modification time of file(s) touched
     * in the format &quot;MM/DD/YYYY HH:MM AM <i>or</i> PM&quot;
     * or &quot;MM/DD/YYYY HH:MM:SS AM <i>or</i> PM&quot;.
     * Optional, default=now.
     * @param dateTime the <code>String</code> date in the specified format.
     */
    public void setDatetime(String dateTime) {
        if (this.dateTime != null) {
            log("Resetting datetime attribute to " + dateTime, Project.MSG_VERBOSE);
        }
        this.dateTime = dateTime;
        dateTimeConfigured = false;
    }

    /**
     * Set whether nonexistent parent directories should be created
     * when touching new files.
     * @param mkdirs <code>boolean</code> whether to create parent directories.
     * @since Ant 1.6.3
     */
    public void setMkdirs(boolean mkdirs) {
        this.mkdirs = mkdirs;
    }

    /**
     * Set whether the touch task will report every file it creates;
     * defaults to <code>true</code>.
     * @param verbose <code>boolean</code> flag.
     * @since Ant 1.6.3
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Set the format of the datetime attribute.
     * @param pattern the <code>SimpleDateFormat</code>-compatible format pattern.
     * @since Ant 1.6.3
     */
    public void setPattern(final String pattern) {
        dfFactory = new DateFormatFactory() {
            @Override
            public DateFormat getPrimaryFormat() {
                return new SimpleDateFormat(pattern);
            }
            @Override
            public DateFormat getFallbackFormat() {
                return null;
            }
        };
    }

    /**
     * Add a <code>Mapper</code>.
     * @param mapper the <code>Mapper</code> to add.
     * @since Ant 1.6.3
     */
    public void addConfiguredMapper(Mapper mapper) {
        add(mapper.getImplementation());
    }

    /**
     * Add a <code>FileNameMapper</code>.
     * @param fileNameMapper the <code>FileNameMapper</code> to add.
     * @since Ant 1.6.3
     * @throws BuildException if multiple mappers are added.
     */
    public void add(FileNameMapper fileNameMapper) throws BuildException {
        if (this.fileNameMapper != null) {
            throw new BuildException(
                "Only one mapper may be added to the %s task.", getTaskName());
        }
        this.fileNameMapper = fileNameMapper;
    }

    /**
     * Add a set of files to touch.
     * @param set the <code>Fileset</code> to add.
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
        add(set);
    }

    /**
     * Add a filelist to touch.
     * @param list the <code>Filelist</code> to add.
     */
    public void addFilelist(FileList list) {
        add(list);
    }

    /**
     * Add a collection of resources to touch.
     * @param rc the collection to add.
     * @since Ant 1.7
     */
    public synchronized void add(ResourceCollection rc) {
        resources = resources == null ? new Union() : resources;
        resources.add(rc);
    }

    /**
     * Check that this task has been configured properly.
     * @throws BuildException if configuration errors are detected.
     * @since Ant 1.6.3
     */
    protected synchronized void checkConfiguration() throws BuildException {
        if (file == null && resources == null) {
            throw new BuildException(
                "Specify at least one source--a file or resource collection.");
        }
        if (file != null && file.exists() && file.isDirectory()) {
            throw new BuildException("Use a resource collection to touch directories.");
        }
        if (dateTime != null && !dateTimeConfigured) {
            long workmillis = millis;
            if ("now".equalsIgnoreCase(dateTime)) {
                workmillis = System.currentTimeMillis();
            } else {
                DateFormat df = dfFactory.getPrimaryFormat();
                ParseException pe = null;
                try {
                    workmillis = df.parse(dateTime).getTime();
                } catch (ParseException peOne) {
                    df = dfFactory.getFallbackFormat();
                    if (df == null) {
                        pe = peOne;
                    } else {
                        try {
                            workmillis = df.parse(dateTime).getTime();
                        } catch (ParseException peTwo) {
                            pe = peTwo;
                        }
                    }
                }
                if (pe != null) {
                    throw new BuildException(pe.getMessage(), pe, getLocation());
                }
                if (workmillis < 0) {
                    throw new BuildException(
                        "Date of %s results in negative milliseconds value relative to epoch (January 1, 1970, 00:00:00 GMT).",
                        dateTime);
                }
            }
            log("Setting millis to " + workmillis + " from datetime attribute",
                    ((millis < 0) ? Project.MSG_DEBUG : Project.MSG_VERBOSE));
            setMillis(workmillis);
            // only set if successful to this point:
            dateTimeConfigured = true;
        }
    }

    /**
     * Execute the touch operation.
     *
     * @throws BuildException
     *             if an error occurs.
     */
    @Override
    public void execute() throws BuildException {
        checkConfiguration();
        touch();
    }

    /**
     * Does the actual work; assumes everything has been checked by now.
     * @throws BuildException if an error occurs.
     */
    protected void touch() throws BuildException {
        long defaultTimestamp = getTimestamp();

        if (file != null) {
            touch(new FileResource(file.getParentFile(), file.getName()),
                  defaultTimestamp);
        }
        if (resources == null) {
            return;
        }
        // deal with the resource collections
        for (Resource r : resources) {
            Touchable t = r.as(Touchable.class);
            if (t == null) {
                throw new BuildException("Can't touch " + r);
            }
            touch(r, defaultTimestamp);
        }

        // deal with filesets in a special way since the task
        // originally also used the directories and Union won't return
        // them.
        for (FileSet fs : filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());

            for (String srcDir : ds.getIncludedDirectories()) {
                touch(new FileResource(fromDir, srcDir), defaultTimestamp);
            }
        }
    }

    /**
     * Touch a single file with the current timestamp (this.millis). This method
     * does not interact with any nested mappers and remains for reasons of
     * backwards-compatibility only.
     * @param file file to touch
     * @throws BuildException on error
     * @deprecated since 1.6.x.
     */
    @Deprecated
    protected void touch(File file) {
        touch(file, getTimestamp());
    }

    private long getTimestamp() {
        return (millis < 0) ? System.currentTimeMillis() : millis;
    }

    private void touch(Resource r, long defaultTimestamp) {
        if (fileNameMapper == null) {
            FileProvider fp = r.as(FileProvider.class);
            if (fp != null) {
                // use this to create file and deal with non-writable files
                touch(fp.getFile(), defaultTimestamp);
            } else {
                r.as(Touchable.class).touch(defaultTimestamp);
            }
        } else {
            String[] mapped = fileNameMapper.mapFileName(r.getName());
            if (mapped != null && mapped.length > 0) {
                long modTime = defaultTimestamp;
                if (millis < 0 && r.isExists()) {
                    modTime = r.getLastModified();
                }
                for (String fileName : mapped) {
                    touch(getProject().resolveFile(fileName), modTime);
                }
            }
        }
    }

    private void touch(File file, long modTime) {
        if (!file.exists()) {
            log("Creating " + file,
                ((verbose) ? Project.MSG_INFO : Project.MSG_VERBOSE));
            try {
                FILE_UTILS.createNewFile(file, mkdirs);
            } catch (IOException ioe) {
                throw new BuildException("Could not create " + file, ioe,
                                         getLocation());
            }
        }
        if (!file.canWrite()) {
            throw new BuildException(
                "Can not change modification date of read-only file %s", file);
        }
        FILE_UTILS.setFileLastModified(file, modTime);
    }

}
