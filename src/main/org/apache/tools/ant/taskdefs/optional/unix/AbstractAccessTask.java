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

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecuteOn;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;

/**
 * @since Ant 1.6
 *
 * @ant.task category="filesystem"
 */

public abstract class AbstractAccessTask extends ExecuteOn {

    /**
     * Chmod task for setting file and directory permissions.
     */
    public AbstractAccessTask() {
        super.setParallel(true);
        super.setSkipEmptyFilesets(true);
    }

    /**
     * Set the file which should have its access attributes modified.
     * @param src the file to modify
     */
    public void setFile(File src) {
        FileSet fs = new FileSet();
        fs.setFile(src);
        addFileset(fs);
    }

    /**
     * Prevent the user from specifying a different command.
     *
     * @ant.attribute ignore="true"
     * @param cmdl A user supplied command line that we won't accept.
     */
    @Override
    public void setCommand(Commandline cmdl) {
        throw new BuildException(getTaskType()
                                 + " doesn't support the command attribute",
                                 getLocation());
    }

    /**
     * Prevent the skipping of empty filesets
     *
     * @ant.attribute ignore="true"
     * @param skip A user supplied boolean we won't accept.
     */
    @Override
    public void setSkipEmptyFilesets(boolean skip) {
        throw new BuildException(getTaskType() + " doesn't support the "
                                 + "skipemptyfileset attribute",
                                 getLocation());
    }

    /**
     * Prevent the use of the addsourcefile attribute.
     *
     * @ant.attribute ignore="true"
     * @param b A user supplied boolean we won't accept.
     */
    @Override
    public void setAddsourcefile(boolean b) {
        throw new BuildException(getTaskType()
            + " doesn't support the addsourcefile attribute", getLocation());
    }

    /**
     * Automatically approve Unix OS's.
     * @return true if a valid OS, for unix this is always true, otherwise
     *              use the superclasses' test (user set).
     */
    @Override
    protected boolean isValidOs() {
        return getOs() == null && getOsFamily() == null
            ? Os.isFamily(Os.FAMILY_UNIX) : super.isValidOs();
    }
}
