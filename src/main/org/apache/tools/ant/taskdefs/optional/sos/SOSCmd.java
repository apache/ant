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
package org.apache.tools.ant.taskdefs.optional.sos;

/**
 * Interface to hold constants used by the SOS tasks
 *
 */
// CheckStyle:InterfaceIsTypeCheck OFF (bc)
public interface SOSCmd {
    // soscmd Command options
    /** The sos executable */
    String COMMAND_SOS_EXE = "soscmd";
    /** The get file command */
    String COMMAND_GET_FILE = "GetFile";
    /** The get project command */
    String COMMAND_GET_PROJECT = "GetProject";
    /** The checkout file command */
    String COMMAND_CHECKOUT_FILE = "CheckOutFile";
    /** The checkout project command */
    String COMMAND_CHECKOUT_PROJECT = "CheckOutProject";
    /** The checkin file command */
    String COMMAND_CHECKIN_FILE = "CheckInFile";
    /** The checkin project command */
    String COMMAND_CHECKIN_PROJECT = "CheckInProject";
    /** The get history command */
    String COMMAND_HISTORY = "GetFileHistory";
    /** The add label command */
    String COMMAND_LABEL = "AddLabel";
    /** The project prefix */
    String PROJECT_PREFIX = "$";

    // soscmd Option flags
    /** The command option */
    String FLAG_COMMAND = "-command";
    /** The database (vss server) option */
    String FLAG_VSS_SERVER = "-database";
    /** The username option */
    String FLAG_USERNAME = "-name";
    /** The password option */
    String FLAG_PASSWORD = "-password"; //NOSONAR
    /** The log option */
    String FLAG_COMMENT = "-log";
    /** The workdir option */
    String FLAG_WORKING_DIR = "-workdir";
    /** The recursive option */
    String FLAG_RECURSION = "-recursive";
    /** The revision option */
    String FLAG_VERSION = "-revision";
    /** The label option */
    String FLAG_LABEL = "-label";
    /** The no compression option */
    String FLAG_NO_COMPRESSION = "-nocompress";
    /** The no cache option */
    String FLAG_NO_CACHE = "-nocache";
    /** The server option */
    String FLAG_SOS_SERVER = "-server";
    /** The sos home option */
    String FLAG_SOS_HOME = "-soshome";
    /** The project option */
    String FLAG_PROJECT = "-project";
    /** The file option */
    String FLAG_FILE = "-file";
    /** The verbose option */
    String FLAG_VERBOSE = "-verbose";
}
