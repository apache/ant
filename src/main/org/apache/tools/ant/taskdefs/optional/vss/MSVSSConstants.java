/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.vss;

/**
 *  Holds all the constants for the VSS tasks.
 *
 * @author  Jesse Stockall
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
}
