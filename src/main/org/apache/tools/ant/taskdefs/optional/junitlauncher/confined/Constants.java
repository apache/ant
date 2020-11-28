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

package org.apache.tools.ant.taskdefs.optional.junitlauncher.confined;

/**
 * Constants used within the junitlauncher task
 */
public final class Constants {

    public static final int FORK_EXIT_CODE_SUCCESS = 0;
    public static final int FORK_EXIT_CODE_EXCEPTION = 1;
    public static final int FORK_EXIT_CODE_TESTS_FAILED = 2;
    public static final int FORK_EXIT_CODE_TIMED_OUT = 3;

    public static final String ARG_PROPERTIES = "--properties";
    public static final String ARG_LAUNCH_DEFINITION = "--launch-definition";


    public static final String LD_XML_ELM_LAUNCH_DEF = "launch-definition";
    public static final String LD_XML_ELM_TEST = "test";
    public static final String LD_XML_ELM_TEST_CLASSES = "test-classes";
    public static final String LD_XML_ATTR_HALT_ON_FAILURE = "haltOnFailure";
    public static final String LD_XML_ATTR_INCLUDE_TAGS = "includeTags";
    public static final String LD_XML_ATTR_EXCLUDE_TAGS = "excludeTags";
    public static final String LD_XML_ATTR_OUTPUT_DIRECTORY = "outDir";
    public static final String LD_XML_ATTR_INCLUDE_ENGINES = "includeEngines";
    public static final String LD_XML_ATTR_EXCLUDE_ENGINES = "excludeEngines";
    public static final String LD_XML_ATTR_CLASS_NAME = "classname";
    public static final String LD_XML_ATTR_METHODS = "methods";
    public static final String LD_XML_ATTR_PRINT_SUMMARY = "printSummary";
    public static final String LD_XML_ELM_LISTENER = "listener";
    public static final String LD_XML_ATTR_SEND_SYS_ERR = "sendSysErr";
    public static final String LD_XML_ATTR_SEND_SYS_OUT = "sendSysOut";
    public static final String LD_XML_ATTR_LISTENER_RESULT_FILE = "resultFile";
    public static final String LD_XML_ATTR_LISTENER_USE_LEGACY_REPORTING_NAME = "useLegacyReportingName";


    private Constants() {

    }
}
