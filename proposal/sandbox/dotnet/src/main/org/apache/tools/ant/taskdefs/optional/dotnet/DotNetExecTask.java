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

package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.condition.Os;

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
}
