/*
 * Copyright  2001-2005 The Apache Software Foundation
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
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

/**
 * Examines and removes out of date target files.  If any of the target files
 * are out of date with respect to any of the source files, all target
 * files are removed.  This is useful where dependencies cannot be
 * computed (for example, dynamically interpreted parameters or files
 * that need to stay in synch but are not directly linked) or where
 * the ant task in question could compute them but does not (for
 * example, the linked DTD for an XML file using the style task).
 *
 * nested arguments:
 * <ul>
 * <li>srcfileset     (fileset describing the source files to examine)
 * <li>srcfilelist    (filelist describing the source files to examine)
 * <li>targetfileset  (fileset describing the target files to examine)
 * <li>targetfilelist (filelist describing the target files to examine)
 * </ul>
 * At least one instance of either a fileset or filelist for both source and
 * target are required.
 * <p>
 * This task will examine each of the source files against each of the target
 * files. If any target files are out of date with respect to any of the source
 * files, all targets are removed. If any files named in a (src or target)
 * filelist do not exist, all targets are removed.
 * Hint: If missing files should be ignored, specify them as include patterns
 * in filesets, rather than using filelists.
 * </p><p>
 * This task attempts to optimize speed of dependency checking.  It will stop
 * after the first out of date file is found and remove all targets, rather
 * than exhaustively checking every source vs target combination unnecessarily.
 * </p><p>
 * Example uses:
 * <ul><li>
 * Record the fact that an XML file must be up to date
 * with respect to its XSD (Schema file), even though the XML file
 * itself includes no reference to its XSD.
 * </li><li>
 * Record the fact that an XSL stylesheet includes other
 * sub-stylesheets
 * </li><li>
 * Record the fact that java files must be recompiled if the ant build
 * file changes
 * </li></ul>
 *
 * @ant.task category="filesystem"
 * @version $Revision$ $Date$
 * @since Ant 1.4
 */
public class DependSet extends MatchingTask {

    private Vector sourceFileSets  = new Vector();
    private Vector sourceFileLists = new Vector();
    private Vector targetFileSets  = new Vector();
    private Vector targetFileLists = new Vector();

    /**
     * Creates a new DependSet Task.
     **/
    public DependSet() {
    } //-- DependSet

    /**
     * Add a set of source files.
     */
    public void addSrcfileset(FileSet fs) {
        sourceFileSets.addElement(fs);
    }

    /**
     * Add a list of source files.
     */
    public void addSrcfilelist(FileList fl) {
        sourceFileLists.addElement(fl);
    }

    /**
     * Add a set of target files.
     */
    public void addTargetfileset(FileSet fs) {
        targetFileSets.addElement(fs);
    }

    /**
     * Add a list of target files.
     */
    public void addTargetfilelist(FileList fl) {
        targetFileLists.addElement(fl);
    }

    /**
     * Executes the task.
     */

