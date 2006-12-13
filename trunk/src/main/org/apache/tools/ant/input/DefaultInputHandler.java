/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;

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
        try {
            r = new BufferedReader(new InputStreamReader(getInputStream()));
            do {
                System.err.println(prompt);
                System.err.flush();
                try {
                    String input = r.readLine();
                    request.setInput(input);
                } catch (IOException e) {
                    throw new BuildException("Failed to read input from"
                                             + " Console.", e);
                }
            } while (!request.isInputValid());
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    throw new BuildException("Failed to close input.", e);
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
            StringBuffer sb = new StringBuffer(prompt);
            sb.append(" (");
            Enumeration e =
                ((MultipleChoiceInputRequest) request).getChoices().elements();
            boolean first = true;
            while (e.hasMoreElements()) {
                if (!first) {
                    sb.append(", ");
                }
                String next = (String) e.nextElement();
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
        return System.in;
    }

}
