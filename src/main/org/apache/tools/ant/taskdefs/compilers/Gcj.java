/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * The implementation of the gcj compiler.
 * This is primarily a cut-and-paste from the jikes.
 *
 * @author <a href="mailto:tora@debian.org">Takashi Okamoto</a>
 * @since Ant 1.4
 */
public class Gcj extends DefaultCompilerAdapter {

    /**
     * Performs a compile using the gcj compiler.
     */
    public boolean execute() throws BuildException {
        Commandline cmd;
        attributes.log("Using gcj compiler", Project.MSG_VERBOSE);
        cmd = setupGCJCommand();

        int firstFileName = cmd.size();
        logAndAddFilesToCompile(cmd);

        return 
            executeExternalCompile(cmd.getCommandline(), firstFileName) == 0;
    }

    protected Commandline setupGCJCommand() {
        Commandline cmd = new Commandline();
        Path classpath = new Path(project);

        // gcj doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if (bootclasspath != null) {
            classpath.append(bootclasspath);
        }

        // gcj doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        classpath.addExtdirs(extdirs);

        if (bootclasspath == null || bootclasspath.size() == 0) {
            // no bootclasspath, therefore, get one from the java runtime
            includeJavaRuntime = true;
        }
        classpath.append(getCompileClasspath());

        // Gcj has no option for source-path so we
        // will add it to classpath.
        if (compileSourcepath != null) {
            classpath.append(compileSourcepath);
        } else {
            classpath.append(src);
        }

        cmd.setExecutable("gcj");

        if (destDir != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(destDir);
            
            if (destDir.mkdirs()) {
                throw new BuildException("Can't make output directories. "
                                         + "Maybe permission is wrong. ");
            };
        }
        
        cmd.createArgument().setValue("-classpath");
        cmd.createArgument().setPath(classpath);

        if (encoding != null) {
            cmd.createArgument().setValue("--encoding=" + encoding);
        }
        if (debug) {
            cmd.createArgument().setValue("-g1");
        }
        if (optimize) {
            cmd.createArgument().setValue("-O");
        }

        /**
         *  gcj should be set for generate class.
         */
        cmd.createArgument().setValue("-C");

        addCurrentCompilerArgs(cmd);

        return cmd;
    }
}
