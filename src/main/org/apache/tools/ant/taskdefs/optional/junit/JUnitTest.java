/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.Project;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/**
 * <p> Run a single JUnit test.
 *
 * <p> The JUnit test is actually run by {@link JUnitTestRunner}.
 * So read the doc comments for that class :)
 *
 * @author Thomas Haas
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>,
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 *
 * @since Ant 1.2
 *
 * @see JUnitTask
 * @see JUnitTestRunner
 */
public class JUnitTest extends BaseTest implements Cloneable {
    
    /** the name of the test case */
    private String name = null;
    
    /** the name of the result file */
    private String outfile = null;

    // @todo this is duplicating TestResult information. Only the time is not
    // part of the result. So we'd better derive a new class from TestResult
    // and deal with it. (SB)
    private long runs, failures, errors;
    private long runTime;

    // Snapshot of the system properties
    private Properties props = null;

    public JUnitTest() {
    }

    public JUnitTest(String name) {
        this.name  = name;
    }

    public JUnitTest(String name, boolean haltOnError, boolean haltOnFailure, 
                     boolean filtertrace) {
        this.name  = name;
        this.haltOnError = haltOnError;
        this.haltOnFail = haltOnFailure;
        this.filtertrace = filtertrace;
    }

    /** 
     * Set the name of the test class.
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Set the name of the output file.
     */
    public void setOutfile(String value) {
        outfile = value;
    }

    /** 
     * Get the name of the test class.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of the output file
     * 
     * @return the name of the output file.
     */
    public String getOutfile() {
        return outfile;
    }

    public void setCounts(long runs, long failures, long errors) {
        this.runs = runs;
        this.failures = failures;
        this.errors = errors;
    }

    public void setRunTime(long runTime) {
        this.runTime = runTime;
    }

    public long runCount() {
        return runs;
    }
    
    public long failureCount() {
        return failures;
    }
    
    public long errorCount() {
        return errors;
    }
    
    public long getRunTime() {
        return runTime;
    }

    public Properties getProperties() {
        return props;
    }
    
    public void setProperties(Hashtable p) { 
        props = new Properties();  
        for (Enumeration enum = p.keys(); enum.hasMoreElements();) {
            Object key = enum.nextElement();
            props.put(key, p.get(key));
        }
    }

    public boolean shouldRun(Project p) {
        if (ifProperty != null && p.getProperty(ifProperty) == null) {
            return false;
        } else if (unlessProperty != null && 
                   p.getProperty(unlessProperty) != null) {
            return false;
        }

        return true;
    }

    public FormatterElement[] getFormatters() {
        FormatterElement[] fes = new FormatterElement[formatters.size()];
        formatters.copyInto(fes);
        return fes;
    }

    /**
     * Convenient method to add formatters to a vector
     */
    void addFormattersTo(Vector v){
        final int count = formatters.size();
        for (int i = 0; i < count; i++){
            v.addElement(formatters.elementAt(i));
        }
    }

    /**
     * @since Ant 1.5
     */
    public Object clone() {
        try {
            JUnitTest t = (JUnitTest) super.clone();
            t.props = props == null ? null : (Properties) props.clone();
            t.formatters = (Vector) formatters.clone();
            return t;
        } catch (CloneNotSupportedException e) {
            // plain impossible
            return this;
        }
    }
}
