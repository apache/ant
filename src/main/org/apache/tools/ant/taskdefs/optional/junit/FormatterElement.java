/*
 * Copyright  2001-2004 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;

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
 * @see JUnitResultFormatter
 */
public class FormatterElement {

    private String classname;
    private String extension;
    private OutputStream out = System.out;
    private File outFile;
    private boolean useFile = true;
    private String ifProperty;
    private String unlessProperty;

    public static final String XML_FORMATTER_CLASS_NAME =
        "org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter";
    public static final String BRIEF_FORMATTER_CLASS_NAME =
        "org.apache.tools.ant.taskdefs.optional.junit.BriefJUnitResultFormatter";
    public static final String PLAIN_FORMATTER_CLASS_NAME =
        "org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter";

    /**
     * <p> Quick way to use a standard formatter.
     *
     * <p> At the moment, there are three supported standard formatters.
     * <ul>
     * <li> The <code>xml</code> type uses a <code>XMLJUnitResultFormatter</code>.
     * <li> The <code>brief</code> type uses a <code>BriefJUnitResultFormatter</code>.
     * <li> The <code>plain</code> type (the default) uses a <code>PlainJUnitResultFormatter</code>.
     * </ul>
     *
     * <p> Sets <code>classname</code> attribute - so you can't use that
     * attribute if you use this one.
     */
    public void setType(TypeAttribute type) {
        if ("xml".equals(type.getValue())) {
            setClassname(XML_FORMATTER_CLASS_NAME);
        } else {
            if ("brief".equals(type.getValue())) {
                setClassname(BRIEF_FORMATTER_CLASS_NAME);
            } else { // must be plain, ensured by TypeAttribute
                setClassname(PLAIN_FORMATTER_CLASS_NAME);
            }
        }
    }

    /**
     * <p> Set name of class to be used as the formatter.
     *
     * <p> This class must implement <code>JUnitResultFormatter</code>
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
     */
    public String getClassname() {
        return classname;
    }

    public void setExtension(String ext) {
        this.extension = ext;
    }

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
     */
    public void setOutput(OutputStream out) {
        this.out = out;
    }

    /**
     * Set whether the formatter should log to file.
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
     * Set whether this formatter should be used.  It will be
     * used if the property has been set, otherwise it won't.
     * @param ifProperty name of property
     */
    public void setIf(String ifProperty) {
        this.ifProperty = ifProperty;
    }

    /**
     * Set whether this formatter should NOT be used. It
     * will not be used if the property has been set, orthwise it
     * will be used.
     * @param unlessProperty name of property
     */
    public void setUnless(String unlessProperty) {
        this.unlessProperty = unlessProperty;
    }

    /**
     * Ensures that the selector passes the conditions placed
     * on it with <code>if</code> and <code>unless</code> properties.
     */
    public boolean shouldUse(Task t) {
        if (ifProperty != null && t.getProject().getProperty(ifProperty) == null) {
            return false;
        } else if (unlessProperty != null
                    && t.getProject().getProperty(unlessProperty) != null) {
            return false;
        }

        return true;
    }

    /**
     * @since Ant 1.2
     */
    JUnitResultFormatter createFormatter() throws BuildException {
        return createFormatter(null);
    }

    /**
     * @since Ant 1.6
     */
    JUnitResultFormatter createFormatter(ClassLoader loader)
        throws BuildException {

        if (classname == null) {
            throw new BuildException("you must specify type or classname");
        }

        Class f = null;
        try {
            if (loader == null) {
                f = Class.forName(classname);
            } else {
                f = Class.forName(classname, true, loader);
            }
        } catch (ClassNotFoundException e) {
            throw new BuildException(e);
        }

        Object o = null;
        try {
            o = f.newInstance();
        } catch (InstantiationException e) {
            throw new BuildException(e);
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        }

        if (!(o instanceof JUnitResultFormatter)) {
            throw new BuildException(classname
                + " is not a JUnitResultFormatter");
        }

        JUnitResultFormatter r = (JUnitResultFormatter) o;

        if (useFile && outFile != null) {
            try {
                out = new FileOutputStream(outFile);
            } catch (java.io.IOException e) {
                throw new BuildException(e);
            }
        }
        r.setOutput(out);
        return r;
    }

    /**
     * <p> Enumerated attribute with the values "plain", "xml" and "brief".
     *
     * <p> Use to enumerate options for <code>type</code> attribute.
     */
    public static class TypeAttribute extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"plain", "xml", "brief"};
        }
    }
}
