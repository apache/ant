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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.filters.util.ChainReaderHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Vector;

/**
 * Load a file into a property
 *
 * @author Steve Loughran
 * @author Magesh Umasankar
 * @since Ant 1.5
 * @ant.task category="utility"
 */
public final class LoadFile extends Task {

    /**
     * source file, usually null
     */
    private File srcFile = null;

    /**
     * what to do when it goes pear-shaped
     */
    private boolean failOnError = true;

    /**
     * Encoding to use for filenames, defaults to the platform's default
     * encoding.
     */
    private String encoding = null;

    /**
     * name of property
     */
    private String property = null;

    /**
     * Holds FilterChains
     */
    private final Vector filterChains = new Vector();

    /**
     * Encoding to use for input, defaults to the platform's default
     * encoding. <p>
     *
     * For a list of possible values see <a href="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">
     * http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html
     * </a>.</p>
     *
     * @param encoding The new Encoding value
     */

    public final void setEncoding(final String encoding) {
        this.encoding = encoding;
    }


    /**
     * Property name to save to.
     *
     * @param property The new Property value
     */
    public final void setProperty(final String property) {
        this.property = property;
    }


    /**
     * Sets the file to load.
     *
     * @param srcFile The new SrcFile value
     */
    public final void setSrcFile(final File srcFile) {
        this.srcFile = srcFile;
    }


    /**
     * If true, fail on load error.
     *
     * @param fail The new Failonerror value
     */
    public final void setFailonerror(final boolean fail) {
        failOnError = fail;
    }


    /**
     * read in a source file to a property
     *
     * @exception BuildException if something goes wrong with the build
     */
    public final void execute()
        throws BuildException {
        //validation
        if (srcFile == null) {
            throw new BuildException("source file not defined");
        }
        if (property == null) {
            throw new BuildException("output property not defined");
        }
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        Reader instream = null;
        log("loading " + srcFile + " into property " + property,
            Project.MSG_VERBOSE);
        try {
            final long len = srcFile.length();
            log("file size = " + len, Project.MSG_DEBUG);
            //discard most of really big files
            final int size = (int) len;
            //open up the file
            fis = new FileInputStream(srcFile);
            bis = new BufferedInputStream(fis);
            if (encoding == null) {
                instream = new InputStreamReader(bis);
            } else {
                instream = new InputStreamReader(bis, encoding);
            }

            String text = "";
            if (size != 0) {
                ChainReaderHelper crh = new ChainReaderHelper();
                crh.setBufferSize(size);
                crh.setPrimaryReader(instream);
                crh.setFilterChains(filterChains);
                crh.setProject(project);
                instream = crh.getAssembledReader();
    
                text = crh.readFully(instream);
            }

            if (text != null) {
                if (text.length() > 0) {
                    project.setNewProperty(property, text);
                    log("loaded " + text.length() + " characters",
                        Project.MSG_VERBOSE);
                    log(property + " := " + text, Project.MSG_DEBUG);
                }
            }

        } catch (final IOException ioe) {
            final String message = "Unable to load file: " + ioe.toString();
            if (failOnError) {
                throw new BuildException(message, ioe, location);
            } else {
                log(message, Project.MSG_ERR);
            }
        } catch (final BuildException be) {
            if (failOnError) {
                throw be;
            } else {
                log(be.getMessage(), Project.MSG_ERR);
            }
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ioex) {
                //ignore
            }
        }
    }

    /**
     * Add the FilterChain element.
     */
    public final void addFilterChain(FilterChain filter) {
        filterChains.addElement(filter);
    }

//end class
}
