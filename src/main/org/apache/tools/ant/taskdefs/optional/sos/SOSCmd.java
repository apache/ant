/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.sos;

/**
 * Interface to hold constants used by the SOS tasks
 *
 * @author    <a href="mailto:jesse@cryptocard.com">Jesse Stockall</a>
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

