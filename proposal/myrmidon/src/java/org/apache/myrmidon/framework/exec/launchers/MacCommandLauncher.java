/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.exec.launchers;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.framework.exec.CommandLauncher;
import org.apache.myrmidon.framework.exec.ExecException;
import org.apache.myrmidon.framework.exec.ExecMetaData;

/**
 * A command launcher for Mac that uses a dodgy mechanism to change working
 * directory before launching commands. This class changes the value of the
 * System property "user.dir" before the command is executed and then resets
 * it after the command is executed. This can have really unhealthy side-effects
 * if there are multiple threads in JVM and should be used with extreme caution.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>
 * @version $Revision$ $Date$
 */
public class MacCommandLauncher
    implements CommandLauncher
{
    /**
     * Execute the specified native command.
     */
    public Process exec( final ExecMetaData metaData )
        throws IOException, ExecException
    {
        final File directory = metaData.getWorkingDirectory().getCanonicalFile();
        if( ExecUtil.isCwd( directory ) )
        {
            return Runtime.getRuntime().
                exec( metaData.getCommand(), metaData.getEnvironment() );
        }

        //WARNING: This is an ugly hack and not thread safe in the slightest way
        //It can have really really undersirable side-effects if multiple threads
        //are running in the JVM
        try
        {
            System.setProperty( "user.dir", directory.toString() );
            return Runtime.getRuntime().
                exec( metaData.getCommand(), metaData.getEnvironment() );
        }
        finally
        {
            System.setProperty( "user.dir", ExecUtil.getCwd().toString() );
        }
    }
}
