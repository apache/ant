/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.compilers;
import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * The implementation of the Java compiler for KJC. This is primarily a
 * cut-and-paste from Jikes.java and DefaultCompilerAdapter.
 *
 * @author <a href="mailto:tora@debian.org">Takashi Okamoto</a> +
 */
public class Kjc extends DefaultCompilerAdapter
{

    public boolean execute()
        throws BuildException
    {
        attributes.log( "Using kjc compiler", Project.MSG_VERBOSE );
        Commandline cmd = setupKjcCommand();

        try
        {
            Class c = Class.forName( "at.dms.kjc.Main" );

            // Call the compile() method
            Method compile = c.getMethod( "compile",
                new Class[]{String[].class} );
            Boolean ok = ( Boolean )compile.invoke( null,
                new Object[]{cmd.getArguments()} );
            return ok.booleanValue();
        }
        catch( ClassNotFoundException ex )
        {
            throw new BuildException( "Cannot use kjc compiler, as it is not available" +
                " A common solution is to set the environment variable" +
                " CLASSPATH to your kjc archive (kjc.jar)." );
        }
        catch( Exception ex )
        {
            if( ex instanceof BuildException )
            {
                throw ( BuildException )ex;
            }
            else
            {
                throw new BuildException( "Error starting kjc compiler: ", ex );
            }
        }
    }

    /**
     * setup kjc command arguments.
     *
     * @return Description of the Returned Value
     */
    protected Commandline setupKjcCommand()
    {
        Commandline cmd = new Commandline();

        // generate classpath, because kjc does't support sourcepath.
        Path classpath = getCompileClasspath();

        if( deprecation == true )
        {
            cmd.createArgument().setValue( "-deprecation" );
        }

        if( destDir != null )
        {
            cmd.createArgument().setValue( "-d" );
            cmd.createArgument().setFile( destDir );
        }

        // generate the clsspath
        cmd.createArgument().setValue( "-classpath" );

        Path cp = new Path( project );

        // kjc don't have bootclasspath option.
        if( bootclasspath != null )
        {
            cp.append( bootclasspath );
        }

        if( extdirs != null )
        {
            cp.addExtdirs( extdirs );
        }

        cp.append( classpath );
        cp.append( src );

        cmd.createArgument().setPath( cp );

        // kjc-1.5A doesn't support -encoding option now.
        // but it will be supported near the feature.
        if( encoding != null )
        {
            cmd.createArgument().setValue( "-encoding" );
            cmd.createArgument().setValue( encoding );
        }

        if( debug )
        {
            cmd.createArgument().setValue( "-g" );
        }

        if( optimize )
        {
            cmd.createArgument().setValue( "-O2" );
        }

        if( verbose )
        {
            cmd.createArgument().setValue( "-verbose" );
        }

        addCurrentCompilerArgs( cmd );

        logAndAddFilesToCompile( cmd );
        return cmd;
    }
}

