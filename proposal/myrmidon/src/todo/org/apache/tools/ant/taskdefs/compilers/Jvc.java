/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.compilers;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * The implementation of the jvc compiler from microsoft. This is primarily a
 * cut-and-paste from the original javac task before it was refactored.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */
public class Jvc extends DefaultCompilerAdapter
{

    public boolean execute()
        throws TaskException
    {
        attributes.log( "Using jvc compiler", Project.MSG_VERBOSE );

        Path classpath = new Path( project );

        // jvc doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if( bootclasspath != null )
        {
            classpath.append( bootclasspath );
        }

        // jvc doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        classpath.addExtdirs( extdirs );

        if( ( bootclasspath == null ) || ( bootclasspath.size() == 0 ) )
        {
            // no bootclasspath, therefore, get one from the java runtime
            includeJavaRuntime = true;
        }
        else
        {
            // there is a bootclasspath stated.  By default, the
            // includeJavaRuntime is false.  If the user has stated a
            // bootclasspath and said to include the java runtime, it's on
            // their head!
        }
        classpath.append( getCompileClasspath() );

        // jvc has no option for source-path so we
        // will add it to classpath.
        classpath.append( src );

        Commandline cmd = new Commandline();
        cmd.setExecutable( "jvc" );

        if( destDir != null )
        {
            cmd.createArgument().setValue( "/d" );
            cmd.createArgument().setFile( destDir );
        }

        // Add the Classpath before the "internal" one.
        cmd.createArgument().setValue( "/cp:p" );
        cmd.createArgument().setPath( classpath );

        // Enable MS-Extensions and ...
        cmd.createArgument().setValue( "/x-" );
        // ... do not display a Message about this.
        cmd.createArgument().setValue( "/nomessage" );
        // Do not display Logo
        cmd.createArgument().setValue( "/nologo" );

        if( debug )
        {
            cmd.createArgument().setValue( "/g" );
        }
        if( optimize )
        {
            cmd.createArgument().setValue( "/O" );
        }
        if( verbose )
        {
            cmd.createArgument().setValue( "/verbose" );
        }

        addCurrentCompilerArgs( cmd );

        int firstFileName = cmd.size();
        logAndAddFilesToCompile( cmd );

        return executeExternalCompile( cmd.getCommandline(), firstFileName ) == 0;
    }
}
