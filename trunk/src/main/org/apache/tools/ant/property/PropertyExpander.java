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
package org.apache.tools.ant.property;

import org.apache.tools.ant.PropertyHelper;

import java.text.ParsePosition;

/** Interface to a class (normally PropertyHelper) to get a property */
public interface PropertyExpander extends PropertyHelper.Delegate {
    /**
     * Parse the next property name.
     * @param s the String to parse.
     * @param pos the ParsePosition in use.
     * @param parseNextProperty parse next property
     * @return parsed String if any, else <code>null</code>.
     */
    String parsePropertyName(
        String s, ParsePosition pos, ParseNextProperty parseNextProperty);
}

