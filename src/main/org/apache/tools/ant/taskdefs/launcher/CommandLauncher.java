package org.apache.tools.ant.taskdefs.launcher;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;

/**
 * A command launcher for a particular JVM/OS platform. This class is
 * a general purpose command launcher which can only launch commands
 * in the current working directory.
 */
public class CommandLauncher {
    private static final String ANT_SHELL_LAUNCHER_REF_ID = "ant.shellLauncher";
    private static final String ANT_VM_LAUNCHER_REF_ID = "ant.vmLauncher";

    protected static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private static CommandLauncher vmLauncher = null;
    private static CommandLauncher shellLauncher = null;

    static {
        // Try using a JDK 1.3 launcher
        try {
            if(!Os.isFamily("os/2")) {
                vmLauncher = new Java13CommandLauncher();
            }
        } catch(NoSuchMethodException exc) {
            // Ignore and keep trying
        }

        if (Os.isFamily("mac") && !Os.isFamily("unix")) {
            // Mac
            shellLauncher = new MacCommandLauncher(new CommandLauncher());
        } else if (Os.isFamily("os/2")) {
            // OS/2
            shellLauncher = new OS2CommandLauncher(new CommandLauncher());
        } else if (Os.isFamily("windows")) {
            CommandLauncher baseLauncher = new CommandLauncher();

            if (!Os.isFamily("win9x")) {
                // Windows XP/2000/NT
                shellLauncher = new WinNTCommandLauncher(baseLauncher);
            } else {
                // Windows 98/95 - need to use an auxiliary script
                shellLauncher = new ScriptCommandLauncher("bin/antRun.bat", baseLauncher);
            }
        } else if (Os.isFamily("netware")) {

            CommandLauncher baseLauncher = new CommandLauncher();

            shellLauncher = new PerlScriptCommandLauncher("bin/antRun.pl", baseLauncher);
        } else if (Os.isFamily("openvms")) {
            // OpenVMS
            try {
                shellLauncher = new VmsCommandLauncher();
            } catch(NoSuchMethodException exc) {
                // Ignore and keep trying
            }
        } else {
            // Generic
            shellLauncher = new ScriptCommandLauncher("bin/antRun", new CommandLauncher());
        }
    }

    /**
     * Launches the given command in a new process.
     * 
     * @param project
     *        The project that the command is part of.
     * @param cmd
     *        The command to execute.
     * @param env
     *        The environment for the new process. If null, the
     *        environment of the current process is used.
     * @return the created Process.
     * @throws IOException
     *         if attempting to run a command in a specific directory.
     */
    public Process exec(Project project, String[] cmd, String[] env) throws IOException {
        if(project != null) {
            project.log("Execute:CommandLauncher: " + Commandline.describeCommand(cmd), Project.MSG_DEBUG);
        }
        return Runtime.getRuntime().exec(cmd, env);
    }

    /**
     * Launches the given command in a new process, in the given
     * working directory.
     * 
     * @param project
     *        The project that the command is part of.
     * @param cmd
     *        The command to execute.
     * @param env
     *        The environment for the new process. If null, the
     *        environment of the current process is used.
     * @param workingDir
     *        The directory to start the command in. If null, the
     *        current directory is used.
     * @return the created Process.
     * @throws IOException
     *         if trying to change directory.
     */
    public Process exec(Project project, String[] cmd, String[] env, File workingDir) throws IOException {
        if(workingDir == null) {
            return exec(project, cmd, env);
        }
        throw new IOException("Cannot execute a process in different "
                              + "directory under this JVM");
    }

    public static CommandLauncher getShellLauncher(Project project) {
        CommandLauncher launcher = null;
        if(project != null) {
            launcher = (CommandLauncher) project
                .getReference(ANT_SHELL_LAUNCHER_REF_ID);
        }
        if (launcher == null) {
            launcher = getSystemLauncher(ANT_SHELL_LAUNCHER_REF_ID);
        }
        if (launcher == null) {
            launcher = shellLauncher;
        }

        return launcher;
    }

    public static CommandLauncher getVMLauncher(Project project) {
        CommandLauncher launcher = null;
        if (project != null) {
            launcher = (CommandLauncher)project.getReference(ANT_VM_LAUNCHER_REF_ID);
        }

        if (launcher == null) {
            launcher = getSystemLauncher(ANT_VM_LAUNCHER_REF_ID);
        }
        if (launcher == null) {
            launcher = vmLauncher;
        }
        return launcher;
    }

    private static CommandLauncher getSystemLauncher(String launcherRefId) {
        CommandLauncher launcher = null;
        String launcherClass = System.getProperty(launcherRefId);
        if (launcherClass != null) {
            try {
                launcher = (CommandLauncher) Class.forName(launcherClass)
                    .newInstance();
            }
            catch(InstantiationException e) {
                System.err.println("Could not instantiate launcher class "
                                   + launcherClass + ": " + e.getMessage());
            }
            catch(IllegalAccessException e) {
                System.err.println("Could not instantiate launcher class "
                                   + launcherClass + ": " + e.getMessage());
            }
            catch(ClassNotFoundException e) {
                System.err.println("Could not instantiate launcher class "
                                   + launcherClass + ": " + e.getMessage());
            }
        }

        return launcher;
    }

    public static void setVMLauncher(Project project, CommandLauncher launcher) {
        if (project != null) {
            project.addReference(ANT_VM_LAUNCHER_REF_ID, launcher);
        }
    }

    public static void setShellLauncher(Project project, CommandLauncher launcher) {
        if (project != null) {
            project.addReference(ANT_SHELL_LAUNCHER_REF_ID, launcher);
        }
    }

}