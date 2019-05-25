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
package org.apache.tools.ant;

import java.io.PrintStream;
import java.util.List;

/**
 * Processor of arguments of the command line.
 * <p>
 * Arguments supported by third party code should not conflict with Ant core
 * ones. It is then recommended to chose specific 'enough' argument name,
 * avoiding for instance one letter arguments. By the way, if there any
 * conflict, Ant will take precedence.
 *
 * @since 1.9
 */
public interface ArgumentProcessor {

    /**
     * Read the arguments from the command line at the specified position
     * <p>
     * If the argument is not supported, returns -1. Else, the position of the
     * first argument not supported.
     * </p>
     *
     * @param args String[]
     * @param pos int
     * @return int
     */
    int readArguments(String[] args, int pos);

    /**
     * If some arguments matched with {@link #readArguments(String[], int)},
     * this method is called after all arguments were parsed. Returns
     * <code>true</code> if Ant should stop there, ie the build file not parsed
     * and the project should not be executed.
     *
     * @param args List&lt;String&gt;
     * @return boolean
     */
    boolean handleArg(List<String> args);

    /**
     * If some arguments matched with {@link #readArguments(String[], int)},
     * this method is called just before the project being configured
     *
     * @param project Project
     * @param args List&lt;String&gt;
     */
    void prepareConfigure(Project project, List<String> args);

    /**
     * Handle the arguments with {@link #readArguments(String[], int)}, just
     * after the project being configured. Returns <code>true</code> if Ant
     * should stop there, ie the build file not parsed and the project should
     * not be executed.
     *
     * @param project Project
     * @param arg List&lt;String&gt;
     * @return boolean
     */
    boolean handleArg(Project project, List<String> arg);

    /**
     * Print the usage of the supported arguments
     *
     * @param writer PrintStream
     * @see org.apache.tools.ant.Main#printUsage()
     */
    void printUsage(PrintStream writer);

}
