/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.vss;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import java.io.IOException;

/**
 * A base class for creating tasks for executing commands on Visual SourceSafe.
 * <p>
 *
 * The class extends the 'exec' task as it operates by executing the ss.exe
 * program supplied with SourceSafe. By default the task expects ss.exe to be in
 * the path, you can override this be specifying the ssdir attribute. </p> <p>
 *
 * This class provides set and get methods for 'login' and 'vsspath' attributes.
 * It also contains constants for the flags that can be passed to SS. </p>
 *
 * @author Craig Cottingham
 * @author Andrew Everitt
 */
public abstract class MSVSS extends Task
{

    /**
     * Constant for the thing to execute
     */
    private final static String SS_EXE = "ss";
    /**
     */
    public final static String PROJECT_PREFIX = "$";

    /**
     * The 'Get' command
     */
    public final static String COMMAND_GET = "Get";
    /**
     * The 'Checkout' command
     */
    public final static String COMMAND_CHECKOUT = "Checkout";
    /**
     * The 'Checkin' command
     */
    public final static String COMMAND_CHECKIN = "Checkin";
    /**
     * The 'Label' command
     */
    public final static String COMMAND_LABEL = "Label";
    /**
     * The 'History' command
     */
    public final static String COMMAND_HISTORY = "History";

    /**
     */
    public final static String FLAG_LOGIN = "-Y";
    /**
     */
    public final static String FLAG_OVERRIDE_WORKING_DIR = "-GL";
    /**
     */
    public final static String FLAG_AUTORESPONSE_DEF = "-I-";
    /**
     */
    public final static String FLAG_AUTORESPONSE_YES = "-I-Y";
    /**
     */
    public final static String FLAG_AUTORESPONSE_NO = "-I-N";
    /**
     */
    public final static String FLAG_RECURSION = "-R";
    /**
     */
    public final static String FLAG_VERSION = "-V";
    /**
     */
    public final static String FLAG_VERSION_DATE = "-Vd";
    /**
     */
    public final static String FLAG_VERSION_LABEL = "-VL";
    /**
     */
    public final static String FLAG_WRITABLE = "-W";
    /**
     */
    public final static String VALUE_NO = "-N";
    /**
     */
    public final static String VALUE_YES = "-Y";
    /**
     */
    public final static String FLAG_QUIET = "-O-";

    private String m_SSDir = "";
    private String m_vssLogin = null;
    private String m_vssPath = null;
    private String m_serverPath = null;

    /**
     * Set the login to use when accessing vss. <p>
     *
     * Should be formatted as username,password
     *
     * @param login the login string to use
     */
    public final void setLogin( String login )
    {
        m_vssLogin = login;
    }

    /**
     * Set the path to the location of the ss.ini
     *
     * @param serverPath
     */
    public final void setServerpath( String serverPath )
    {
        m_serverPath = serverPath;
    }

    /**
     * Set the directory where ss.exe is located
     *
     * @param dir the directory containing ss.exe
     */
    public final void setSsdir( String dir )
    {
        m_SSDir = project.translatePath( dir );
    }

    /**
     * Set the path to the item in vss to operate on <p>
     *
     * Ant can't cope with a '$' sign in an attribute so we have to add it here.
     * Also we strip off any 'vss://' prefix which is an XMS special and should
     * probably be removed!
     *
     * @param vssPath
     */
    public final void setVsspath( String vssPath )
    {
        if( vssPath.startsWith( "vss://" ) )
        {
            m_vssPath = PROJECT_PREFIX + vssPath.substring( 5 );
        }
        else
        {
            m_vssPath = PROJECT_PREFIX + vssPath;
        }
    }

    /**
     * Builds and returns the command string to execute ss.exe
     *
     * @return The SSCommand value
     */
    public final String getSSCommand()
    {
        String toReturn = m_SSDir;
        if( !toReturn.equals( "" ) && !toReturn.endsWith( "\\" ) )
        {
            toReturn += "\\";
        }
        toReturn += SS_EXE;

        return toReturn;
    }

    /**
     * @param cmd Description of Parameter
     */
    public void getLoginCommand( Commandline cmd )
    {
        if( m_vssLogin == null )
        {
            return;
        }
        else
        {
            cmd.createArgument().setValue( FLAG_LOGIN + m_vssLogin );
        }
    }

    /**
     * @return m_vssPath
     */
    public String getVsspath()
    {
        return m_vssPath;
    }

    protected int run( Commandline cmd )
    {
        try
        {
            Execute exe = new Execute( new LogStreamHandler( this,
                Project.MSG_INFO,
                Project.MSG_WARN ) );

            // If location of ss.ini is specified we need to set the
            // environment-variable SSDIR to this value
            if( m_serverPath != null )
            {
                String[] env = exe.getEnvironment();
                if( env == null )
                {
                    env = new String[0];
                }
                String[] newEnv = new String[env.length + 1];
                for( int i = 0; i < env.length; i++ )
                {
                    newEnv[i] = env[i];
                }
                newEnv[env.length] = "SSDIR=" + m_serverPath;

                exe.setEnvironment( newEnv );
            }

            exe.setAntRun( project );
            exe.setWorkingDirectory( project.getBaseDir() );
            exe.setCommandline( cmd.getCommandline() );
            return exe.execute();
        }
        catch( IOException e )
        {
            throw new BuildException( "Error", e );
        }
    }
}

