/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.clearcase;

import java.io.File;
import java.io.IOException;
import org.apache.aut.nativelib.ExecManager;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute2;
import org.apache.tools.ant.types.Commandline;

/**
 * A base class for creating tasks for executing commands on ClearCase. <p>
 *
 * The class extends the 'exec' task as it operates by executing the cleartool
 * program supplied with ClearCase. By default the task expects the cleartool
 * executable to be in the path, * you can override this be specifying the
 * cleartooldir attribute. </p> <p>
 *
 * This class provides set and get methods for the 'viewpath' attribute. It also
 * contains constants for the flags that can be passed to cleartool. </p>
 *
 * @author Curtis White
 */
public abstract class ClearCase extends Task
{

    /**
     * Constant for the thing to execute
     */
    private final static String CLEARTOOL_EXE = "cleartool";

    /**
     * The 'Update' command
     */
    public final static String COMMAND_UPDATE = "update";
    /**
     * The 'Checkout' command
     */
    public final static String COMMAND_CHECKOUT = "checkout";
    /**
     * The 'Checkin' command
     */
    public final static String COMMAND_CHECKIN = "checkin";
    /**
     * The 'UndoCheckout' command
     */
    public final static String COMMAND_UNCHECKOUT = "uncheckout";
    private String m_ClearToolDir = "";
    private String m_viewPath = null;

    /**
     * Set the directory where the cleartool executable is located
     *
     * @param dir the directory containing the cleartool executable
     */
    public final void setClearToolDir( final File dir )
    {
        m_ClearToolDir = dir.toString();
    }

    /**
     * Set the path to the item in a clearcase view to operate on
     *
     * @param viewPath Path to the view directory or file
     */
    public final void setViewPath( String viewPath )
    {
        m_viewPath = viewPath;
    }

    /**
     * Get the path to the item in a clearcase view
     *
     * @return m_viewPath
     */
    public String getViewPath()
    {
        return m_viewPath;
    }

    /**
     * Builds and returns the command string to execute cleartool
     *
     * @return String containing path to the executable
     */
    protected final String getClearToolCommand()
    {
        String toReturn = m_ClearToolDir;
        if( !toReturn.equals( "" ) && !toReturn.endsWith( "/" ) )
        {
            toReturn += "/";
        }

        toReturn += CLEARTOOL_EXE;

        return toReturn;
    }

    protected int run( Commandline cmd )
        throws TaskException
    {
        try
        {
            final ExecManager execManager = (ExecManager)getService( ExecManager.class );
            final Execute2 exe = new Execute2( execManager );
            exe.setWorkingDirectory( getBaseDirectory() );
            exe.setCommandline( cmd );
            return exe.execute();
        }
        catch( IOException e )
        {
            throw new TaskException( "Error", e );
        }
    }

}

