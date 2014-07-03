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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.KeepAliveOutputStream;

/**
 * <p> A wrapper for the implementations of <code>JUnitResultFormatter</code>.
 * In particular, used as a nested <code>&lt;formatter&gt;</code> element in
 * a <code>&lt;junit&gt;</code> task.
 * <p> For example,
 * <code><pre>
 *       &lt;junit printsummary="no" haltonfailure="yes" fork="false"&gt;
 *           &lt;formatter type="plain" usefile="false" /&gt;
 *           &lt;test name="org.apache.ecs.InternationalCharTest" /&gt;
 *       &lt;/junit&gt;</pre></code>
 * adds a <code>plain</code> type implementation
 * (<code>PlainJUnitResultFormatter</code>) to display the results of the test.
 *
 * <p> Either the <code>type</code> or the <code>classname</code> attribute
 * must be set.
 *
 * @see JUnitTask
 * @see XMLJUnitResultFormatter
 * @see BriefJUnitResultFormatter
 * @see PlainJUnitResultFormatter
 * @see FailureRecorder
 * @see JUnitResultFormatter
 */
public class FormatterElement {

    private String classname;
    private String extension;
    private OutputStream out = new KeepAliveOutputStream(System.out);
    private File outFile;
    private boolean useFile = true;
    private Object ifCond;
    private Object unlessCond;

    /**
     * Store the project reference for passing it to nested components.
     * @since Ant 1.8
     */
    private Project project;

    /** xml formatter class */
    public static final String XML_FORMATTER_CLASS_NAME =
        "org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter";
    /** brief formatter class */
    public static final String BRIEF_FORMATTER_CLASS_NAME =
        "org.apache.tools.ant.taskdefs.optional.junit.BriefJUnitResultFormatter";
    /** plain formatter class */
    public static final String PLAIN_FORMATTER_CLASS_NAME =
        "org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter";
    /** failure recorder class */
    public static final String FAILURE_RECORDER_CLASS_NAME =
        "org.apache.tools.ant.taskdefs.optional.junit.FailureRecorder";

    /**
     * <p> Quick way to use a standard formatter.
     *
     * <p> At the moment, there are three supported standard formatters.
     * <ul>
     * <li> The <code>xml</code> type uses a <code>XMLJUnitResultFormatter</code>.
     * <li> The <code>brief</code> type uses a <code>BriefJUnitResultFormatter</code>.
     * <li> The <code>plain</code> type (the default) uses a <code>PlainJUnitResultFormatter</code>.
     * <li> The <code>failure</code> type uses a <code>FailureRecorder</code>.
     * </ul>
     *
     * <p> Sets <code>classname</code> attribute - so you can't use that
     * attribute if you use this one.
     * @param type the enumerated value to use.
     */
    public void setType(TypeAttribute type) {
        if ("xml".equals(type.getValue())) {
            setClassname(XML_FORMATTER_CLASS_NAME);
        } else {
            if ("brief".equals(type.getValue())) {
                setClassname(BRIEF_FORMATTER_CLASS_NAME);
            } else {
                if ("failure".equals(type.getValue())) {
                    setClassname(FAILURE_RECORDER_CLASS_NAME);
                } else { // must be plain, ensured by TypeAttribute
                    setClassname(PLAIN_FORMATTER_CLASS_NAME);
                }
            }
        }
    }

    /**
     * <p> Set name of class to be used as the formatter.
     *
     * <p> This class must implement <code>JUnitResultFormatter</code>
     * @param classname the name of the formatter class.
     */
    public void setClassname(String classname) {
        this.classname = classname;
        if (XML_FORMATTER_CLASS_NAME.equals(classname)) {
           setExtension(".xml");
        } else if (PLAIN_FORMATTER_CLASS_NAME.equals(classname)) {
           setExtension(".txt");
        } else if (BRIEF_FORMATTER_CLASS_NAME.equals(classname)) {
           setExtension(".txt");
        }
    }

    /**
     * Get name of class to be used as the formatter.
     * @return the name of the class.
     */
    public String getClassname() {
        return classname;
    }

    /**
     * Set the extension to use for the report file.
     * @param ext the extension to use.
     */
    public void setExtension(String ext) {
        this.extension = ext;
    }

    /**
     * Get the extension used for the report file.
     * @return the extension.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * <p> Set the file which the formatte should log to.
     *
     * <p> Note that logging to file must be enabled .
     */
    void setOutfile(File out) {
        this.outFile = out;
    }

    /**
     * <p> Set output stream for formatter to use.
     *
     * <p> Defaults to standard out.
     * @param out the output stream to use.
     */
    public void setOutput(OutputStream out) {
        if (out == System.out || out == System.err) {
            out = new KeepAliveOutputStream(out);
        }
        this.out = out;
    }

    /**
     * Set whether the formatter should log to file.
     * @param useFile if true use a file, if false send
     *                to standard out.
     */
    public void setUseFile(boolean useFile) {
        this.useFile = useFile;
    }

    /**
     * Get whether the formatter should log to file.
     */
    boolean getUseFile() {
        return useFile;
    }

