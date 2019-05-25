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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.launcher.CommandLauncher;

/**
 * Task that configures the {@link
 * org.apache.tools.ant.taskdefs.launcher.CommandLauncher} to used
 * when starting external processes.
 * @since Ant 1.9.0
 */
public class CommandLauncherTask extends Task {
    private boolean vmLauncher;
    private CommandLauncher commandLauncher;

    public synchronized void addConfigured(CommandLauncher commandLauncher) {
        if (this.commandLauncher != null) {
            throw new BuildException("Only one CommandLauncher can be installed");
        }
        this.commandLauncher = commandLauncher;
    }

    @Override
    public void execute() {
        if (commandLauncher != null) {
            if (vmLauncher) {
                CommandLauncher.setVMLauncher(getProject(), commandLauncher);
            } else {
                CommandLauncher.setShellLauncher(getProject(), commandLauncher);
            }
        }
    }

    public void setVmLauncher(boolean vmLauncher) {
        this.vmLauncher = vmLauncher;
    }

}
