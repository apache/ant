package org.apache.tools.ant.taskdefs.launcher;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 * A command launcher for JDK/JRE 1.3 (and higher). Uses the built-in
 * Runtime.exec() command.
 */
public class Java13CommandLauncher extends CommandLauncher {

    public Java13CommandLauncher() throws NoSuchMethodException {
        // Used to verify if Java13 is available, is prerequisite in ant 1.8
    }

    /**
     * Launches the given command in a new process, in the given
     * working directory.
     * 
     * @param project
     *        the Ant project.
     * @param cmd
     *        the command line to execute as an array of strings.
     * @param env
     *        the environment to set as an array of strings.
     * @param workingDir
     *        the working directory where the command should run.
     * @return the created Process.
     * @throws IOException
     *         probably forwarded from Runtime#exec.
     */
    @Override
    public Process exec(Project project, String[] cmd, String[] env,
                        File workingDir) throws IOException {
        try {
            if (project != null) {
                project.log("Execute:Java13CommandLauncher: "
                            + Commandline.describeCommand(cmd),
                            Project.MSG_DEBUG);
            }
            return Runtime.getRuntime().exec(cmd, env, workingDir);
        } catch(IOException ioex) {
            throw ioex;
        } catch(Exception exc) {
            // IllegalAccess, IllegalArgument, ClassCast
            throw new BuildException("Unable to execute command", exc);
        }
    }
}