    /**
     * Set whether this formatter should be used.  It will be used if
     * the expression evaluates to true or the name of a property
     * which has been set, otherwise it won't.
     * @param ifCond name of property
     * @since Ant 1.8.0
     */
    public void setIf(Object ifCond) {
        this.ifCond = ifCond;
    }

    /**
     * Set whether this formatter should be used.  It will be used if
     * the expression evaluates to true or the name of a property
     * which has been set, otherwise it won't.
     * @param ifCond name of property
     */
    public void setIf(String ifCond) {
        setIf((Object) ifCond);
    }

    /**
     * Set whether this formatter should NOT be used. It will be used
     * if the expression evaluates to false or the name of a property
     * which has not been set, orthwise it will not be used.
     * @param unlessCond name of property
     * @since Ant 1.8.0
     */
    public void setUnless(Object unlessCond) {
        this.unlessCond = unlessCond;
    }

    /**
     * Set whether this formatter should NOT be used. It will be used
     * if the expression evaluates to false or the name of a property
     * which has not been set, orthwise it will not be used.
     * @param unlessCond name of property
     */
    public void setUnless(String unlessCond) {
        setUnless((Object) unlessCond);
    }

    /**
     * Ensures that the selector passes the conditions placed
     * on it with <code>if</code> and <code>unless</code> properties.
     * @param t the task the this formatter is used in.
     * @return true if the formatter should be used.
     */
    public boolean shouldUse(Task t) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(t.getProject());
        return ph.testIfCondition(ifCond)
            && ph.testUnlessCondition(unlessCond);
    }

    /**
     * @since Ant 1.2
     */
    JUnitTaskMirror.JUnitResultFormatterMirror createFormatter() throws BuildException {
        return createFormatter(null);
    }

    /**
     * Store the project reference for passing it to nested components.
     * @param project the reference
     * @since Ant 1.8
     */
    public void setProject(Project project) {
        this.project = project;
    }


    /**
     * @since Ant 1.6
     */
    JUnitTaskMirror.JUnitResultFormatterMirror createFormatter(ClassLoader loader)
            throws BuildException {

        if (classname == null) {
            throw new BuildException("you must specify type or classname");
        }
        //although this code appears to duplicate that of ClasspathUtils.newInstance,
        //we cannot use that because this formatter may run in a forked process,
        //without that class.
        Class f = null;
        try {
            if (loader == null) {
                f = Class.forName(classname);
            } else {
                f = Class.forName(classname, true, loader);
            }
        } catch (ClassNotFoundException e) {
            throw new BuildException(
                "Using loader " + loader + " on class " + classname
                + ": " + e, e);
        } catch (NoClassDefFoundError e) {
            throw new BuildException(
                "Using loader " + loader + " on class " + classname
                + ": " + e, e);
        }

        Object o = null;
        try {
            o = f.newInstance();
        } catch (InstantiationException e) {
            throw new BuildException(e);
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        }

        if (!(o instanceof JUnitTaskMirror.JUnitResultFormatterMirror)) {
            throw new BuildException(classname + " is not a JUnitResultFormatter");
        }
        JUnitTaskMirror.JUnitResultFormatterMirror r =
            (JUnitTaskMirror.JUnitResultFormatterMirror) o;
        if (useFile && outFile != null) {
            out = new DelayedFileOutputStream(outFile);
        }
        r.setOutput(out);


        boolean needToSetProjectReference = true;
        try {
            Field field = r.getClass().getField("project");
            Object value = field.get(r);
            if (value instanceof Project) {
                // there is already a project reference so dont overwrite this
                needToSetProjectReference = false;
            }
        } catch (NoSuchFieldException e) {
            // no field present, so no previous reference exists
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        }

        if (needToSetProjectReference) {
            Method setter;
            try {
                setter = r.getClass().getMethod("setProject", new Class[] {Project.class});
                setter.invoke(r, new Object[] {project});
            } catch (NoSuchMethodException e) {
                // no setProject to invoke; just ignore
            } catch (IllegalAccessException e) {
                throw new BuildException(e);
            } catch (InvocationTargetException e) {
                throw new BuildException(e);
            }
        }

        return r;
    }

    /**
     * <p> Enumerated attribute with the values "plain", "xml", "brief" and "failure".
     *
     * <p> Use to enumerate options for <code>type</code> attribute.
     */
    public static class TypeAttribute extends EnumeratedAttribute {
        /** {@inheritDoc}. */
        public String[] getValues() {
            return new String[] {"plain", "xml", "brief", "failure"};
        }
    }

    /**
     * A standard FileOutputStream creates a file as soon as it's opened. This
     * class delays the creation of the file until the first time a caller attempts
     * to write to it so we don't end up with empty files if the listeners don't use
     * them.
     */
    private static class DelayedFileOutputStream extends OutputStream {

        private BufferedOutputStream outputStream;
        private final File file;

        public DelayedFileOutputStream(File file) {
            this.file = file;
        }

        @Override
        public void write(int b) throws IOException {
            synchronized (this) {
                if (outputStream == null) {
                    outputStream = new BufferedOutputStream(new FileOutputStream(file));
                }
            }
            outputStream.write(b);
        }

        @Override
        public void flush() throws IOException {
            if (outputStream != null) {
                outputStream.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }
}
