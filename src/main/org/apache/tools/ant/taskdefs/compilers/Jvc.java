/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the jvc compiler from microsoft.
 * This is primarily a cut-and-paste from the original javac task before it
 * was refactored.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green 
 *         <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com</a>
 * @author Stefan Bodewig 
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @since Ant 1.3
 */
public class Jvc extends DefaultCompilerAdapter {

    /**
     * Run the compilation.
     *
     * @exception BuildException if the compilation has problems.
     */
    public boolean execute() throws BuildException {
        attributes.log("Using jvc compiler", Project.MSG_VERBOSE);

        Path classpath = new Path(project);

        // jvc doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if (bootclasspath != null) {
            classpath.append(bootclasspath);
        }

        // jvc doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        classpath.addExtdirs(extdirs);

        classpath.append(getCompileClasspath());

        // jvc has no option for source-path so we
        // will add it to classpath.
        if (compileSourcepath != null) {
            classpath.append(compileSourcepath);
        } else {
            classpath.append(src);
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable("jvc");

        if (destDir != null) {
            cmd.createArgument().setValue("/d");
            cmd.createArgument().setFile(destDir);
        }
        
        // Add the Classpath before the "internal" one.
        cmd.createArgument().setValue("/cp:p");
        cmd.createArgument().setPath(classpath);

        // Enable MS-Extensions and ...
        cmd.createArgument().setValue("/x-");
        // ... do not display a Message about this.
        cmd.createArgument().setValue("/nomessage");
        // Do not display Logo
        cmd.createArgument().setValue("/nologo");

        if (debug) {
            cmd.createArgument().setValue("/g");
        }
        if (optimize) {
            cmd.createArgument().setValue("/O");
        }
        if (verbose) {
            cmd.createArgument().setValue("/verbose");
        }

        addCurrentCompilerArgs(cmd);

        int firstFileName = cmd.size();
        logAndAddFilesToCompile(cmd);

        return 
            executeExternalCompile(cmd.getCommandline(), firstFileName) == 0;
    }
}
