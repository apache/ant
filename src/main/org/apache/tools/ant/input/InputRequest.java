/*
 * Copyright  2002,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

/**
 * Encapsulates an input request.
 *
 * @author Stefan Bodewig
 * @version $Revision$
 * @since Ant 1.5
 */
public class InputRequest {
    private String prompt;
    private String input;

    /**
     * @param prompt The prompt to show to the user.  Must not be null.
     */
    public InputRequest(String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt must not be null");
        }

        this.prompt = prompt;
    }

    /**
     * Retrieves the prompt text.
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets the user provided input.
     */
    public void setInput(String input) {
        this.input = input;
    }

    /**
     * Is the user input valid?
     */
    public boolean isInputValid() {
        return true;
    }

    /**
     * Retrieves the user input.
     */
    public String getInput() {
        return input;
    }

}
