/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.rjunit.formatter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.rjunit.KeepAliveOutputStream;
import org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunEvent;

/**
 * Base formatter providing default implementation to deal with
 * either stdout or a file.
 * <p>
 * The file is specified by initializing the formatter with
 * a filepath mapped by the key 'file'.
 * </p>
 * <p>
 * if no file key exists in the properties, it defaults to stdout.
 * </p>
 *
 */
public class BaseStreamFormatter extends BaseFormatter {

    /** the key used to specifiy a filepath */
    public final static String FILE_KEY = "file";

    /** writer to output the data to */
    private PrintWriter writer;

    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    public void init(Properties props) throws BuildException {
        String file = props.getProperty(FILE_KEY);
        OutputStream os = null;
        if (file != null) {
            try {
                // fixme need to resolve the file !!!!
                os = new FileOutputStream(file);
            } catch (IOException e) {
                throw new BuildException(e);
            }
        } else {
            os = new KeepAliveOutputStream(System.out);
        }
        setOutput(os);
    }

    /**
     * Helper method to wrap the stream over an UTF8 buffered writer.
     */
    protected void setOutput(OutputStream value) {
        try {
            // do not buffer but flush each line.
            writer = new PrintWriter(new OutputStreamWriter(value, "UTF8"), true);
        } catch (IOException e) {
            // should not happen
            throw new BuildException(e);
        }
    }

    public void onRunEnded(TestRunEvent evt) {
        close();
    }

    protected void close() {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }

    /**
     * @return the writer used to print data.
     */
    protected final PrintWriter getWriter() {
        return writer;
    }

}
