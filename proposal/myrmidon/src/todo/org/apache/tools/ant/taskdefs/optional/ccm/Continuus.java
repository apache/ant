/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ccm;

import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.exec.LogStreamHandler;
import org.apache.tools.ant.taskdefs.exec.LogOutputStream;
import org.apache.tools.ant.types.Commandline;

/**
 * A base class for creating tasks for executing commands on Continuus 5.1 <p>
 *
 * The class extends the task as it operates by executing the ccm.exe program
 * supplied with Continuus/Synergy. By default the task expects the ccm
 * executable to be in the path, you can override this be specifying the ccmdir
 * attribute. </p>
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public abstract class Continuus
    extends Task
{
    /**
     * Constant for the thing to execute
     */
    private final static String CCM_EXE = "ccm";

    /**
     * The 'CreateTask' command
     */
    public final static String COMMAND_CREATE_TASK = "create_task";
    /**
     * The 'Checkout' command
     */
    public final static String COMMAND_CHECKOUT = "co";
    /**
     * The 'Checkin' command
     */
    public final static String COMMAND_CHECKIN = "ci";
    /**
     * The 'Reconfigure' command
     */
    public final static String COMMAND_RECONFIGURE = "reconfigure";

    /**
     * The 'Reconfigure' command
     */
    public final static String COMMAND_DEFAULT_TASK = "default_task";

    private String m_ccmDir = "";
    private String m_ccmAction = "";

    /**
     * Set the directory where the ccm executable is located
     *
     * @param dir the directory containing the ccm executable
     */
    public final void setCcmDir( String dir )
    {
        m_ccmDir = getProject().translatePath( dir );
    }

    /**
     * Set the value of ccmAction.
     *
     * @param v Value to assign to ccmAction.
     */
    public void setCcmAction( final String ccmAction )
    {
        m_ccmAction = ccmAction;
    }

    /**
     * Get the value of ccmAction.
     *
     * @return value of ccmAction.
     */
    public String getCcmAction()
    {
        return m_ccmAction;
    }

    /**
     * Builds and returns the command string to execute ccm
     *
     * @return String containing path to the executable
     */
    protected final String getCcmCommand()
    {
        String toReturn = m_ccmDir;
        if( !toReturn.equals( "" ) && !toReturn.endsWith( "/" ) )
        {
            toReturn += "/";
        }

        toReturn += CCM_EXE;

        return toReturn;
    }

    protected int run( final Commandline cmd,
                       final ExecuteStreamHandler handler )
        throws TaskException
    {
        try
        {
            final Execute exe = new Execute( handler );
            exe.setWorkingDirectory( getBaseDirectory() );
            exe.setCommandline( cmd.getCommandline() );
            return exe.execute();
        }
        catch( final IOException ioe )
        {
            throw new TaskException( "Error", ioe );
        }
    }

    protected int run( final Commandline cmd )
        throws TaskException
    {
        final LogOutputStream output = new LogOutputStream( getLogger(), false );
        final LogOutputStream error = new LogOutputStream( getLogger(), true );
        final LogStreamHandler handler = new LogStreamHandler( output, error );
        return run( cmd, handler );
    }
}
