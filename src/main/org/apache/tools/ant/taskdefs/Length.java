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
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.Comparison;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.util.FileUtils;
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
     * Set the single resource for this task.
     * @param resource the Resource whose length to retrieve.
     */
    public synchronized void setResource(Resource resource) {
        add(resource);
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
        length = ell;
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
     * Set whether to trim in string mode. Default false.
     * @param trim <code>boolean</code>.
     */
    public synchronized void setTrim(boolean trim) {
        this.trim = trim;
    }

    /**
     * Learn whether strings will be trimmed. Default false.
     * @return boolean trim setting.
     */
    public boolean getTrim() {
        return Boolean.TRUE.equals(trim);
    }

    /**
     * Execute the length task.
     */
    @Override
    public void execute() {
        validate();
        OutputStream out =
            property == null ? new LogOutputStream(this, Project.MSG_INFO)
                : new PropertyOutputStream(getProject(), property);
        PrintStream ps = new PrintStream(out);

        switch (mode) {
        case STRING:
            ps.print(getLength(string, getTrim()));
            ps.close();
            break;
        case EACH:
            handleResources(new EachHandler(ps));
            break;
        case ALL:
            handleResources(new AllHandler(ps));
            break;
        }
    }

    /**
     * Fulfill the condition contract.
     * @return true if the condition is true.
     * @throws BuildException if an error occurs.
     */
    @Override
    public boolean eval() {
        validate();
        if (length == null) {
            throw new BuildException(LENGTH_REQUIRED);
        }
        Long ell;
        if (STRING.equals(mode)) {
            ell = getLength(string, getTrim());
        } else {
            AccumHandler h = new AccumHandler();
            handleResources(h);
            ell = h.getAccum();
        }
        return when.evaluate(ell.compareTo(length));
    }

    private void validate() {
        if (string != null) {
            if (resources != null) {
                throw new BuildException(
                    "the string length function is incompatible with the file/resource length function");
            }
            if (!(STRING.equals(mode))) {
                throw new BuildException(
                    "the mode attribute is for use with the file/resource length function");
            }
        } else if (resources != null) {
            if (!EACH.equals(mode) && !ALL.equals(mode)) {
                throw new BuildException(
                    "invalid mode setting for file/resource length function: \""
                        + mode + "\"");
            }
            if (trim != null) {
                throw new BuildException(
                    "the trim attribute is for use with the string length function only");
            }
        } else {
            throw new BuildException(
                "you must set either the string attribute or specify one or more files using the file attribute or nested resource collections");
        }
    }

    private void handleResources(Handler h) {
        for (Resource r : resources) {
            if (!r.isExists()) {
                log(r + " does not exist", Project.MSG_WARN);
            }
            if (r.isDirectory()) {
                log(r + " is a directory; length may not be meaningful", Project.MSG_WARN);
            }
            h.handle(r);
        }
        h.complete();
    }

    private static long getLength(String s, boolean t) {
        return (t ? s.trim() : s).length();
    }

    /** EnumeratedAttribute operation mode */
    public static class FileMode extends EnumeratedAttribute {
        static final String[] MODES = new String[] {EACH, ALL}; //NOSONAR

        /**
         * Return the possible values for FileMode.
         * @return <code>String[]</code>.
         */
        @Override
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
            FileUtils.close(ps);
        }
    }

    private class EachHandler extends Handler {
        EachHandler(PrintStream ps) {
            super(ps);
        }

        @Override
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

    private class AccumHandler extends Handler {
        private long accum = 0L;

        AccumHandler() {
            super(null);
        }

        protected AccumHandler(PrintStream ps) {
            super(ps);
        }

        protected long getAccum() {
            return accum;
        }

        @Override
        protected synchronized void handle(Resource r) {
            long size = r.getSize();
            if (size == Resource.UNKNOWN_SIZE) {
                log("Size unknown for " + r.toString(), Project.MSG_WARN);
            } else {
                accum += size;
            }
        }
    }

    private class AllHandler extends AccumHandler {
        AllHandler(PrintStream ps) {
            super(ps);
        }

        @Override
        void complete() {
            getPs().print(getAccum());
            super.complete();
        }
    }

}
