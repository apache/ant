/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
import java.util.Iterator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.TimeComparison;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.selectors.Not;
import org.apache.tools.ant.types.resources.selectors.Exists;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.types.resources.comparators.Reverse;
import org.apache.tools.ant.types.resources.comparators.ResourceComparator;

/**
 * Examines and removes out of date target files.  If any of the target files
 * are out of date with respect to any of the source files, all target
 * files are removed.  This is useful where dependencies cannot be
 * computed (for example, dynamically interpreted parameters or files
 * that need to stay in synch but are not directly linked) or where
 * the ant task in question could compute them but does not (for
 * example, the linked DTD for an XML file using the XSLT task).
 *
 * nested arguments:
 * <ul>
 * <li>sources        (resource union describing the source resources to examine)
 * <li>srcfileset     (fileset describing the source files to examine)
 * <li>srcfilelist    (filelist describing the source files to examine)
 * <li>targets        (path describing the target files to examine)
 * <li>targetfileset  (fileset describing the target files to examine)
 * <li>targetfilelist (filelist describing the target files to examine)
 * </ul>
 * At least one of both source and target entities is required.
 * <p>
 * This task will examine each of the sources against each of the target files. If
 * any target files are out of date with respect to any of the sources, all targets
 * are removed. If any sources or targets do not exist, all targets are removed.
 * Hint: If missing files should be ignored, specify them as include patterns
 * in filesets, rather than using filelists.
 * </p><p>
 * This task attempts to optimize speed of dependency checking
 * by comparing only the dates of the oldest target file and the newest source.
 * </p><p>
 * Example uses:
 * <ul><li>
 * Record the fact that an XML file must be up to date with respect to its XSD
 * (Schema file), even though the XML file itself includes no reference to its XSD.
 * </li><li>
 * Record the fact that an XSL stylesheet includes other sub-stylesheets
 * </li><li>
 * Record the fact that java files must be recompiled if the ant build file changes
 * </li></ul>
 *
 * @ant.task category="filesystem"
 * @since Ant 1.4
 */
public class DependSet extends MatchingTask {

    private static final ResourceSelector NOT_EXISTS = new Not(new Exists());
    private static final ResourceComparator DATE
        = new org.apache.tools.ant.types.resources.comparators.Date();
    private static final ResourceComparator REVERSE_DATE = new Reverse(DATE);

    private static final class NonExistent extends Restrict {
        private NonExistent(ResourceCollection rc) {
            super.add(rc);
            super.add(NOT_EXISTS);
        }
    }

    private static final class HideMissingBasedir
        implements ResourceCollection {
        private FileSet fs;

        private HideMissingBasedir(FileSet fs) {
            this.fs = fs;
        }
        public Iterator iterator() {
            return basedirExists() ? fs.iterator() : Resources.EMPTY_ITERATOR;
        }
        public int size() {
            return basedirExists() ? fs.size() : 0;
        }
        public boolean isFilesystemOnly() {
            return true;
        }
        private boolean basedirExists() {
            File basedir = fs.getDir();
            //trick to evoke "basedir not set" if null:
            return basedir == null || basedir.exists();
        }
    }

    private Union sources = null;
    private Path targets = null;

    /**
     * Create a nested sources element.
     * @return a Union instance.
     */
    public synchronized Union createSources() {
        sources = (sources == null) ? new Union() : sources;
        return sources;
    }

    /**
     * Add a set of source files.
     * @param fs the FileSet to add.
     */
    public void addSrcfileset(FileSet fs) {
        createSources().add(fs);
    }

    /**
     * Add a list of source files.
     * @param fl the FileList to add.
     */
    public void addSrcfilelist(FileList fl) {
        createSources().add(fl);
    }

    /**
     * Create a nested targets element.
     * @return a Union instance.
     */
    public synchronized Path createTargets() {
        targets = (targets == null) ? new Path(getProject()) : targets;
        return targets;
    }

    /**
     * Add a set of target files.
     * @param fs the FileSet to add.
     */
    public void addTargetfileset(FileSet fs) {
        createTargets().add(new HideMissingBasedir(fs));
    }

    /**
     * Add a list of target files.
     * @param fl the FileList to add.
     */
    public void addTargetfilelist(FileList fl) {
        createTargets().add(fl);
    }

    /**
     * Execute the task.
     * @throws BuildException if errors occur.
     */
    public void execute() throws BuildException {
        if (sources == null) {
          throw new BuildException(
              "At least one set of source resources must be specified");
        }
        if (targets == null) {
          throw new BuildException(
              "At least one set of target files must be specified");
        }
        //no sources = nothing to compare; no targets = nothing to delete:
        if (sources.size() > 0 && targets.size() > 0 && !uptodate(sources, targets)) {
           log("Deleting all target files.", Project.MSG_VERBOSE);
           Delete delete = new Delete();
           delete.bindToOwner(this);
           delete.add(targets);
           delete.perform();
        }
    }

    private boolean uptodate(ResourceCollection src, ResourceCollection target) {
        org.apache.tools.ant.types.resources.selectors.Date datesel
            = new org.apache.tools.ant.types.resources.selectors.Date();
        datesel.setMillis(System.currentTimeMillis());
        datesel.setWhen(TimeComparison.AFTER);
        logFuture(targets, datesel);

        int neTargets = new NonExistent(targets).size();
        if (neTargets > 0) {
            log(neTargets + " nonexistent targets", Project.MSG_VERBOSE);
            return false;
        }
        FileResource oldestTarget = (FileResource) getOldest(targets);
        log(oldestTarget + " is oldest target file", Project.MSG_VERBOSE);

        logFuture(sources, datesel);

        int neSources = new NonExistent(sources).size();
        if (neSources > 0) {
            log(neSources + " nonexistent sources", Project.MSG_VERBOSE);
            return false;
        }
        Resource newestSource = (Resource) getNewest(sources);
        log(newestSource.toLongString() + " is newest source", Project.MSG_VERBOSE);
        return oldestTarget.getLastModified() >= newestSource.getLastModified();
    }

    private void logFuture(ResourceCollection rc, ResourceSelector rsel) {
        Restrict r = new Restrict();
        r.add(rsel);
        r.add(rc);
        for (Iterator i = r.iterator(); i.hasNext();) {
            log("Warning: " + i.next() + " modified in the future.", Project.MSG_WARN);
        }
    }

    private Resource getXest(ResourceCollection rc, ResourceComparator c) {
        Iterator i = rc.iterator();
        if (!i.hasNext()) {
            return null;

        }
        Resource xest = (Resource) i.next();
        while (i.hasNext()) {
            Resource next = (Resource) i.next();
            if (c.compare(xest, next) < 0) {
                xest = next;
            }
        }
        return xest;
    }

    private Resource getOldest(ResourceCollection rc) {
        return getXest(rc, REVERSE_DATE);
    }

    private Resource getNewest(ResourceCollection rc) {
        return getXest(rc, DATE);
    }

}
