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
package org.apache.ant.taskdef.io;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.ant.common.task.AbstractTask;
import org.apache.ant.common.task.TaskException;
import org.apache.ant.common.util.MessageLevel;

/**
 * Basic Echo Tast for testing
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 16 January 2002
 */
public class Echo extends AbstractTask {
    /** The message to be echoed */
    private String message = "";
    /** the file to which output is sent if any */
    private File file = null;
    /** indicates if the fileoutput is to be appended to an existing file */
    private boolean append = false;

    // by default, messages are always displayed
    /** the log level to be used when echoing - defaults to Warning level */
    private int logLevel = MessageLevel.MSG_WARN;

    /**
     * Sets the message variable.
     *
     * @param msg Sets the value for the message variable.
     */
    public void setMessage(String msg) {
        this.message = msg;
    }

    /**
     * Set the file to which output is to be sent
     *
     * @param file the new file value
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Indicate if output is to be appended to the file
     *
     * @param append true if output should be appended
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * Does the work.
     *
     * @throws TaskException if cannot output message
     */
    public void execute() throws TaskException {
        if (file == null) {
            log(message, logLevel);
        } else {
            FileWriter out = null;
            try {
                out = new FileWriter(file.getAbsolutePath(), append);
                out.write(message, 0, message.length());
            } catch (IOException ioe) {
                throw new TaskException(ioe);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ioex) {
                    }
                }
            }
        }
    }

    /**
     * Set a multiline message.
     *
     * @param msg the message
     */
    public void addText(String msg) {
        message += msg;
    }

    /**
     * testing only
     *
     * @param frame testing
     */
    public void addFrame(java.awt.Frame frame) {
        log("Adding frame " + frame, MessageLevel.MSG_WARN);
    }

    /**
     * testing
     *
     * @param runnable testing
     */
    public void addRun(Runnable runnable) {
        log("Adding runnable of type "
             + runnable.getClass().getName(), MessageLevel.MSG_WARN);
    }

}

