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

/**
 * A command launcher for JDK/JRE 1.1 under Windows. Fixes quoting problems
 * in Runtime.exec(). Can only launch commands in the current working
 * directory
 */
class Java11CommandLauncher
    extends CommandLauncher
{
    /**
     * Launches the given command in a new process. Needs to quote arguments
     *
     * @param project Description of Parameter
     * @param cmd Description of Parameter
     * @param env Description of Parameter
     * @return Description of the Returned Value
     * @exception IOException Description of Exception
     */
    public Process exec( Project project, String[] cmd, String[] env )
        throws IOException, TaskException
    {
        // Need to quote arguments with spaces, and to escape quote characters
        String[] newcmd = new String[ cmd.length ];
        for( int i = 0; i < cmd.length; i++ )
        {
            newcmd[ i ] = Commandline.quoteArgument( cmd[ i ] );
        }
        if( project != null )
        {
            project.log( "Execute:Java11CommandLauncher: " +
                         Commandline.toString( newcmd ), Project.MSG_DEBUG );
        }
        return Runtime.getRuntime().exec( newcmd, env );
    }
}
