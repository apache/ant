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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.KeepAliveInputStream;

/**
 * Prompts on System.err, reads input from System.in
 *
 * @since Ant 1.5
 */
public class DefaultInputHandler implements InputHandler {

    /**
     * Empty no-arg constructor
     */
    public DefaultInputHandler() {
    }

    /**
     * Prompts and requests input.  May loop until a valid input has
     * been entered.
     * @param request the request to handle
     * @throws BuildException if not possible to read from console
     */
    public void handleInput(InputRequest request) throws BuildException {
        String prompt = getPrompt(request);
        BufferedReader r = null;
        boolean success = false;
        try {
            r = new BufferedReader(new InputStreamReader(getInputStream()));
            do {
                System.err.println(prompt);
                System.err.flush();
                try {
                    String input = r.readLine();
                    if (input == null) {
                        throw new BuildException("unexpected end of stream while reading input");
                    }
                    request.setInput(input);
                } catch (IOException e) {
                    throw new BuildException("Failed to read input from"
                                             + " Console.", e);
                }
            } while (!request.isInputValid());
            success = true;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    if (success) { // don't hide inner exception
                        throw new BuildException("Failed to close input.", e); //NOSONAR
                    }
                }
            }
        }
    }

    /**
     * Constructs user prompt from a request.
     *
     * <p>This implementation adds (choice1,choice2,choice3,...) to the
     * prompt for <code>MultipleChoiceInputRequest</code>s.</p>
     *
     * @param request the request to construct the prompt for.
     *                Must not be <code>null</code>.
     * @return the prompt to ask the user
     */
    protected String getPrompt(InputRequest request) {
        String prompt = request.getPrompt();
        String def = request.getDefaultValue();
        if (request instanceof MultipleChoiceInputRequest) {
            StringBuilder sb = new StringBuilder(prompt).append(" (");
            boolean first = true;
            for (String next : ((MultipleChoiceInputRequest) request).getChoices()) {
                if (!first) {
                    sb.append(", ");
                }
                if (next.equals(def)) {
                    sb.append('[');
                }
                sb.append(next);
                if (next.equals(def)) {
                    sb.append(']');
                }
                first = false;
            }
            sb.append(")");
            return sb.toString();
        } else if (def != null) {
            return prompt + " [" + def + "]";
        } else {
            return prompt;
        }
    }

    /**
     * Returns the input stream from which the user input should be read.
     * @return the input stream from which the user input should be read.
     */
    protected InputStream getInputStream() {
        return KeepAliveInputStream.wrapSystemIn();
    }
}
