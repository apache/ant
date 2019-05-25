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

package org.apache.tools.ant.taskdefs.optional.junit;

/**
 * Constants, like filenames shared between various classes in this package.
 */
public class Constants {

    private Constants() {
    }

    static final String METHOD_NAMES = "methods=";
    static final String HALT_ON_ERROR = "haltOnError=";
    static final String HALT_ON_FAILURE = "haltOnFailure=";
    static final String FILTERTRACE = "filtertrace=";
    static final String CRASHFILE = "crashfile=";
    static final String BEFORE_FIRST_TEST = "BeforeFirstTest";
    static final String PROPSFILE = "propsfile=";
    static final String SHOWOUTPUT = "showoutput=";
    static final String OUTPUT_TO_FORMATTERS = "outputtoformatters=";
    static final String FORMATTER = "formatter=";
    static final String LOGTESTLISTENEREVENTS = "logtestlistenerevents=";
    static final String TESTSFILE = "testsfile=";
    static final String TERMINATED_SUCCESSFULLY = "terminated successfully";
    static final String LOG_FAILED_TESTS = "logfailedtests=";
    static final String SKIP_NON_TESTS = "skipNonTests=";
    /** @since Ant 1.9.4 */
    static final String THREADID = "threadid=";
}
