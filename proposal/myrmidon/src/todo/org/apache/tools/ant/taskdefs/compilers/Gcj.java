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
 * The implementation of the gcj compiler. This is primarily a cut-and-paste
 * from the jikes.
 *
 * @author <a href="mailto:tora@debian.org">Takashi Okamoto</a>
 */
public class Gcj extends DefaultCompilerAdapter
{

    /**
     * Performs a compile using the gcj compiler.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     * @author tora@debian.org
     */
    public boolean execute()
        throws TaskException
    {
        Commandline cmd;
        attributes.log( "Using gcj compiler", Project.MSG_VERBOSE );
        cmd = setupGCJCommand();

        int firstFileName = cmd.size();
        logAndAddFilesToCompile( cmd );

        return executeExternalCompile( cmd.getCommandline(), firstFileName ) == 0;
    }

    protected Commandline setupGCJCommand()
    {
        Commandline cmd = new Commandline();
        Path classpath = new Path( project );

        // gcj doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if( bootclasspath != null )
        {
            classpath.append( bootclasspath );
        }

        // gcj doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        classpath.addExtdirs( extdirs );

        if( ( bootclasspath == null ) || ( bootclasspath.size() == 0 ) )
        {
            // no bootclasspath, therefore, get one from the java runtime
            includeJavaRuntime = true;
        }
        classpath.append( getCompileClasspath() );

        // Gcj has no option for source-path so we
        // will add it to classpath.
        classpath.append( src );

        cmd.setExecutable( "gcj" );

        if( destDir != null )
        {
            cmd.createArgument().setValue( "-d" );
            cmd.createArgument().setFile( destDir );

            if( destDir.mkdirs() )
            {
                throw new TaskException( "Can't make output directories. Maybe permission is wrong. " );
            }
            ;
        }

        cmd.createArgument().setValue( "-classpath" );
        cmd.createArgument().setPath( classpath );

        if( encoding != null )
        {
            cmd.createArgument().setValue( "--encoding=" + encoding );
        }
        if( debug )
        {
            cmd.createArgument().setValue( "-g1" );
        }
        if( optimize )
        {
            cmd.createArgument().setValue( "-O" );
        }

        /**
         * gcj should be set for generate class.
         */
        cmd.createArgument().setValue( "-C" );

        addCurrentCompilerArgs( cmd );

        return cmd;
    }
}
