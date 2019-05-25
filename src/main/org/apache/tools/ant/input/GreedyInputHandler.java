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

package org.apache.tools.ant.input;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.StreamPumper;
import org.apache.tools.ant.util.FileUtils;

/**
 * Prompts on System.err, reads input from System.in until EOF
 *
 * @since Ant 1.7
 */
public class GreedyInputHandler extends DefaultInputHandler {

    /**
     * Empty no-arg constructor
     */
    public GreedyInputHandler() {
    }

    /**
     * Prompts and requests input.
     * @param request the request to handle
     * @throws BuildException if not possible to read from console,
     *         or if input is invalid.
     */
    public void handleInput(InputRequest request) throws BuildException {
        String prompt = getPrompt(request);
        InputStream in = null;
        try {
            in = getInputStream();
            System.err.println(prompt);
            System.err.flush();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamPumper p = new StreamPumper(in, baos);
            Thread t = new Thread(p);
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                try {
                    t.join();
                } catch (InterruptedException e2) {
                    // Ignore
                }
            }
            request.setInput(new String(baos.toByteArray()));
            if (!request.isInputValid()) {
                throw new BuildException(
                    "Received invalid console input");
            }
            if (p.getException() != null) {
                throw new BuildException(
                    "Failed to read input from console", p.getException());
            }
        } finally {
            FileUtils.close(in);
        }
    }
}
