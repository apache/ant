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
import org.apache.avalon.excalibur.io.FileUtil;
import java.io.File;
import java.io.IOException;

/**
 * A command launcher that uses an auxiliary script to launch commands in
 * directories other than the current working directory.
 */
class ScriptCommandLauncher
    extends CommandLauncherProxy
{
    private String _script;

    ScriptCommandLauncher( String script, CommandLauncher launcher )
    {
        super( launcher );
        _script = script;
    }

    /**
     * Launches the given command in a new process, in the given working
     * directory
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
        if( project == null )
        {
            if( workingDir == null )
            {
                return exec( project, cmd, env );
            }
            throw new IOException( "Cannot locate antRun script: No project provided" );
        }

        // Locate the auxiliary script
        String antHome = project.getProperty( "ant.home" );
        if( antHome == null )
        {
            throw new IOException( "Cannot locate antRun script: Property 'ant.home' not found" );
        }
        String antRun = FileUtil.
            resolveFile( project.getBaseDir(), antHome + File.separator + _script ).toString();

        // Build the command
        File commandDir = workingDir;
        if( workingDir == null && project != null )
        {
            commandDir = project.getBaseDir();
        }

        String[] newcmd = new String[ cmd.length + 2 ];
        newcmd[ 0 ] = antRun;
        newcmd[ 1 ] = commandDir.getAbsolutePath();
        System.arraycopy( cmd, 0, newcmd, 2, cmd.length );

        return exec( project, newcmd, env );
    }
}
