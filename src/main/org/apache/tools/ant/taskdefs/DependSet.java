/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Date;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FileList;

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
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
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
          If we're on Windows, we have to munge the time up to 2 secs to
          be able to check file modification times.
          (Windows has a max resolution of two secs for modification times)
        */
        if (Os.isFamily("windows")) {
            now += 2000;
        }

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

           DirectoryScanner targetDS = targetFS.getDirectoryScanner(project);
           String[] targetFiles      = targetDS.getIncludedFiles();

           for (int i = 0; i < targetFiles.length; i++) {

              File dest = new File(targetFS.getDir(project), targetFiles[i]);
              allTargets.addElement(dest);

              if (dest.lastModified() > now) {
                 log("Warning: " + targetFiles[i] + " modified in the future.",
                     Project.MSG_WARN);
              }

              if (oldestTarget == null ||
                  dest.lastModified() < oldestTargetTime) {
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
           String[] targetFiles = targetFL.getFiles(project);

           for (int i = 0; i < targetFiles.length; i++) {

              File dest = new File(targetFL.getDir(project), targetFiles[i]);
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

              if (oldestTarget == null ||
                  dest.lastModified() < oldestTargetTime) {
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
              String[] sourceFiles = sourceFL.getFiles(project);

              for (int i = 0; upToDate && i < sourceFiles.length; i++) {
                 File src = new File(sourceFL.getDir(project), sourceFiles[i]);

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
                    log(oldestTarget + " is out of date with respect to " +
                        sourceFiles[i], Project.MSG_VERBOSE);
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
              DirectoryScanner sourceDS = sourceFS.getDirectoryScanner(project);
              String[] sourceFiles      = sourceDS.getIncludedFiles();

              for (int i = 0; upToDate && i < sourceFiles.length; i++) {
                 File src = new File(sourceFS.getDir(project), sourceFiles[i]);

                 if (src.lastModified() > now) {
                    log("Warning: " + sourceFiles[i]
                        + " modified in the future.", Project.MSG_WARN);
                 }

                 if (src.lastModified() > oldestTargetTime) {
                    upToDate = false;
                    log(oldestTarget + " is out of date with respect to " +
                        sourceFiles[i], Project.MSG_VERBOSE);
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
