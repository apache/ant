/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.util.StringUtils;

/**
 * Alters the default excludes for the <strong>entire</strong> build..
 *
 * @since Ant 1.6
 *
 * @ant.task category="utility"
 */
public class DefaultExcludes extends Task {
    private String add = "";
    private String remove = "";
    private boolean defaultrequested = false;
    private boolean echo = false;

    // by default, messages are always displayed
    private int logLevel = Project.MSG_WARN;

    /**
     * Does the work.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {
        if (!defaultrequested && add.equals("") && remove.equals("") && !echo) {
            throw new BuildException("<defaultexcludes> task must set "
                + "at least one attribute (echo=\"false\""
                + " doesn't count since that is the default");
        }
        if (defaultrequested) {
            DirectoryScanner.resetDefaultExcludes();
        }
        if (!add.equals("")) {
            DirectoryScanner.addDefaultExclude(add);
        }
        if (!remove.equals("")) {
            DirectoryScanner.removeDefaultExclude(remove);
        }
        if (echo) {
            StringBuffer message
                = new StringBuffer("Current Default Excludes:");
            message.append(StringUtils.LINE_SEP);
            String[] excludes = DirectoryScanner.getDefaultExcludes();
            for (int i = 0; i < excludes.length; i++) {
                message.append("  ");
                message.append(excludes[i]);
                message.append(StringUtils.LINE_SEP);
            }
            log(message.toString(), logLevel);
        }
    }

    /**
     * go back to standard default patterns
     *
     * @param def if true go back to default patterns
     */
    public void setDefault(boolean def) {
        defaultrequested = def;
    }
    /**
     * Pattern to add to the default excludes
     *
     * @param add Sets the value for the pattern to exclude.
     */
    public void setAdd(String add) {
        this.add = add;
    }

     /**
     * Pattern to remove from the default excludes.
     *
     * @param remove Sets the value for the pattern that
     *            should no longer be excluded.
     */
    public void setRemove(String remove) {
        this.remove = remove;
    }

    /**
     * If true, echo the default excludes.
     *
     * @param echo whether or not to echo the contents of
     *             the default excludes.
     */
    public void setEcho(boolean echo) {
        this.echo = echo;
    }


}