    public void execute() throws BuildException {

        if ((sourceFileSets.size() == 0) && (sourceFileLists.size() == 0)) {
          throw new BuildException("At least one <srcfileset> or <srcfilelist>"
                                   + " element must be set");
        }

        if ((targetFileSets.size() == 0) && (targetFileLists.size() == 0)) {
          throw new BuildException("At least one <targetfileset> or"
                                   + " <targetfilelist> element must be set");
        }

        long now = (new Date()).getTime();
        /*
          We have to munge the time to allow for the filesystem time
          granularity.
        */
        now += FileUtils.getFileUtils().getFileTimestampGranularity();

        //
        // Grab all the target files specified via filesets
        //
        Vector  allTargets         = new Vector();
        long oldestTargetTime = 0;
        File oldestTarget = null;
        Enumeration enumTargetSets = targetFileSets.elements();
        while (enumTargetSets.hasMoreElements()) {

           FileSet targetFS          = (FileSet) enumTargetSets.nextElement();
           if (!targetFS.getDir(getProject()).exists()) {
               // this is the same as if it was empty, no target files found
               continue;
           }

           DirectoryScanner targetDS = targetFS.getDirectoryScanner(getProject());
           String[] targetFiles      = targetDS.getIncludedFiles();

           for (int i = 0; i < targetFiles.length; i++) {

              File dest = new File(targetFS.getDir(getProject()), targetFiles[i]);
              allTargets.addElement(dest);

              if (dest.lastModified() > now) {
                 log("Warning: " + targetFiles[i] + " modified in the future.",
                     Project.MSG_WARN);
              }

              if (oldestTarget == null
                || dest.lastModified() < oldestTargetTime) {
                  oldestTargetTime = dest.lastModified();
                  oldestTarget = dest;
              }
           }
        }

        //
        // Grab all the target files specified via filelists
        //
        boolean upToDate            = true;
        Enumeration enumTargetLists = targetFileLists.elements();
        while (enumTargetLists.hasMoreElements()) {

           FileList targetFL    = (FileList) enumTargetLists.nextElement();
           String[] targetFiles = targetFL.getFiles(getProject());

           for (int i = 0; i < targetFiles.length; i++) {

              File dest = new File(targetFL.getDir(getProject()), targetFiles[i]);
              if (!dest.exists()) {
                 log(targetFiles[i] + " does not exist.", Project.MSG_VERBOSE);
                 upToDate = false;
                 continue;
              } else {
                 allTargets.addElement(dest);
              }
              if (dest.lastModified() > now) {
                 log("Warning: " + targetFiles[i] + " modified in the future.",
                     Project.MSG_WARN);
              }

              if (oldestTarget == null
                  || dest.lastModified() < oldestTargetTime) {
                  oldestTargetTime = dest.lastModified();
                  oldestTarget = dest;
              }
           }
        }
        if (oldestTarget != null) {
            log(oldestTarget + " is oldest target file", Project.MSG_VERBOSE);
        } else {
            // no target files, then we cannot remove any target files and
            // skip the following tests right away
            upToDate = false;
        }

        //
        // Check targets vs source files specified via filelists
        //
        if (upToDate) {
           Enumeration enumSourceLists = sourceFileLists.elements();
           while (upToDate && enumSourceLists.hasMoreElements()) {

              FileList sourceFL    = (FileList) enumSourceLists.nextElement();
              String[] sourceFiles = sourceFL.getFiles(getProject());

              for (int i = 0; upToDate && i < sourceFiles.length; i++) {
                 File src = new File(sourceFL.getDir(getProject()), sourceFiles[i]);

                 if (src.lastModified() > now) {
                    log("Warning: " + sourceFiles[i]
                        + " modified in the future.", Project.MSG_WARN);
                 }

                 if (!src.exists()) {
                    log(sourceFiles[i] + " does not exist.",
                        Project.MSG_VERBOSE);
                    upToDate = false;
                    break;
                 }

                 if (src.lastModified() > oldestTargetTime) {
                    upToDate = false;
                    log(oldestTarget + " is out of date with respect to "
                        + sourceFiles[i], Project.MSG_VERBOSE);
                 }
              }
           }
        }

        //
        // Check targets vs source files specified via filesets
        //
        if (upToDate) {
           Enumeration enumSourceSets = sourceFileSets.elements();
           while (upToDate && enumSourceSets.hasMoreElements()) {

              FileSet sourceFS         = (FileSet) enumSourceSets.nextElement();
              DirectoryScanner sourceDS = sourceFS.getDirectoryScanner(getProject());
              String[] sourceFiles      = sourceDS.getIncludedFiles();

              for (int i = 0; upToDate && i < sourceFiles.length; i++) {
                 File src = new File(sourceFS.getDir(getProject()), sourceFiles[i]);

                 if (src.lastModified() > now) {
                    log("Warning: " + sourceFiles[i]
                        + " modified in the future.", Project.MSG_WARN);
                 }

                 if (src.lastModified() > oldestTargetTime) {
                    upToDate = false;
                    log(oldestTarget + " is out of date with respect to "
                        + sourceFiles[i], Project.MSG_VERBOSE);
                 }
              }
           }
        }

        if (!upToDate) {
           log("Deleting all target files. ", Project.MSG_VERBOSE);
           for (Enumeration e = allTargets.elements(); e.hasMoreElements();) {
              File fileToRemove = (File) e.nextElement();
              log("Deleting file " + fileToRemove.getAbsolutePath(),
                  Project.MSG_VERBOSE);
              fileToRemove.delete();
           }
        }

    } //-- execute

} //-- DependSet.java
