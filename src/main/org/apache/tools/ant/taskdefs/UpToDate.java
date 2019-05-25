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
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.ResourceUtils;
import org.apache.tools.ant.util.SourceFileScanner;

/**
 * Sets the given property if the specified target has a timestamp
 * greater than all of the source files.
 *
 * @since Ant 1.2
 *
 * @ant.task category="control"
 */

public class UpToDate extends Task implements Condition {

    private String property;
    private String value;
    private File sourceFile;
    private File targetFile;
    private List<FileSet> sourceFileSets = new Vector<>();
    private Union sourceResources = new Union();

    // CheckStyle:VisibilityModifier OFF - bc
    protected Mapper mapperElement = null;
    // CheckStyle:VisibilityModifier ON

    /**
     * The property to set if the target file is more up-to-date than
     * (each of) the source file(s).
     *
     * @param property the name of the property to set if Target is up-to-date.
     */
    public void setProperty(final String property) {
        this.property = property;
    }

    /**
     * The value to set the named property to if the target file is more
     * up-to-date than (each of) the source file(s). Defaults to 'true'.
     *
     * @param value the value to set the property to if Target is up-to-date
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * Returns the value, or "true" if a specific value wasn't provided.
     */
    private String getValue() {
        return (value != null) ? value : "true";
    }

    /**
     * The file which must be more up-to-date than (each of) the source file(s)
     * if the property is to be set.
     *
     * @param file the file we are checking against.
     */
    public void setTargetFile(final File file) {
        this.targetFile = file;
    }

    /**
     * The file that must be older than the target file
     * if the property is to be set.
     *
     * @param file the file we are checking against the target file.
     */
    public void setSrcfile(final File file) {
        this.sourceFile = file;
    }

    /**
     * Nested &lt;srcfiles&gt; element.
     * @param fs the source files
     */
    public void addSrcfiles(final FileSet fs) {
        sourceFileSets.add(fs);
    }

    /**
     * Nested resource collections as sources.
     * @return the source resources to configure.
     * @since Ant 1.7
     */
    public Union createSrcResources() {
        return sourceResources;
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     * @return a mapper to be configured
     * @throws BuildException if more than one mapper is defined
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * A nested filenamemapper
     * @param fileNameMapper the mapper to add
     * @since Ant 1.6.3
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    /**
     * Evaluate (all) target and source file(s) to
     * see if the target(s) is/are up-to-date.
     * @return true if the target(s) is/are up-to-date
     */
    @Override
    public boolean eval() {
        if (sourceFileSets.isEmpty() && sourceResources.isEmpty()
            && sourceFile == null) {
            throw new BuildException(
                "At least one srcfile or a nested <srcfiles> or <srcresources> element must be set.");
        }

        if ((!sourceFileSets.isEmpty() || !sourceResources.isEmpty())
            && sourceFile != null) {
            throw new BuildException(
                "Cannot specify both the srcfile attribute and a nested <srcfiles> or <srcresources> element.");
        }

        if (targetFile == null && mapperElement == null) {
            throw new BuildException(
                "The targetfile attribute or a nested mapper element must be set.");
        }

        // if the target file is not there, then it can't be up-to-date
        if (targetFile != null && !targetFile.exists()) {
            log("The targetfile \"" + targetFile.getAbsolutePath()
                    + "\" does not exist.", Project.MSG_VERBOSE);
            return false;
        }

        // if the source file isn't there, throw an exception
        if (sourceFile != null && !sourceFile.exists()) {
            throw new BuildException("%s not found.",
                sourceFile.getAbsolutePath());
        }

        boolean upToDate = true;
        if (sourceFile != null) {
            if (mapperElement == null) {
                upToDate = targetFile.lastModified() >= sourceFile.lastModified();
            } else {
                SourceFileScanner sfs = new SourceFileScanner(this);
                upToDate = sfs.restrict(new String[] {sourceFile.getAbsolutePath()},
                                  null, null,
                                  mapperElement.getImplementation()).length == 0;
            }
            if (!upToDate) {
                log(sourceFile.getAbsolutePath()
                    + " is newer than (one of) its target(s).",
                    Project.MSG_VERBOSE);
            }
        }

        // filesets are separate from the rest for performance
        // reasons.  If we use the code for union below, we'll always
        // scan all filesets, even if we know the target is out of
        // date after the first test.

        for (FileSet fs : sourceFileSets) {
            if (!scanDir(fs.getDir(getProject()),
                    fs.getDirectoryScanner(getProject()).getIncludedFiles())) {
                upToDate = false;
                break;
            }
        }

        if (upToDate) {
            Resource[] r = sourceResources.listResources();
            if (r.length > 0) {
                upToDate = ResourceUtils.selectOutOfDateSources(
                        this, r, getMapper(), getProject()).length == 0;
            }
        }

        return upToDate;
    }

    /**
     * Sets property to true if target file(s) have a more recent timestamp
     * than (each of) the corresponding source file(s).
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        if (property == null) {
            throw new BuildException("property attribute is required.",
                                     getLocation());
        }
        boolean upToDate = eval();
        if (upToDate) {
            getProject().setNewProperty(property, getValue());
            if (mapperElement == null) {
                log("File \"" + targetFile.getAbsolutePath()
                    + "\" is up-to-date.", Project.MSG_VERBOSE);
            } else {
                log("All target files are up-to-date.",
                    Project.MSG_VERBOSE);
            }
        }
    }

    /**
     * Scan a directory for files to check for "up to date"ness
     * @param srcDir the directory
     * @param files the files to scan for
     * @return true if the files are up to date
     */
    protected boolean scanDir(File srcDir, String[] files) {
        SourceFileScanner sfs = new SourceFileScanner(this);
        FileNameMapper mapper = getMapper();
        File dir = srcDir;
        if (mapperElement == null) {
            dir = null;
        }
        return sfs.restrict(files, srcDir, dir, mapper).length == 0;
    }

    private FileNameMapper getMapper() {
        if (mapperElement == null) {
            MergingMapper mm = new MergingMapper();
            mm.setTo(targetFile.getAbsolutePath());
            return mm;
        }
        return mapperElement.getImplementation();
    }
}
