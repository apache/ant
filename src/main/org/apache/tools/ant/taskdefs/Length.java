/*
 * Copyright 2005 The Apache Software Foundation
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
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;
import java.util.HashSet;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;

/**
 * Gets lengths:  of files, byte size; of strings, length (optionally trimmed).
 * The task is overloaded in this way for semantic reasons, much like Available.
 * @since Ant 1.7
 */
public class Length extends Task {

    private static final String ALL = "all";
    private static final String EACH = "each";
    private static final String STRING = "string";

    private String property;
    private String string;
    private Boolean trim;
    private Vector filesets;
    private String mode = ALL;

    /**
     * The property in which the length will be stored.
     * @param property the <code>String</code> property key.
     */
    public synchronized void setProperty(String property) {
        this.property = property;
    }

    /**
     * Set the single file for this task.
     * @param file the <code>File</code> whose length to retrieve.
     */
    public synchronized void setFile(File file) {
        FileSet fs = new FileSet();
        fs.setFile(file);
        add(fs);
    }

    /**
     * Add a FileSet.
     * @param fs the <code>FileSet</code> to add.
     */
    public synchronized void add(FileSet fs) {
        filesets = (filesets == null) ? new Vector() : filesets;
        filesets.add(fs);
    }

    /**
     * Set the execution mode for working with files.
     * @param m the <code>FileMode</code> to use.
     */
    public synchronized void setMode(FileMode m) {
        this.mode = m.getValue();
    }

    /**
     * Set the string whose length to get.
     * @param string <code>String</code>.
     */
    public synchronized void setString(String string) {
        this.string = string;
        this.mode = STRING;
    }

    /**
     * Set whether to trim in string mode.
     * @param trim <code>boolean</code>.
     */
    public synchronized void setTrim(boolean trim) {
        this.trim = trim ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Execute the length task.
     */
    public void execute() {
        validate();
        PrintStream ps = new PrintStream((property != null)
            ? (OutputStream) new PropertyOutputStream()
            : (OutputStream) new LogOutputStream(this, Project.MSG_INFO));

        if (STRING.equals(mode)) {
            ps.print(((trim != null && trim.booleanValue())
                ? string.trim() : string).length());
            ps.close();
        } else if (EACH.equals(mode)) {
            handleFilesets(new EachHandler(ps));
        } else if (ALL.equals(mode)) {
            handleFilesets(new AllHandler(ps));
        }
    }

    private void validate() {
        if (string != null) {
            if (filesets != null && filesets.size() > 0) {
                throw new BuildException("the string length function"
                    + " is incompatible with the file length function");
            }
            if (!(STRING.equals(mode))) {
                throw new BuildException("the mode attribute is for use"
                    + " with the file length function");
            }
        } else if (filesets != null && filesets.size() > 0) {
            if (!(EACH.equals(mode) || ALL.equals(mode))) {
                throw new BuildException("invalid mode setting for"
                    + " file length function: \"" + mode + "\"");
            } else if (trim != null) {
                throw new BuildException("the trim attribute is"
                    + " for use with the string length function only");
            }
        } else {
            throw new BuildException("you must set either the string attribute"
                + " or specify one or more files using the file attribute or"
                + " nested filesets");
        }
    }

    private void handleFilesets(Handler h) {
        HashSet included = new HashSet(filesets.size() * 10);
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) (filesets.get(i));
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File basedir = fs.getDir(getProject());
            String[] f = ds.getIncludedFiles();
            for (int j = 0; j < f.length; j++) {
                File file = FileUtils.getFileUtils().resolveFile(basedir, f[j]);
                if (included.add(file)) {
                    h.handle(file);
                }
            }
        }
        included.clear();
        included = null;
        h.complete();
    }

    private static long getLength(File f) {
        //should be an existing file
        if (!(f.isFile())) {
            throw new BuildException("The absolute pathname " + f
                + " does not denote an existing file.");
        }
        return f.length();
    }

    /** EnumeratedAttribute operation mode */
    public static class FileMode extends EnumeratedAttribute {
        static final String[] MODES = new String[] {EACH, ALL};

        /**
         * Return the possible values for FileMode.
         * @return <code>String[]</code>.
         */
        public String[] getValues() {
            return MODES;
        }

    }

    private class PropertyOutputStream extends ByteArrayOutputStream {
        public void close() {
            getProject().setNewProperty(
                property, new String(toByteArray()).trim());
        }
    }

    private abstract class Handler {
        PrintStream ps;
        Handler(PrintStream ps) {
            this.ps = ps;
        }

        protected abstract void handle(File f);

        void complete() {
            ps.close();
        }
    }

    private class EachHandler extends Handler {
        EachHandler(PrintStream ps) {
            super(ps);
        }
        protected void handle(File f) {
            ps.print(f);
            ps.print(" : ");
            //when writing to the log, we'll see what's happening:
            ps.println(getLength(f));
       }
    }

    private class AllHandler extends Handler {
        long length = 0L;
        AllHandler(PrintStream ps) {
            super(ps);
        }
        protected synchronized void handle(File f) {
            length += getLength(f);
        }
        void complete() {
            ps.print(length);
            super.complete();
        }
    }
}
