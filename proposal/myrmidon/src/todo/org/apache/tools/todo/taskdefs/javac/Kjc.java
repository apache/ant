/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.javac;

import java.lang.reflect.Method;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.ArgumentList;
import org.apache.myrmidon.framework.file.Path;
import org.apache.myrmidon.framework.file.FileListUtil;

/**
 * The implementation of the Java compiler for KJC. This is primarily a
 * cut-and-paste from Jikes.java and DefaultCompilerAdapter.
 *
 * @author <a href="mailto:tora@debian.org">Takashi Okamoto</a> +
 */
public class Kjc extends DefaultCompilerAdapter
{

    public boolean execute()
        throws TaskException
    {
        getTaskContext().debug( "Using kjc compiler" );
        ArgumentList cmd = setupKjcCommand();

        try
        {
            Class c = Class.forName( "at.dms.kjc.Main" );

            // Call the compile() method
            Method compile = c.getMethod( "compile",
                                          new Class[]{String[].class} );
            Boolean ok = (Boolean)compile.invoke( null,
                                                  new Object[]{cmd.getArguments()} );
            return ok.booleanValue();
        }
        catch( ClassNotFoundException ex )
        {
            throw new TaskException( "Cannot use kjc compiler, as it is not available" +
                                     " A common solution is to set the environment variable" +
                                     " CLASSPATH to your kjc archive (kjc.jar)." );
        }
        catch( Exception ex )
        {
            if( ex instanceof TaskException )
            {
                throw (TaskException)ex;
            }
            else
            {
                throw new TaskException( "Error starting kjc compiler: ", ex );
            }
        }
    }

    /**
     * setup kjc command arguments.
     *
     * @return Description of the Returned Value
     */
    protected ArgumentList setupKjcCommand()
        throws TaskException
    {
        ArgumentList cmd = new Commandline();

        // generate classpath, because kjc does't support sourcepath.
        Path classpath = new Path();
        addCompileClasspath( classpath );

        if( m_deprecation == true )
        {
            cmd.addArgument( "-deprecation" );
        }

        if( m_destDir != null )
        {
            cmd.addArgument( "-d" );
            cmd.addArgument( m_destDir );
        }

        // generate the clsspath
        cmd.addArgument( "-classpath" );

        Path cp = new Path();

        // kjc don't have bootclasspath option.
        cp.add( m_bootclasspath );

        if( m_extdirs != null )
        {
            addExtdirs( cp );
        }

        cp.add( classpath );
        cp.add( src );

        cmd.addArgument( FileListUtil.formatPath( cp, getTaskContext() ) );

        // kjc-1.5A doesn't support -encoding option now.
        // but it will be supported near the feature.
        if( m_encoding != null )
        {
            cmd.addArgument( "-encoding" );
            cmd.addArgument( m_encoding );
        }

        if( m_debug )
        {
            cmd.addArgument( "-g" );
        }

        if( m_optimize )
        {
            cmd.addArgument( "-O2" );
        }

        if( m_verbose )
        {
            cmd.addArgument( "-verbose" );
        }

        addCurrentCompilerArgs( cmd );

        logFilesToCompile( cmd );
        addFilesToCompile( cmd );
        return cmd;
    }
}

