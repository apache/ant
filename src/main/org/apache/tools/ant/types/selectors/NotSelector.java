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

package org.apache.tools.ant.types.selectors;


/**
 * This selector has one other selectors whose meaning it inverts. It
 * actually relies on NoneSelector for its implementation of the
 * isSelected() method, but it adds a check to ensure there is only one
 * other selector contained within.
 *
 * @since 1.5
 */
public class NotSelector extends NoneSelector {

    /**
     * Default constructor.
     */
    public NotSelector() {
    }

    /**
     * Constructor that inverts the meaning of its argument.
     * @param other the selector to invert
     * @since Ant 1.7
     */
    public NotSelector(FileSelector other) {
        this();
        appendSelector(other);
    }

    /**
     * @return a string representation of the selector
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (hasSelectors()) {
            buf.append("{notselect: ");
            buf.append(super.toString());
            buf.append("}");
        }
        return buf.toString();
    }

    /**
     * Makes sure that there is only one entry, sets an error message if
     * not.
     */
    public void verifySettings() {
        if (selectorCount() != 1) {
            setError(
                "One and only one selector is allowed within the <not> tag");
        }
    }

}

