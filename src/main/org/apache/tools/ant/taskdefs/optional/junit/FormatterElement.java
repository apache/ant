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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.junit;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * <p> A wrapper for the implementations of <code>JUnitResultFormatter</code>.
 * In particular, used as a nested <code>&lt;formatter&gt;</code> element in a <code>&lt;junit&gt;</code> task.
 * <p> For example, 
 * <code><pre>
 *       &lt;junit printsummary="no" haltonfailure="yes" fork="false"&gt;
 *           &lt;formatter type="plain" usefile="false" /&gt;
 *           &lt;test name="org.apache.ecs.InternationalCharTest" /&gt;
 *       &lt;/junit&gt;</pre></code>
 * adds a <code>plain</code> type implementation (<code>PlainJUnitResultFormatter</code>) to display the results of the test.
 *
 * <p> Either the <code>type</code> or the <code>classname</code> attribute
 * must be set. 
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
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
     * <p> Sets <code>classname</code> attribute - so you can't use that attribute if you use this one.
     */
    public void setType(TypeAttribute type) {
        if ("xml".equals(type.getValue())) {
            setClassname("org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter");
            setExtension(".xml");
        } else {
            if ("brief".equals(type.getValue())) {
                setClassname("org.apache.tools.ant.taskdefs.optional.junit.BriefJUnitResultFormatter");
            } else { // must be plain, ensured by TypeAttribute
                setClassname("org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter");
            }
            setExtension(".txt");
        }
    }

    /**
     * <p> Set name of class to be used as the formatter.
     *
     * <p> This class must implement <code>JUnitResultFormatter</code>
     */
    public void setClassname(String classname) {
        this.classname = classname;
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

    JUnitResultFormatter createFormatter() throws BuildException {
        if (classname == null) {
            throw new BuildException("you must specify type or classname");
        }
        
        Class f = null;
        try {
            f = Class.forName(classname);
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
