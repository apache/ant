/*
 * Copyright  2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional.sos;

/**
 * Interface to hold constants used by the SOS tasks
 *
 */
public interface SOSCmd {
    // soscmd Command options
    String COMMAND_SOS_EXE = "soscmd";
    String COMMAND_GET_FILE = "GetFile";
    String COMMAND_GET_PROJECT = "GetProject";
    String COMMAND_CHECKOUT_FILE = "CheckOutFile";
    String COMMAND_CHECKOUT_PROJECT = "CheckOutProject";
    String COMMAND_CHECKIN_FILE = "CheckInFile";
    String COMMAND_CHECKIN_PROJECT = "CheckInProject";
    String COMMAND_HISTORY = "GetFileHistory";
    String COMMAND_LABEL = "AddLabel";
    String PROJECT_PREFIX = "$";
    // soscmd Option flags
    String FLAG_COMMAND = "-command";
    String FLAG_VSS_SERVER = "-database";
    String FLAG_USERNAME = "-name";
    String FLAG_PASSWORD = "-password";
    String FLAG_COMMENT = "-log";
    String FLAG_WORKING_DIR = "-workdir";
    String FLAG_RECURSION = "-recursive";
    String FLAG_VERSION = "-revision";
    String FLAG_LABEL = "-label";
    String FLAG_NO_COMPRESSION = "-nocompress";
    String FLAG_NO_CACHE = "-nocache";
    String FLAG_SOS_SERVER = "-server";
    String FLAG_SOS_HOME = "-soshome";
    String FLAG_PROJECT = "-project";
    String FLAG_FILE = "-file";
    String FLAG_VERBOSE = "-verbose";
}
