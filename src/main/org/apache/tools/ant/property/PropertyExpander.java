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
package org.apache.tools.ant.property;

import java.text.ParsePosition;

import org.apache.tools.ant.PropertyHelper;

/**
 * Responsible for locating a property reference inside a String.
 * @since Ant 1.8.0
 */
public interface PropertyExpander extends PropertyHelper.Delegate {

    /**
     * Determine whether there is a property reference at the current
     * ParsePosition and return its name (or null if there is none).
     *
     * <p>Implementations should advance the ParsePosition to the last
     * character that makes up the property reference.  E.g. the
     * default implementation would return <code>"foo"</code> for
     * <code>${foo}</code> and advance the ParsePosition to the
     * <code>}</code> character.</p>
     *
     * @param s the String to parse.
     * @param pos the ParsePosition in use, the location is expected
     * to be modified if a property reference has been found (and may
     * even be modified if no reference has been found).
     * @param parseNextProperty provides access to the Project and may
     * be used to look up property values.
     * @return property name if any, else <code>null</code>.
     */
    String parsePropertyName(String s, ParsePosition pos,
                             ParseNextProperty parseNextProperty);
}

