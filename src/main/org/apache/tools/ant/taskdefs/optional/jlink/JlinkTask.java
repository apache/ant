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
package org.apache.tools.ant.taskdefs.optional.jlink;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;

/**
 * This task defines objects that can link together various jar and
 * zip files.  It is not related to the {@code jlink} tool present in
 * Java 9 and later;  for that, see
 * {@link org.apache.tools.ant.taskdefs.modules.Link}.
 *
 * <p>It is basically a wrapper for the jlink code written originally
 * by <a href="mailto:beard@netscape.com">Patrick Beard</a>.  The
 * classes org.apache.tools.ant.taskdefs.optional.jlink.Jlink and
 * org.apache.tools.ant.taskdefs.optional.jlink.ClassNameReader
 * support this class.</p>
 *
 * <p>For example:</p>
 * <pre>
 * &lt;jlink compress=&quot;false&quot; outfile=&quot;out.jar&quot;/&gt;
 *   &lt;mergefiles&gt;
 *     &lt;pathelement path=&quot;${build.dir}/mergefoo.jar&quot;/&gt;
 *     &lt;pathelement path=&quot;${build.dir}/mergebar.jar&quot;/&gt;
 *   &lt;/mergefiles&gt;
 *   &lt;addfiles&gt;
 *     &lt;pathelement path=&quot;${build.dir}/mac.jar&quot;/&gt;
 *     &lt;pathelement path=&quot;${build.dir}/pc.zip&quot;/&gt;
 *   &lt;/addfiles&gt;
 * &lt;/jlink&gt;
 * </pre>
 *
 * @ant.task ignore="true"
 */
public class JlinkTask extends MatchingTask {

    private File outfile = null;

    private Path mergefiles = null;

    private Path addfiles = null;

    private boolean compress = false;

    /**
     * The output file for this run of jlink. Usually a jar or zip file.
     * @param outfile the output file
     */
    public void setOutfile(File outfile) {
        this.outfile = outfile;
    }

    /**
     * Establishes the object that contains the files to
     * be merged into the output.
     * @return a path to be configured
     */
    public Path createMergefiles() {
        if (this.mergefiles == null) {
            this.mergefiles = new Path(getProject());
        }
        return this.mergefiles.createPath();
    }

    /**
     * Sets the files to be merged into the output.
     * @param mergefiles a path
     */
    public void setMergefiles(Path mergefiles) {
        if (this.mergefiles == null) {
            this.mergefiles = mergefiles;
        } else {
            this.mergefiles.append(mergefiles);
        }
    }

    /**
     * Establishes the object that contains the files to
     * be added to the output.
     * @return a path to be configured
     */
    public Path createAddfiles() {
        if (this.addfiles == null) {
            this.addfiles = new Path(getProject());
        }
        return this.addfiles.createPath();
    }

    /**
     * Sets the files to be added into the output.
     * @param addfiles a path
     */
    public void setAddfiles(Path addfiles) {
        if (this.addfiles == null) {
            this.addfiles = addfiles;
        } else {
            this.addfiles.append(addfiles);
        }
    }

    /**
     * Defines whether or not the output should be compacted.
     * @param compress a <code>boolean</code> value
     */
    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    /**
     * Does the adding and merging.
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        //Be sure everything has been set.
        if (outfile == null) {
            throw new BuildException(
                "outfile attribute is required! Please set.");
        }
        if (!haveAddFiles() && !haveMergeFiles()) {
            throw new BuildException(
                "addfiles or mergefiles required! Please set.");
        }
        log("linking:     " + outfile.getPath());
        log("compression: " + compress, Project.MSG_VERBOSE);
        jlink linker = new jlink();
        linker.setOutfile(outfile.getPath());
        linker.setCompression(compress);
        if (haveMergeFiles()) {
            log("merge files: " + mergefiles.toString(), Project.MSG_VERBOSE);
            linker.addMergeFiles(mergefiles.list());
        }
        if (haveAddFiles()) {
            log("add files: " + addfiles.toString(), Project.MSG_VERBOSE);
            linker.addAddFiles(addfiles.list());
        }
        try  {
            linker.link();
        } catch (Exception ex) {
            throw new BuildException(ex, getLocation());
        }
    }

    private boolean haveAddFiles() {
        return haveEntries(addfiles);
    }

    private boolean haveMergeFiles() {
        return haveEntries(mergefiles);
    }

    private boolean haveEntries(Path p) {
        return !(p == null || p.isEmpty());
    }
}
