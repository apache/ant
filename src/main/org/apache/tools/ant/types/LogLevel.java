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

package org.apache.tools.ant.types;

import org.apache.tools.ant.Project;

/**
 * The enumerated values for Ant's log level.
 */
public class LogLevel extends EnumeratedAttribute {

    /** ERR loglevel constant. */
    public static final LogLevel ERR = new LogLevel("error");

    /** WARN loglevel constant. */
    public static final LogLevel WARN = new LogLevel("warn");

    /** INFO loglevel constant. */
    public static final LogLevel INFO = new LogLevel("info");

    /** VERBOSE loglevel constant. */
    public static final LogLevel VERBOSE = new LogLevel("verbose");

    /** DEBUG loglevel constant. */
    public static final LogLevel DEBUG = new LogLevel("debug");

    /**
     * Public constructor.
     */
    public LogLevel() {
    }

    private LogLevel(String value) {
        this();
        setValue(value);
    }

    /**
     * @see EnumeratedAttribute#getValues
     * @return the strings allowed for the level attribute
     */
    @Override
    public String[] getValues() {
        return new String[] {
            "error",
            "warn",
            "warning",
            "info",
            "verbose",
            "debug"};
    }

    /**
     * mapping of enumerated values to log levels
     */
    private static int[] levels = {
        Project.MSG_ERR,
        Project.MSG_WARN,
        Project.MSG_WARN,
        Project.MSG_INFO,
        Project.MSG_VERBOSE,
        Project.MSG_DEBUG
    };

    /**
     * get the level of the echo of the current value
     * @return the level
     */
    public int getLevel() {
        return levels[getIndex()];
    }
}
