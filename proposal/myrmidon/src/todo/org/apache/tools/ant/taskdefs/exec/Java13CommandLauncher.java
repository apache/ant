/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.myrmidon.api.TaskException;

/**
 * A command launcher for JDK/JRE 1.3 (and higher). Uses the built-in
 * Runtime.exec() command
 *
 * @author RT
 */
class Java13CommandLauncher
    extends CommandLauncher
{

    private Method _execWithCWD;

    public Java13CommandLauncher()
        throws NoSuchMethodException
    {
        // Locate method Runtime.exec(String[] cmdarray, String[] envp, File dir)
        _execWithCWD = Runtime.class.getMethod( "exec", new Class[]{String[].class, String[].class, File.class} );
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
        try
        {
            if( project != null )
            {
                project.log( "Execute:Java13CommandLauncher: " +
                             Commandline.toString( cmd ), Project.MSG_DEBUG );
            }
            Object[] arguments = {cmd, env, workingDir};
            return (Process)_execWithCWD.invoke( Runtime.getRuntime(), arguments );
        }
        catch( InvocationTargetException exc )
        {
            Throwable realexc = exc.getTargetException();
            if( realexc instanceof ThreadDeath )
            {
                throw (ThreadDeath)realexc;
            }
            else if( realexc instanceof IOException )
            {
                throw (IOException)realexc;
            }
            else
            {
                throw new TaskException( "Unable to execute command", realexc );
            }
        }
        catch( Exception exc )
        {
            // IllegalAccess, IllegalArgument, ClassCast
            throw new TaskException( "Unable to execute command", exc );
        }
    }
}
