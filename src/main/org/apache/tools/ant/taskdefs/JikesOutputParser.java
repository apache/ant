/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
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
    
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Parses output from jikes and
 * passes errors and warnings
 * into the right logging channels of Project.
 *
 * <p><strong>As of Ant 1.2, this class is considered to be dead code
 * by the Ant developers and is unmaintained.  Don't use
 * it.</strong></p>
 *
 * @author skanthak@muehlheim.de
 * @deprecated use Jikes' exit value to detect compilation failure.
 */
public class JikesOutputParser implements ExecuteStreamHandler {
    protected Task task;
    protected boolean errorFlag = false; // no errors so far
    protected int errors;
    protected int warnings;
    protected boolean error = false;
    protected boolean emacsMode;
    
    protected BufferedReader br;

    /**
     * Ignore.
     */
    public void setProcessInputStream(OutputStream os) {}

    /**
     * Ignore.
     */
    public void setProcessErrorStream(InputStream is) {}

    /**
     * Set the inputstream
     */
    public void setProcessOutputStream(InputStream is) throws IOException {
        br = new BufferedReader(new InputStreamReader(is));
    }

    /**
     * Invokes parseOutput.
     */
    public void start() throws IOException {
        parseOutput(br);
    }

    /**
     * Ignore.
     */
    public void stop() {}

    /**
     * Construct a new Parser object
     * @param task - task in whichs context we are called
     */
    protected JikesOutputParser(Task task, boolean emacsMode) {
        super();

        System.err.println("As of Ant 1.2 released in October 2000, the " 
            + "JikesOutputParser class");
        System.err.println("is considered to be dead code by the Ant " 
            + "developers and is unmaintained.");
        System.err.println("Don\'t use it!");

        this.task = task;
        this.emacsMode = emacsMode;
    }

    /**
     * Parse the output of a jikes compiler
     * @param reader - Reader used to read jikes's output
     */
    protected void parseOutput(BufferedReader reader) throws IOException {
       if (emacsMode) {
           parseEmacsOutput(reader);
       } else {
           parseStandardOutput(reader);
       }
    }

    private void parseStandardOutput(BufferedReader reader) throws IOException {
        String line;
        String lower;
        // We assume, that every output, jike does, stands for an error/warning
        // XXX 
        // Is this correct?
        
        // TODO:
        // A warning line, that shows code, which contains a variable
        // error will cause some trouble. The parser should definitely
        // be much better.

        while ((line = reader.readLine()) != null) {
            lower = line.toLowerCase();
            if (line.trim().equals("")) {
                continue;
            }
            if (lower.indexOf("error") != -1) {
                setError(true);
            } else if (lower.indexOf("warning") != -1) {
                setError(false);
                   } else {
                // If we don't know the type of the line
                // and we are in emacs mode, it will be
                // an error, because in this mode, jikes won't
                // always print "error", but sometimes other
                // keywords like "Syntax". We should look for
                // all those keywords.
                if (emacsMode) {
                    setError(true);
                }
            }
            log(line);
        }
    }

    private void parseEmacsOutput(BufferedReader reader) throws IOException {
       // This may change, if we add advanced parsing capabilities.
       parseStandardOutput(reader);
    }

    private void setError(boolean err) {
        error = err;
        if (error) {
            errorFlag = true;
        }
    }

    private void log(String line) {
       if (!emacsMode) {
           task.log("", (error ? Project.MSG_ERR : Project.MSG_WARN));
       }
       task.log(line, (error ? Project.MSG_ERR : Project.MSG_WARN));
    }

    /**
     * Indicate if there were errors during the compile
     * @return if errors ocured
     */
    protected boolean getErrorFlag() {
        return errorFlag;
    }
}
