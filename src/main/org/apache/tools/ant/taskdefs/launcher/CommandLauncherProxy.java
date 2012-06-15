package org.apache.tools.ant.taskdefs.launcher;

import java.io.IOException;

import org.apache.tools.ant.Project;

/**
 * A command launcher that proxies another command
 * launcher. Sub-classes override exec(args, env, workdir).
 */
public class CommandLauncherProxy extends CommandLauncher {
    private final CommandLauncher myLauncher;

    protected CommandLauncherProxy(CommandLauncher launcher) {
        myLauncher = launcher;
    }

    /**
     * Launches the given command in a new process. Delegates this
     * method to the proxied launcher.
     * 
     * @param project
     *        the Ant project.
     * @param cmd
     *        the command line to execute as an array of strings.
     * @param env
     *        the environment to set as an array of strings.
     * @return the created Process.
     * @throws IOException
     *         forwarded from the exec method of the command launcher.
     */
    @Override
    public Process exec(Project project, String[] cmd, String[] env)
        throws IOException {
        return myLauncher.exec(project, cmd, env);
    }
}