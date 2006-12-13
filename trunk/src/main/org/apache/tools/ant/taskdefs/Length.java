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
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.Comparison;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.PropertyOutputStream;

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
    private Comparison when = Comparison.EQUAL;
    private Long length;
    private Resources resources;

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
        add(new FileResource(file));
    }

    /**
     * Add a FileSet.
     * @param fs the <code>FileSet</code> to add.
     */
    public synchronized void add(FileSet fs) {
        add((ResourceCollection) fs);
    }

    /**
     * Add a ResourceCollection.
     * @param c the <code>ResourceCollection</code> to add.
     * @since Ant 1.7
     */
    public synchronized void add(ResourceCollection c) {
        if (c == null) {
            return;
        }
        resources = (resources == null) ? new Resources() : resources;
        resources.add(c);
    }

    /**
     * Set the target count number for use as a Condition.
     * @param ell the long length to compare with.
     */
    public synchronized void setLength(long ell) {
        length = new Long(ell);
    }

    /**
     * Set the comparison for use as a Condition.
     * @param w EnumeratedAttribute When.
     * @see org.apache.tools.ant.types.Comparison
     */
    public synchronized void setWhen(When w) {
        setWhen((Comparison) w);
    }

    /**
     * Set the comparison for use as a Condition.
     * @param c Comparison.
     * @see org.apache.tools.ant.types.Comparison
     * @since Ant 1.7
     */
    public synchronized void setWhen(Comparison c) {
        when = c;
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
            ? (OutputStream) new PropertyOutputStream(getProject(), property)
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
        return when.evaluate(ell.compareTo(length));
    }

    private void validate() {
        if (string != null) {
            if (resources != null) {
                throw new BuildException("the string length function"
                    + " is incompatible with the file/resource length function");
            }
            if (!(STRING.equals(mode))) {
                throw new BuildException("the mode attribute is for use"
                    + " with the file/resource length function");
            }
        } else if (resources != null) {
            if (!(EACH.equals(mode) || ALL.equals(mode))) {
                throw new BuildException("invalid mode setting for"
                    + " file/resource length function: \"" + mode + "\"");
            } else if (trim != null) {
                throw new BuildException("the trim attribute is"
                    + " for use with the string length function only");
            }
        } else {
            throw new BuildException("you must set either the string attribute"
                + " or specify one or more files using the file attribute or"
                + " nested resource collections");
        }
    }

    private void handleResources(Handler h) {
        for (Iterator i = resources.iterator(); i.hasNext();) {
            Resource r = (Resource) i.next();
            if (!r.isExists()) {
                log(r + " does not exist", Project.MSG_ERR);
            } else if (r.isDirectory()) {
                log(r + " is a directory; length unspecified",
                    Project.MSG_ERR);
            } else {
                h.handle(r);
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
    public static class When extends Comparison {
        //extend Comparison; retain for BC only
    }

    private abstract class Handler {
        private PrintStream ps;
        Handler(PrintStream ps) {
            this.ps = ps;
        }

        protected PrintStream getPs() {
            return ps;
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
            getPs().print(r.toString());
            getPs().print(" : ");
            //when writing to the log, we'll see what's happening:
            long size = r.getSize();
            if (size == Resource.UNKNOWN_SIZE) {
                getPs().println("unknown");
            } else {
                getPs().println(size);
            }
       }
    }

    private class AllHandler extends Handler {
        private long accum = 0L;
        AllHandler(PrintStream ps) {
            super(ps);
        }
        protected long getAccum() {
            return accum;
        }
        protected synchronized void handle(Resource r) {
            long size = r.getSize();
            if (size == Resource.UNKNOWN_SIZE) {
                log("Size unknown for " + r.toString(), Project.MSG_WARN);
            } else {
                accum += size;
            }
        }
        void complete() {
            getPs().print(accum);
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
            return getAccum();
        }
    }
}
