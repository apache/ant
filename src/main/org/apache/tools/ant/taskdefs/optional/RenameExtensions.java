/*
 * RenameExtensions.java
 *
 * Created on 16 March 2000, 23:14
 */

package org.apache.tools.ant.taskdefs.optional;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

/**
 *
 * @author  dion
 * @version
 */
public class RenameExtensions extends MatchingTask {

    private String fromExtension = "";
    private String toExtension = "";
    private boolean replace = false;
    private File srcDir;


    /** Creates new RenameExtensions */
    public RenameExtensions() {
        super();
    }

    /** store fromExtension **/
    public void setFromExtension(String from) {
        fromExtension = from;
    }

    /** store toExtension **/
    public void setToExtension(String to) {
        toExtension = to;
    }

    /**
     * store replace attribute - this determines whether the target file
     * should be overwritten if present
     */
    public void setReplace(String replaceString) {
        replace = Project.toBoolean(replaceString);
    }

    /**
     * Set the source dir to find the files to be renamed.
     */
    public void setSrcDir(String srcDirName) {
        srcDir = project.resolveFile(srcDirName);
    }

    /**
     * Executes the task, i.e. does the actual compiler call
     */
    public void execute() throws BuildException {

        // first off, make sure that we've got a from and to extension
        if (fromExtension == null || toExtension == null || srcDir == null) {
            throw new BuildException("srcDir, destDir, fromExtension and toExtension attributes must be set!");
        }

        // scan source and dest dirs to build up rename list
        DirectoryScanner ds = getDirectoryScanner(srcDir);

        String[] files = ds.getIncludedFiles();

        Hashtable renameList = scanDir(srcDir, files);

        Enumeration e = renameList.keys();
        File fromFile = null;
        File toFile = null;
        while (e.hasMoreElements()) {
            fromFile = (File)e.nextElement();
            toFile = (File)renameList.get(fromFile);
            if (toFile.exists() && replace) toFile.delete();
            if (!fromFile.renameTo(toFile)) throw new BuildException("Rename from: '" + fromFile + "' to '" + toFile + "' failed.");
        }

    }
    private Hashtable scanDir(File srcDir, String[] files) {
        Hashtable list = new Hashtable();
        for (int i = 0; i < files.length; i++) {
            File srcFile = new File(srcDir, files[i]);
            String filename = files[i];
            // if it's a file that ends in the fromExtension, copy to the rename list
            if (filename.toLowerCase().endsWith(fromExtension)) {
                File destFile = new File(srcDir, filename.substring(0, filename.lastIndexOf(fromExtension)) + toExtension);
                if (replace || !destFile.exists()) {
                    list.put(srcFile, destFile);
                } else {
                    project.log("Rejecting file: '" + srcFile + "' for rename as replace is false and file exists", Project.MSG_VERBOSE);
                }
            } else {
                project.log("File '"+ filename + "' doesn't match fromExtension: '" + fromExtension + "'", Project.MSG_VERBOSE);
            }
        }
        return list;
    }

}
