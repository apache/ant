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

/*
 * Since the initial version of this file was developed on the clock on
 * an NSF grant I should say the following boilerplate:
 *
 * This material is based upon work supported by the National Science
 * Foundation under Grant No. EIA-0196404. Any opinions, findings, and
 * conclusions or recommendations expressed in this material are those
 * of the author and do not necessarily reflect the views of the
 * National Science Foundation.
 */

package org.apache.tools.ant.taskdefs.optional.unix;

import org.apache.tools.ant.BuildException;

/**
 * Chown equivalent for unix-like environments.
 *
 * @since Ant 1.6
 *
 * @ant.task category="filesystem"
 */
public class Chown extends AbstractAccessTask {

    private boolean haveOwner = false;

    /**
     * Chown task for setting file and directory permissions.
     */
    public Chown() {
        super.setExecutable("chown");
    }

    /**
     * Set the owner attribute.
     *
     * @param owner    The new owner for the file(s) or directory(ies)
     */
    public void setOwner(String owner) {
        createArg().setValue(owner);
        haveOwner = true;
    }

    /**
     * Ensure that all the required arguments and other conditions have
     * been set.
     */
    @Override
    protected void checkConfiguration() {
        if (!haveOwner) {
            throw new BuildException("Required attribute owner not set in"
                                     + " chown", getLocation());
        }
        super.checkConfiguration();
    }

    /**
     * We don't want to expose the executable attribute, so override it.
     *
     * @param e User supplied executable that we won't accept.
     */
    @Override
    public void setExecutable(String e) {
        throw new BuildException(getTaskType()
                                 + " doesn't support the executable"
                                 + " attribute", getLocation());
    }
}
