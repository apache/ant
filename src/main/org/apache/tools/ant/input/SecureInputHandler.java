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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.ReflectUtil;

/**
 * Prompts and requests input.  May loop until a valid input has
 * been entered. Doesn't echo input (requires Java6). If Java6 is not
 * available, falls back to the DefaultHandler (insecure).
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
    public void handleInput(InputRequest request) throws BuildException {
        String prompt = getPrompt(request);
        try {
            Object console = ReflectUtil.invokeStatic(System.class, "console");
            do {
                char[] input = (char[]) ReflectUtil.invoke(
                    console, "readPassword", String.class, prompt,
                    Object[].class, (Object[]) null);
                request.setInput(new String(input));
                /* for security zero char array after retrieving value */
                java.util.Arrays.fill(input, ' ');
            } while (!request.isInputValid());
        } catch (Exception e) {
            /* Java6 not present use default handler */
            super.handleInput(request);
        }
    }
}