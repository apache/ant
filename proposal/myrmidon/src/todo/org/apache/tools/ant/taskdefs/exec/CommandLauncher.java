/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.myrmidon.api.TaskException;
import java.io.IOException;
import java.io.File;

/**
 * A command launcher for a particular JVM/OS platform. This class is a
 * general purpose command launcher which can only launch commands in the
 * current working directory.
 */
class CommandLauncher
{
    /**
     * Launches the given command in a new process.
     *
     * @param project The project that the command is part of
     * @param cmd The command to execute
     * @param env The environment for the new process. If null, the
     *      environment of the current proccess is used.
     * @return Description of the Returned Value
     * @exception IOException Description of Exception
     */
    public Process exec( Project project, String[] cmd, String[] env )
        throws IOException, TaskException
    {
        if( project != null )
        {
            project.log( "Execute:CommandLauncher: " +
                         Commandline.toString( cmd ), Project.MSG_DEBUG );
        }
        return Runtime.getRuntime().exec( cmd, env );
    }

    /**
     * Launches the given command in a new process, in the given working
     * directory.
     *
     * @param project The project that the command is part of
     * @param cmd The command to execute
     * @param env The environment for the new process. If null, the
     *      environment of the current proccess is used.
     * @param workingDir The directory to start the command in. If null, the
     *      current directory is used
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
        throw new IOException( "Cannot execute a process in different directory under this JVM" );
    }
}
