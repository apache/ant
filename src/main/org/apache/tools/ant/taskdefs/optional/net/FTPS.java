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
package org.apache.tools.ant.taskdefs.optional.net;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

/**
 * Basic FTPS client. Performs the following actions:
 * <ul>
 *   <li><strong>send</strong> - send files to a remote server. This is the
 *   default action.</li>
 *   <li><strong>get</strong> - retrieve files from a remote server.</li>
 *   <li><strong>del</strong> - delete files from a remote server.</li>
 *   <li><strong>list</strong> - create a file listing.</li>
 *   <li><strong>chmod</strong> - change unix file permissions.</li>
 *   <li><strong>rmdir</strong> - remove directories, if empty, from a
 *   remote server.</li>
 * </ul>
 * This class is an extention of FTP class that supports FTPS Client for FTPS taskdef.
 *
 */
public class FTPS extends FTP {
    @Override
    protected FTPClient getFTPClient(){
        return new FTPSClient();
    }
}
