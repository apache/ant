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

package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Environment;

/**
 * Specialized <exec> that knows how to deal with Mono vs. Microsoft's
 * VM - and maybe Rotor at some point.
 */
public class DotNetExecTask extends ExecTask {

    /**
     * "Magic" VM argument for Microsoft's VM.
     */
    private static final String MS_VM = "microsoft";

    /**
     * The user supplied executable attribute.
     */
    private String executable;

    /**
     * The .NET VM to use.
     *
     * <p>Defaults to Microsoft's on Windows and mono on any other
     * platform.</p>
     */
    private String vm = Os.isFamily("windows") ? MS_VM : "mono";

    /**
     * Empty Constructor.
     */
    public DotNetExecTask() {
        super();
    }

    /**
     * Set the name of the executable program.
     * @param value the name of the executable program
     */
    public void setExecutable(String value) {
        this.executable = value;
    }

    /**
     * Set the name of the executable for the virtual machine or the
     * magic name "microsoft" which implies that we can invoke the
     * executable directly.
     *
     * @param value the name of the executable for the virtual machine
     */
    public void setVm(String value) {
        this.vm = value;
    }

    /**
     * Do the work.
     *
     * @throws BuildException if executable is empty or &lt;exec&gt;
     * throws an exception.
     */
    public void execute() throws BuildException {
        if (executable == null) {
            throw new BuildException("The executable attribute is required");
        }
        setupCommandline();
        super.execute();
    }

    /**
     * If the inherited Commandline doesn't know about the executable
     * yet, set it and deal with the vm attribute.
     *
     * <p>The inherited Commandline may know the executable already if
     * this task instance is getting reused.</p>
     */
    protected void setupCommandline() {
        if (cmdl.getExecutable() == null) {
            if (vm.equals(MS_VM)) {
                // can invoke executable directly
                super.setExecutable(executable);
            } else {
                boolean b = getResolveExecutable();
                // Mono wants the absolte path of the assembly
                setResolveExecutable(b || isMono(vm));
                super.setExecutable(vm);
                cmdl.createArgument(true)
                    .setValue(resolveExecutable(executable, isMono(vm)));
                setResolveExecutable(b);
            }
        }
    }

    /**
     * Whether the given vm looks like the Mono executable.
     */
    protected final static boolean isMono(String vm) {
        return "mono".equals(vm) || "mint".equals(vm);
    }

    /**
     * Creates an instance of this task based on a different tasks settings.
     */
    public static DotNetExecTask getTask(Task t, String vm, 
                                         String executable,
                                         Environment env) {
        DotNetExecTask exec = new DotNetExecTask();
        if (vm != null) {
            exec.setVm(vm);
        }
        exec.setProject(t.getProject());
        exec.setExecutable(executable);
        exec.setTaskName(t.getTaskName());
        if (env != null) {
            String[] environment = env.getVariables();
            if (environment != null) {
                for (int i = 0; i < environment.length; i++) {
                    int idx = environment[i].indexOf("=");
                    Environment.Variable v = new Environment.Variable();
                    v.setKey(environment[i].substring(0, idx));
                    v.setValue(environment[i].substring(idx + 1));
                    exec.addEnv(v);
                }
            }
        }
        
        return exec;
    }

}
