/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.exec.launchers;

import java.io.IOException;
import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.exec.CommandLauncher;
import org.apache.myrmidon.framework.exec.ExecMetaData;
import org.apache.avalon.excalibur.io.FileUtil;

/**
 * A command launcher that uses an auxiliary script to launch commands in
 * directories other than the current working directory.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>
 * @version $Revision$ $Date$
 */
public class ScriptCommandLauncher
    implements CommandLauncher
{
    private String m_script;

    public ScriptCommandLauncher( final String script )
    {
        m_script = script;
    }

    /**
     * Launches the given command in a new process using cmd.exe to
     * set the working directory.
     */
    public Process exec( final ExecMetaData metaData )
        throws IOException, TaskException
    {
        final File homeDir = ExecUtil.getAntHomeDirectory();
        final String script = FileUtil.resolveFile( homeDir, m_script ).toString();

        // Build the command
        final String[] prefix = new String[ 2 ];
        prefix[ 0 ] = script;
        prefix[ 1 ] = metaData.getWorkingDirectory().getCanonicalPath();

        final ExecMetaData newMetaData = ExecUtil.prepend( metaData, prefix );
        return Runtime.getRuntime().
            exec( newMetaData.getCommand(), newMetaData.getEnvironment() );
    }
}
