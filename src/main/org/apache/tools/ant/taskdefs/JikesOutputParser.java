/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Parses output from jikes and
 * passes errors and warnings
 * into the right logging channels of Project.
 *
 * <p><strong>As of Ant 1.2, this class is considered to be dead code
 * by the Ant developers and is unmaintained.  Don't use
 * it.</strong></p>
 *
 * @deprecated since 1.2.
 *             Use Jikes' exit value to detect compilation failure.
 */
@Deprecated
public class JikesOutputParser implements ExecuteStreamHandler {
    // CheckStyle:VisibilityModifier OFF - bc
    protected Task task;
    protected boolean errorFlag = false; // no errors so far
    protected int errors;
    protected int warnings;
    protected boolean error = false;
    protected boolean emacsMode;

    protected BufferedReader br;
    // CheckStyle:VisibilityModifier ON

    /**
     * Ignore.
     * @param os ignored
     */
    public void setProcessInputStream(OutputStream os) {
    }

    /**
     * Ignore.
     * @param is ignored
     */
    public void setProcessErrorStream(InputStream is) {
    }

    /**
     * Set the inputstream
     * @param is the input stream
     * @throws IOException on error
     */
    public void setProcessOutputStream(InputStream is) throws IOException {
        br = new BufferedReader(new InputStreamReader(is));
    }

    /**
     * Invokes parseOutput.
     * @throws IOException on error
     */
    public void start() throws IOException {
        parseOutput(br);
    }

    /**
     * Ignore.
     */
    public void stop() {
    }

    /**
     * Construct a new Parser object
     * @param task      task in which context we are called
     * @param emacsMode if true output in emacs mode
     */
    protected JikesOutputParser(Task task, boolean emacsMode) {
        super();

        System.err.println("As of Ant 1.2 released in October 2000, the "
            + "JikesOutputParser class");
        System.err.println("is considered to be dead code by the Ant "
            + "developers and is unmaintained.");
        System.err.println("Don't use it!");

        this.task = task;
        this.emacsMode = emacsMode;
    }

    /**
     * Parse the output of a jikes compiler
     * @param reader - Reader used to read jikes's output
     * @throws IOException on error
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
        // We assume, that every output, jikes does, stands for an error/warning
        // TODO
        // Is this correct?

        // TODO:
        // A warning line, that shows code, which contains a variable
        // error will cause some trouble. The parser should definitely
        // be much better.

        while ((line = reader.readLine()) != null) {
            lower = line.toLowerCase();
            if (line.trim().isEmpty()) {
                continue;
            }
            if (lower.contains("error")) {
                setError(true);
            } else if (lower.contains("warning")) {
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
     * @return if errors occurred
     */
    protected boolean getErrorFlag() {
        return errorFlag;
    }
}
