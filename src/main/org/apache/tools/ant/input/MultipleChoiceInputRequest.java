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

import java.util.Vector;

/**
 * Encapsulates an input request.
 *
 * @since Ant 1.5
 */
public class MultipleChoiceInputRequest extends InputRequest {
    private Vector choices = new Vector();

    /**
     * @param prompt The prompt to show to the user.  Must not be null.
     * @param choices holds all input values that are allowed.
     *                Must not be null.
     */
    public MultipleChoiceInputRequest(String prompt, Vector choices) {
        super(prompt);
        if (choices == null) {
            throw new IllegalArgumentException("choices must not be null");
        }
        this.choices = choices;
    }

    /**
     * @return The possible values.
     */
    public Vector getChoices() {
        return choices;
    }

    /**
     * @return true if the input is one of the allowed values.
     */
    public boolean isInputValid() {
        return choices.contains(getInput()) || ("".equals(getInput()) && getDefaultValue() != null);
    }
}
