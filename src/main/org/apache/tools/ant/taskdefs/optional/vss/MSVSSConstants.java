/*
 * Copyright  2003-2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.vss;

/**
 *  Holds all the constants for the VSS tasks.
 *
 */
public interface MSVSSConstants {
    /**  Constant for the thing to execute  */
    String SS_EXE = "ss";
    /** Dollar Sigh to prefix the project path */
    String PROJECT_PREFIX = "$";

    /**  The 'CP' command  */
    String COMMAND_CP = "CP";
    /**  The 'Add' command  */
    String COMMAND_ADD = "Add";
    /**  The 'Get' command  */
    String COMMAND_GET = "Get";
    /**  The 'Checkout' command  */
    String COMMAND_CHECKOUT = "Checkout";
    /**  The 'Checkin' command  */
    String COMMAND_CHECKIN = "Checkin";
    /**  The 'Label' command  */
    String COMMAND_LABEL = "Label";
    /**  The 'History' command  */
    String COMMAND_HISTORY = "History";
    /**  The 'Create' command  */
    String COMMAND_CREATE = "Create";

    /**  The brief style flag  */
    String STYLE_BRIEF = "brief";
    /**  The codediff style flag  */
    String STYLE_CODEDIFF = "codediff";
    /**  The nofile style flag  */
    String STYLE_NOFILE = "nofile";
    /**  The default style flag  */
    String STYLE_DEFAULT = "default";

    /**  The text for  current (default) timestamp */
    String TIME_CURRENT = "current";
    /**  The text for  modified timestamp */
    String TIME_MODIFIED = "modified";
    /**  The text for  updated timestamp */
    String TIME_UPDATED = "updated";

    /**  The text for replacing writable files   */
    String WRITABLE_REPLACE = "replace";
    /**  The text for skiping writable files  */
    String WRITABLE_SKIP = "skip";
    /**  The text for failing on writable files  */
    String WRITABLE_FAIL = "fail";

    String FLAG_LOGIN = "-Y";
    String FLAG_OVERRIDE_WORKING_DIR = "-GL";
    String FLAG_AUTORESPONSE_DEF = "-I-";
    String FLAG_AUTORESPONSE_YES = "-I-Y";
    String FLAG_AUTORESPONSE_NO = "-I-N";
    String FLAG_RECURSION = "-R";
    String FLAG_VERSION = "-V";
    String FLAG_VERSION_DATE = "-Vd";
    String FLAG_VERSION_LABEL = "-VL";
    String FLAG_WRITABLE = "-W";
    String VALUE_NO = "-N";
    String VALUE_YES = "-Y";
    String FLAG_QUIET = "-O-";
    String FLAG_COMMENT = "-C";
    String FLAG_LABEL = "-L";
    String VALUE_FROMDATE = "~d";
    String VALUE_FROMLABEL = "~L";
    String FLAG_OUTPUT = "-O";
    String FLAG_USER = "-U";
    String FLAG_NO_FILE = "-F-";
    String FLAG_BRIEF = "-B";
    String FLAG_CODEDIFF = "-D";
    String FLAG_FILETIME_DEF = "-GTC";
    String FLAG_FILETIME_MODIFIED = "-GTM";
    String FLAG_FILETIME_UPDATED = "-GTU";
    String FLAG_REPLACE_WRITABLE = "-GWR";
    String FLAG_SKIP_WRITABLE = "-GWS";
    String FLAG_NO_GET = "-G-";
}
