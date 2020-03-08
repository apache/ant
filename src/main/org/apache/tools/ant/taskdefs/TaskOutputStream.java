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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.ant.Task;

/**
 * Redirects text written to a stream through the standard
 * Ant logging mechanism. This class is useful for integrating
 * with tools that write to System.out and System.err. For example,
 * the following will cause all text written to System.out to be
 * logged with "info" priority:
 * <pre>System.setOut(new PrintStream(new TaskOutputStream(project, Project.MSG_INFO)));</pre>
 *
 * <p><strong>As of Ant 1.2, this class is considered to be dead code
 * by the Ant developers and is unmaintained.  Don't use
 * it.</strong></p>
 *
 * @deprecated since 1.2.x.
 * Use LogOutputStream instead.
 */
@Deprecated
public class TaskOutputStream extends OutputStream {

    private Task task;
    private StringBuffer line;
    private int msgOutputLevel;

    /**
     * Constructs a new JavacOutputStream with the given project
     * as the output source for messages.
     */

    TaskOutputStream(Task task, int msgOutputLevel) {
        System.err.println("As of Ant 1.2 released in October 2000, the "
            + "TaskOutputStream class");
        System.err.println("is considered to be dead code by the Ant "
            + "developers and is unmaintained.");
        System.err.println("Don't use it!");

        this.task = task;
        this.msgOutputLevel = msgOutputLevel;

        line = new StringBuffer();
    }

    /**
     * Write a character to the output stream. This method looks
     * to make sure that there isn't an error being reported and
     * will flush each line of input out to the project's log stream.
     * @param c the character to write
     * @throws IOException on error
     */

    public void write(int c) throws IOException {
        char cc = (char) c;
        if (cc == '\r' || cc == '\n') {
            // line feed
            if (line.length() > 0) {
                processLine();
            }
        } else {
            line.append(cc);
        }
    }

    /**
     * Processes a line of input and determines if an error occurred.
     */

    private void processLine() {
        String s = line.toString();
        task.log(s, msgOutputLevel);
        line = new StringBuffer();
    }
}

