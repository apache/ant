/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.*;

/**
 * Interface used by Ant to log the build output. 
 *
 * A build logger is a build listener which has the 'right' to send output to the
 * ant log, which is usually System.out unles redirected by the -logfile option.
 *
 * @author Conor MacNeill
 */
public interface BuildLogger extends BuildListener {
    /**
     * Set the msgOutputLevel this logger is to respond to.
     *
     * Only messages with a message level lower than or equal to the given level are 
     * output to the log.
     * <P>
     * Constants for the message levels are in Project.java. The order of
     * the levels, from least to most verbose, is MSG_ERR, MSG_WARN,
     * MSG_INFO, MSG_VERBOSE, MSG_DEBUG.
     *
     * @param level the logging level for the logger.
     */
    public void setMessageOutputLevel(int level);
    
    /**
     * Set the output stream to which this logger is to send its output.
     *
     * @param output the output stream for the logger.
     */
    public void setOutputPrintStream(PrintStream output);
    
    /**
     * Set this logger to produce emacs (and other editor) friendly output.
     *
     * @param emacsMode true if output is to be unadorned so that emacs and other
     * editors can parse files names, etc.
     */
    public void setEmacsMode(boolean emacsMode);

    /**
     * Set the output stream to which this logger is to send error messages.
     *
     * @param err the error stream for the logger.
     */
    public void setErrorPrintStream(PrintStream err);
    
}
