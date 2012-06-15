package org.apache.tools.ant.taskdefs.launcher;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.Project;

/**
 * A command launcher for Mac that uses a dodgy mechanism to change
 * working directory before launching commands.
 */
public class MacCommandLauncher extends CommandLauncherProxy {
    public MacCommandLauncher(CommandLauncher launcher) {
        super(launcher);
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
     *        working directory where the command should run.
     * @return the created Process.
     * @throws IOException
     *         forwarded from the exec method of the command launcher.
     */
    @Override
    public Process exec(Project project, String[] cmd, String[] env,
                        File workingDir) throws IOException {
        if (workingDir == null) {
            return exec(project, cmd, env);
        }
        System.getProperties().put("user.dir", workingDir.getAbsolutePath());
        try {
            return exec(project, cmd, env);
        }
        finally {
            System.getProperties().put("user.dir", System.getProperty("user.dir"));
        }
    }
}