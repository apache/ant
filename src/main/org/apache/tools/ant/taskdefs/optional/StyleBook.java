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
package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import org.apache.tools.ant.BuildException;


import org.apache.tools.ant.taskdefs.Java;

/**
 * Executes the Apache Stylebook documentation generator.
 * Unlike the commandline version of this tool, all three arguments
 * are required to run stylebook.
 * <p>
 * Being extended from &lt;Java&gt;, all the parent's attributes
 * and options are available. Do not set any apart from the <tt>classpath</tt>
 * as they are not guaranteed to be there in future.
 * @todo stop extending from Java.
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @author <a href="mailto:marcus.boerger@post.rwth-aachen.de">Marcus
 *      B&ouml;rger</a>
 */
public class StyleBook
     extends Java {
    protected File m_targetDirectory;
    protected File m_skinDirectory;
    protected String m_loaderConfig;
    protected File m_book;


    public StyleBook() {
        setClassname("org.apache.stylebook.StyleBook");
        setFork(true);
        setFailonerror(true);
    }

    /**
     * The book xml file that the documentation generation starts from; 
     * required.
     */
     
    public void setBook(final File book) {
        m_book = book;
    }


    /**
     * the directory that contains the stylebook skin;
     * required.
     */
    public void setSkinDirectory(final File skinDirectory) {
        m_skinDirectory = skinDirectory;
    }


    /**
     * the destination directory where the documentation is generated;
     * required.
     */
    public void setTargetDirectory(final File targetDirectory) {
        m_targetDirectory = targetDirectory;
    }

    /**
     * A loader configuration to send to stylebook; optional.
     */
    public void setLoaderConfig(final String loaderConfig) {
        m_loaderConfig = loaderConfig;
    }


    /**
     * call the program
     */
    public void execute()
         throws BuildException {

        if (null == m_targetDirectory) {
            throw new BuildException("TargetDirectory attribute not set.");
        }

        if (null == m_skinDirectory) {
            throw new BuildException("SkinDirectory attribute not set.");
        }

        if (null == m_book) {
            throw new BuildException("book attribute not set.");
        }

        createArg().setValue("targetDirectory=" + m_targetDirectory);
        createArg().setValue(m_book.toString());
        createArg().setValue(m_skinDirectory.toString());
        if (null != m_loaderConfig) {
            createArg().setValue("loaderConfig=" + m_loaderConfig);
        }

        super.execute();
    }
}

