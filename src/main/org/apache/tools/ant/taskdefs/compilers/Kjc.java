/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * The implementation of the Java compiler for KJC.
 * This is primarily a cut-and-paste from Jikes.java and
 * DefaultCompilerAdapter.
 *
 * @author <a href="mailto:tora@debian.org">Takashi Okamoto</a> + */
public class Kjc extends DefaultCompilerAdapter {

    public boolean execute() throws BuildException {
        attributes.log("Using kjc compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupKjcCommand();

        try {
            Class c = Class.forName("at.dms.kjc.Main");

            // Call the compile() method
            Method compile = c.getMethod("compile", 
                                         new Class [] { String [].class });
            Boolean ok = (Boolean)compile.invoke(null, 
                                                 new Object[] {cmd.getArguments()});
            return ok.booleanValue();
        }
        catch (ClassNotFoundException ex) {
            throw new BuildException("Cannot use kjc compiler, as it is not available"+
                                     " A common solution is to set the environment variable"+
                                     " CLASSPATH to your kjc archive (kjc.jar).", location);
        }
        catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            } else {
                throw new BuildException("Error starting kjc compiler: ", ex, location);
            }
        }
    }

    /**
     * setup kjc command arguments.
     */
    protected Commandline setupKjcCommand() {
        Commandline cmd = new Commandline();
	
	// generate classpath, because kjc does't support sourcepath.
        Path classpath = getCompileClasspath();

        if (deprecation == true) {
            cmd.createArgument().setValue("-deprecation");
        }

        if (destDir != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(destDir);
        }

        // generate the clsspath 
        cmd.createArgument().setValue("-classpath");

	Path cp = new Path(project);

	// kjc don't have bootclasspath option.
	if (bootclasspath != null) {
            cp.append(bootclasspath);
	}

	if (extdirs != null) {
            addExtdirsToClasspath(cp);
	}

	cp.append(classpath);
	cp.append(src);

	cmd.createArgument().setPath(cp);

	// kjc-1.5A doesn't support -encoding option now.
        // but it will be supported near the feature.
        if (encoding != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(encoding);
        }

        if (debug) {
            cmd.createArgument().setValue("-g");
        }

        if (optimize) {
            cmd.createArgument().setValue("-O2");
        }

        if (verbose) {
            cmd.createArgument().setValue("-verbose");
        }

        logAndAddFilesToCompile(cmd);
        return cmd;
    }
}


