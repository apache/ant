/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.formatter.BriefFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.formatter.Formatter;
import org.apache.tools.ant.taskdefs.optional.junit.formatter.KeepAliveOutputStream;
import org.apache.tools.ant.taskdefs.optional.junit.formatter.XMLFormatter;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * An element representing a <tt>Formatter</tt>
 *
 * <pre>
 * <!ELEMENT formatter (filter)*>
 * <!ATTLIST formatter type (plain|xml|brief) #REQUIRED>
 * <!ATTLIST formatter classname CDATA #REQUIRED>
 * <!ATTLIST formatter extension CDATA #IMPLIED>
 * <!ATTLIST formatter usefile (yes|no) no>
 * </pre>
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 *
 * @see JUnitTask,
 * @see Formatter
 */
public class FormatterElement {

    private OutputStream out = new KeepAliveOutputStream(System.out);
    private String classname;

    /**
     * @fixme we can remove this and use a specific attribute
     * to denote a filepathname and stdout as a reserved word.
     */
    private String extension;

    /** are we using a file ? */
    private File outFile;
    private boolean useFile = true;

    /** the filters to apply to this formatter */
    private Vector filters = new Vector();

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
        setClassname(type.getClassName());
        setExtension(type.getExtension());
    }

    /**
     * <p> Set name of class to be used as the formatter.
     *
     * <p> This class must implement <code>Formatter</code>
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
     * Setting a comma separated list of filters in the specified order.
     * @see #addFilter(FilterAttribute)
     * @see FilterAttribute
     */
    public void setFilters(String filters) {
        StringTokenizer st = new StringTokenizer(filters, ",");
        while (st.hasMoreTokens()) {
            FilterElement fe = new FilterElement();
            FilterElement.FilterAttribute fa = new FilterElement.FilterAttribute();
            fa.setValue(st.nextToken());
            fe.setType(fa);
            addFilter(fe);
        }
    }

    /**
     * Add a filter to this formatter.
     */
    public void addFilter(FilterElement fe) {
        filters.addElement(fe);
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
     * create the Formatter corresponding to this element.
     */
    protected Formatter createFormatter() throws BuildException {
        if (classname == null) {
            throw new BuildException("you must specify type or classname");
        }
        Formatter f = null;
        try {
            Class clazz = Class.forName(classname);
            if (!Formatter.class.isAssignableFrom(clazz)) {
                throw new BuildException(clazz + " is not a JUnitResultFormatter");
            }
            f = (Formatter) clazz.newInstance();

            // create the stream if necessary
            if (useFile && outFile != null) {
                out = new FileOutputStream(outFile);
            }
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException(e);
        }

        f.setOutput(out);

        // wrap filters in the reverse order: first = top, last = bottom.
        for (int i = filters.size() - 1; i >= 0; i--) {
            FilterElement fe = (FilterElement) filters.elementAt(i);
            f = fe.createFilterFormatter(f);
        }

        return f;
    }

    /**
     * <p> Enumerated attribute with the values "plain", "xml" and "brief".
     * <p> Use to enumerate options for <code>type</code> attribute.
     */
    public static class TypeAttribute extends EnumeratedAttribute {
        private final static String[] VALUES = {"plain", "xml", "brief"};
        private final static String[] CLASSNAMES = {"xxx", XMLFormatter.class.getName(), BriefFormatter.class.getName()};
        private final static String[] EXTENSIONS = {".txt", ".xml", ".txt"};

        public String[] getValues() {
            return VALUES;
        }

        public String getClassName() {
            return CLASSNAMES[index];
        }

        public String getExtension() {
            return EXTENSIONS[index];
        }
    }

}

