/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.ccm;

import java.io.File;
import org.apache.aut.nativelib.ExecManager;
import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.todo.types.Commandline;

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
    extends AbstractTask
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
    public final void setCcmDir( final File dir )
    {
        m_ccmDir = dir.toString();
    }

    /**
     * Set the value of ccmAction.
     *
     * @param ccmAction Value to assign to ccmAction.
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

    protected void run( final Commandline cmd, final ExecOutputHandler handler )
        throws TaskException
    {
        final Execute exe = new Execute();
        if( null != handler )
        {
            exe.setExecOutputHandler( handler );
        }
        exe.setCommandline( cmd );
        exe.execute( getContext() );
    }
}
