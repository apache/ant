/*
 * Copyright  2001-2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.ide;

/**
 * Super class for all VAJ tasks. Contains common
 * attributes (remoteServer) and util methods
 *
 * @author: Wolf Siberski
 * @author: Martin Landers, Beck et al. projects
 */
import org.apache.tools.ant.Task;


public class VAJTask extends Task {
    /**
     * Adaption of VAJLocalUtil to Task context.
     */
    class VAJLocalToolUtil extends VAJLocalUtil {
        public void log(String msg, int level) {
            VAJTask.this.log(msg, level);
        }
    }

    // server name / port of VAJ remote tool api server
    protected String remoteServer = null;

    // holds the appropriate VAJUtil implementation
    private VAJUtil util = null;

    // checks if this task throws BuildException on error
    protected boolean haltOnError = true;

    /**
     * returns the VAJUtil implementation
     */
    protected VAJUtil getUtil() {
        if (util == null) {
            if (remoteServer == null) {
                util = new VAJLocalToolUtil();
            } else {
                util = new VAJRemoteUtil(this, remoteServer);
            }
        }
        return util;
    }

    /**
     * Name and port of a remote tool server, optiona.
     * Format: &lt;servername&gt;:&lt;port no&gt;.
     * If this attribute is set, the tasks will be executed on the specified tool
     * server.
     */
    public void setRemote(String remoteServer) {
        this.remoteServer = remoteServer;
    }

    /**
    * Flag to control behaviour in case of VAJ errors.
    * If this attribute is set errors will be ignored
    * (no BuildException will be thrown) otherwise
    * VAJ errors will be wrapped into a BuildException and
    * stop the build.
    */
    public void setHaltonerror(boolean newHaltOnError) {
        haltOnError = newHaltOnError;
    }
}
