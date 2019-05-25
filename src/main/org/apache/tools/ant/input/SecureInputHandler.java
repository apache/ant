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

import java.util.Arrays;
import org.apache.tools.ant.BuildException;

/**
 * Prompts and requests input.  May loop until a valid input has
 * been entered. Doesn't echo input.
 * @since Ant 1.7.1
 */
public class SecureInputHandler extends DefaultInputHandler {

    /**
     * Default no-args constructor
     */
    public SecureInputHandler() {
    }

    /**
     * Handle the input
     * @param request the request to handle
     * @throws BuildException if not possible to read from console
     */
    @SuppressWarnings("unused")
    public void handleInput(InputRequest request) throws BuildException {
        String prompt = getPrompt(request);
        do {
            char[] input = System.console().readPassword(prompt);
            if (input == null) {
                throw new BuildException("unexpected end of stream while reading input");
            }
            request.setInput(new String(input));
            Arrays.fill(input, ' ');
        } while (!request.isInputValid());
    }
}
