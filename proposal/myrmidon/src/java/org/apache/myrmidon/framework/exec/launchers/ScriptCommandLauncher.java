/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.exec.launchers;

import java.io.IOException;
import org.apache.myrmidon.framework.exec.CommandLauncher;
import org.apache.myrmidon.framework.exec.ExecException;
import org.apache.myrmidon.framework.exec.ExecMetaData;

/**
 * A command launcher that uses an auxiliary script to launch commands in
 * directories other than the current working directory. The script specified
 * in the constructor is invoked with the directory passed as second argument
 * and the actual command as subsequent arguments.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>
 * @version $Revision$ $Date$
 */
public class ScriptCommandLauncher
    implements CommandLauncher
{
    private String[] m_script;

    /**
     * Create a command launcher whos script is a single
     * command. An example would be "bin/antRun.bat".
     */
    public ScriptCommandLauncher( final String script )
    {
        this( new String[]{script} );
    }

    /**
     * Create a command launcher whos script takes multiple
     * commands. Examples would be "perl bin/antRun.pl",
     * "python bin/antRun.py", ""tcl8 bin/antRun.tcl" etc
     */
    public ScriptCommandLauncher( final String[] script )
    {
        m_script = script;
        if( null == m_script )
        {
            throw new NullPointerException( "script" );
        }
        if( 0 == m_script.length )
        {
            throw new IllegalArgumentException( "script" );
        }
    }

    /**
     * Launches the given command in a new process using cmd.exe to
     * set the working directory.
     */
    public Process exec( final ExecMetaData metaData )
        throws IOException, ExecException
    {
        // Build the command
        final String[] prefix = new String[ m_script.length + 1 ];
        for( int i = 0; i < m_script.length; i++ )
        {
            prefix[ i ] = m_script[ i ];
        }
        prefix[ m_script.length ] = metaData.getWorkingDirectory().getCanonicalPath();

        final ExecMetaData newMetaData = ExecUtil.prepend( metaData, prefix );
        final String[] env = ExecUtil.getEnvironmentSpec( metaData );
        return Runtime.getRuntime().exec( newMetaData.getCommand(), env );
    }
}
