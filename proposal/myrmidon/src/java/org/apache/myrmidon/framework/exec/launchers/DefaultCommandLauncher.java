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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.framework.exec.CommandLauncher;
import org.apache.myrmidon.framework.exec.Environment;
import org.apache.myrmidon.framework.exec.ExecException;
import org.apache.myrmidon.framework.exec.ExecMetaData;

/**
 * A command launcher for a particular JVM/OS platform. This class is a
 * general purpose command launcher which can only launch commands in the
 * current working directory.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>
 * @version $Revision$ $Date$
 */
public class DefaultCommandLauncher
    implements CommandLauncher
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultCommandLauncher.class );

    private static final Method c_execWithCWD;

    static
    {
        // Locate method Runtime.exec(String[] cmdarray, String[] envp, File dir)
        Method method = null;
        try
        {
            final Class[] types =
                new Class[]{String[].class, String[].class, File.class};
            method = Runtime.class.getMethod( "exec", types );
        }
        catch( final NoSuchMethodException nsme )
        {
            //ignore
        }

        c_execWithCWD = method;
    }

    /**
     * Execute the specified native command.
     *
     * @param metaData the native command to execute
     * @return the Process launched by the CommandLauncher
     * @exception IOException is thrown when the native code can not
     *            launch the application for some reason. Usually due
     *            to the command not being fully specified and not in
     *            the PATH env var.
     * @exception ExecException if the command launcher detects that
     *            it can not execute the native command for some reason.
     */
    public Process exec( final ExecMetaData metaData )
        throws IOException, ExecException
    {
        if( ExecUtil.isCwd( metaData.getWorkingDirectory() ) )
        {
            final String[] env = getEnvironmentSpec( metaData );
            return Runtime.getRuntime().exec( metaData.getCommand(), env );
        }
        else if( null == c_execWithCWD )
        {
            final String message = REZ.getString( "default.bad-dir.error" );
            throw new ExecException( message );
        }
        else
        {
            return execJava13( metaData );
        }
    }

    /**
     * Get the native environment according to proper rules.
     * Return null if no environment specified, return environment combined
     * with native environment if environment data is additive else just return
     * converted environment data.
     */
    private String[] getEnvironmentSpec( final ExecMetaData metaData )
        throws ExecException, IOException
    {
        final Properties environment = metaData.getEnvironment();
        if( 0 == environment.size() )
        {
            return null;
        }
        else
        {
            if( metaData.isEnvironmentAdditive() )
            {
                final Properties newEnvironment = new Properties();
                newEnvironment.putAll( Environment.getNativeEnvironment() );
                newEnvironment.putAll( environment );
                return ExecUtil.toNativeEnvironment( newEnvironment );
            }
            else
            {
                return ExecUtil.toNativeEnvironment( environment );
            }
        }
    }

    /**
     * Execute the Java1.3 Runtime.exec() 3 parame method that sets working
     * directory. This needs to be done via reflection so that it can compile
     * under 1.2.
     */
    private Process execJava13( final ExecMetaData metaData )
        throws IOException, ExecException
    {
        final String[] env = getEnvironmentSpec( metaData );
        final Object[] args =
            {metaData.getCommand(),
             env,
             metaData.getWorkingDirectory()};
        try
        {
            return (Process)c_execWithCWD.invoke( Runtime.getRuntime(), args );
        }
        catch( final IllegalAccessException iae )
        {
            throw new ExecException( iae.getMessage(), iae );
        }
        catch( final IllegalArgumentException iae )
        {
            throw new ExecException( iae.getMessage(), iae );
        }
        catch( final InvocationTargetException ite )
        {
            final Throwable t = ite.getTargetException();
            if( t instanceof IOException )
            {
                t.fillInStackTrace();
                throw (IOException)t;
            }
            else
            {
                throw new ExecException( t.getMessage(), t );
            }
        }
    }
}
