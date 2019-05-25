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

/**
 * Target executor abstraction.
 * @since Ant 1.6.3
 */
public interface Executor {

    /**
     * Execute the specified Targets for the specified Project.
     * @param project       the Ant Project.
     * @param targetNames   String[] of Target names as specified on the command line.
     * @throws BuildException on error
     */
    void executeTargets(Project project, String[] targetNames)
        throws BuildException;

    /**
     * Get the appropriate subproject Executor instance.
     *
     * This allows the top executor to control what type of executor is used to execute
     * subprojects via &lt;ant&gt;/&lt;antcall&gt;/&lt;subant&gt; and task that extend these.
     * All bundled Executors return a SingleCheckExecutor (running a merged set of
     * depended targets for all targets called) to run sub-builds.
     *
     * @return an Executor instance.
     */
    Executor getSubProjectExecutor();

}
