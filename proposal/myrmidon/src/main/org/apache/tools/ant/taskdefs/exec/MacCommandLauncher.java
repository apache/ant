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
 * A command launcher for Mac that uses a dodgy mechanism to change working
 * directory before launching commands.
 */
class MacCommandLauncher
    extends CommandLauncherProxy
{
    MacCommandLauncher( CommandLauncher launcher )
    {
        super( launcher );
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
        if( workingDir == null )
        {
            return exec( project, cmd, env );
        }

        System.getProperties().put( "user.dir", workingDir.getAbsolutePath() );
        try
        {
            return exec( project, cmd, env );
        }
        finally
        {
            System.getProperties().put( "user.dir", Execute.antWorkingDirectory );
        }
    }
}
