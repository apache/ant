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
import java.util.Iterator;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;

/**
 * Gets lengths:  of files/resources, byte size; of strings, length (optionally trimmed).
 * The task is overloaded in this way for semantic reasons, much like Available.
 * @since Ant 1.6.3
 */
public class Length extends Task implements Condition {

    private static final String ALL = "all";
    private static final String EACH = "each";
    private static final String STRING = "string";

    private static final String LENGTH_REQUIRED
        = "Use of the Length condition requires that the length attribute be set.";

    private String property;
    private String string;
    private Boolean trim;
    private String mode = ALL;
    private When when = When.EQUAL;
    private Long length;
    private Vector filesets;

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
        if (fs == null) {
            return;
        }
        filesets = (filesets == null) ? new Vector() : filesets;
        filesets.add(fs);
    }

    /**
     * Set the target count number for use as a Condition.
     * @param ell the long length to compare with.
     */
    public synchronized void setLength(long ell) {
        length = new Long(ell);
    }

    /**
     * Set the comparison criteria for use as a Condition:
     * "equal", "greater", "less". Default is "equal".
     * @param w EnumeratedAttribute When.
     */
    public synchronized void setWhen(When w) {
        when = w;
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
     * Learn whether strings will be trimmed.
     * @return boolean trim setting.
     */
    public boolean getTrim() {
        return trim != null && trim.booleanValue();
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
            ps.print(getLength(string, getTrim()));
            ps.close();
        } else if (EACH.equals(mode)) {
            handleResources(new EachHandler(ps));
        } else if (ALL.equals(mode)) {
            handleResources(new AllHandler(ps));
        }
    }

    /**
     * Fulfill the condition contract.
     * @return true if the condition is true.
     * @throws BuildException if an error occurs.
     */
    public boolean eval() {
        validate();
        if (length == null) {
            throw new BuildException(LENGTH_REQUIRED);
        }
        Long ell = null;
        if (STRING.equals(mode)) {
            ell = new Long(getLength(string, getTrim()));
        } else {
            ConditionHandler h = new ConditionHandler();
            handleResources(h);
            ell = new Long(h.getLength());
        }
        int w = when.getIndex();
        int comp = ell.compareTo(length);
        return (w == 0 && comp == 0)
            || (w == 1 && comp > 0)
            || (w == 2 && comp < 0);
    }

    private void validate() {
        if (string != null) {
            if (filesets != null && filesets.size() > 0) {
                throw new BuildException("the string length function"
                    + " is incompatible with the file length function");
            }
            if (!(STRING.equals(mode))) {
                throw new BuildException("the mode attribute is for use"
                    + " with the file/resource length function");
            }
        } else if (filesets != null) {
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

    private void handleResources(Handler h) {
        for (Iterator i = filesets.iterator(); i.hasNext();) {
            FileSet fs = (FileSet) i.next();
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] f = ds.getIncludedFiles();
            for (int j = 0; j < f.length; j++) {
                Resource r = ds.getResource(f[j]);
                if (!r.isExists()) {
                    log(r.getName() + " does not exist", Project.MSG_ERR);
                } else if (r.isDirectory()) {
                    log(r.getName() + " is a directory; length unspecified",
                        Project.MSG_ERR);
                } else {
                    //force a full path:
                    File basedir = ds.getBasedir();
                    String s = FileUtils.getFileUtils().resolveFile(
                        basedir, r.getName()).getAbsolutePath();
                    h.handle(new Resource(s, true,
                        r.getLastModified(), false, r.getSize()));
                }
            }
        }
        h.complete();
    }

    private static long getLength(String s, boolean t) {
        return (t ? s.trim() : s).length();
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

    /**
     * EnumeratedAttribute for the when attribute.
     */
    public static class When extends EnumeratedAttribute {
        private static final String[] VALUES
            = new String[] {"equal", "greater", "less"};

        private static final When EQUAL = new When("equal");

        public When() {
        }
        public When(String value) {
            setValue(value);
        }
            public String[] getValues() {
            return VALUES;
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

        protected abstract void handle(Resource r);

        void complete() {
            ps.close();
        }
    }

    private class EachHandler extends Handler {
        EachHandler(PrintStream ps) {
            super(ps);
        }
        protected void handle(Resource r) {
            ps.print(r.getName());
            ps.print(" : ");
            //when writing to the log, we'll see what's happening:
            long size = r.getSize();
            if (size == Resource.UNKNOWN_SIZE) {
                ps.println("unknown");
            } else {
                ps.println(size);
            }
       }
    }

    private class AllHandler extends Handler {
        long accum = 0L;
        AllHandler(PrintStream ps) {
            super(ps);
        }
        protected synchronized void handle(Resource r) {
            long size = r.getSize();
            if (size == Resource.UNKNOWN_SIZE) {
                log("Size unknown for " + r.getName(), Project.MSG_WARN);
            } else {
                accum += size;
            }
        }
        void complete() {
            ps.print(accum);
            super.complete();
        }
    }

    private class ConditionHandler extends AllHandler {
        ConditionHandler() {
            super(null);
        }
        void complete() {
        }
        long getLength() {
            return accum;
        }
    }
}
