/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import org.apache.tools.ant.Project;
import org.apache.myrmidon.api.TaskException;
import java.io.File;
import java.io.IOException;

/**
 * A command launcher for Windows 2000/NT that uses 'cmd.exe' when launching
 * commands in directories other than the current working directory.
 */
class WinNTCommandLauncher
    extends CommandLauncherProxy
{
    WinNTCommandLauncher( CommandLauncher launcher )
    {
        super( launcher );
    }

    /**
     * Launches the given command in a new process, in the given working
     * directory.
     *
     * @param project Description of Parameter
     * @param cmd Description of Parameter
     * @param env Description of Parameter
     * @param workingDir Description of Parameter
     * @return Description of the Returned Value
     * @exception IOException Description of Exception
     */
    public Process exec( Project project, String[] cmd, String[] env, File workingDir )
        throws IOException, TaskException
    {
        File commandDir = workingDir;
        if( workingDir == null )
        {
            if( project != null )
            {
                commandDir = project.getBaseDir();
            }
            else
            {
                return exec( project, cmd, env );
            }
        }

        // Use cmd.exe to change to the specified directory before running
        // the command
        final int preCmdLength = 6;
        String[] newcmd = new String[ cmd.length + preCmdLength ];
        newcmd[ 0 ] = "cmd";
        newcmd[ 1 ] = "/c";
        newcmd[ 2 ] = "cd";
        newcmd[ 3 ] = "/d";
        newcmd[ 4 ] = commandDir.getAbsolutePath();
        newcmd[ 5 ] = "&&";
        System.arraycopy( cmd, 0, newcmd, preCmdLength, cmd.length );

        return exec( project, newcmd, env );
    }
}
