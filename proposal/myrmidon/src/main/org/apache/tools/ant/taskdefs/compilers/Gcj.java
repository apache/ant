/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.compilers;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

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
        getLogger().debug( "Using gcj compiler" );
        cmd = setupGCJCommand();

        int firstFileName = cmd.size();
        logAndAddFilesToCompile( cmd );

        return executeExternalCompile( cmd.getCommandline(), firstFileName ) == 0;
    }

    protected Commandline setupGCJCommand()
        throws TaskException
    {
        Commandline cmd = new Commandline();
        Path classpath = new Path();

        // gcj doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if( m_bootclasspath != null )
        {
            classpath.append( m_bootclasspath );
        }

        // gcj doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        addExtdirs( classpath );

        if( ( m_bootclasspath == null ) || ( m_bootclasspath.size() == 0 ) )
        {
            // no bootclasspath, therefore, get one from the java runtime
            m_includeJavaRuntime = true;
        }
        classpath.append( getCompileClasspath() );

        // Gcj has no option for source-path so we
        // will add it to classpath.
        classpath.append( src );

        cmd.setExecutable( "gcj" );

        if( m_destDir != null )
        {
            cmd.addArgument( "-d" );
            cmd.addArgument( m_destDir );

            if( m_destDir.mkdirs() )
            {
                throw new TaskException( "Can't make output directories. Maybe permission is wrong. " );
            }
            ;
        }

        cmd.addArgument( "-classpath" );
        cmd.addArguments( FileUtils.translateCommandline( classpath ) );

        if( m_encoding != null )
        {
            cmd.addArgument( "--encoding=" + m_encoding );
        }
        if( m_debug )
        {
            cmd.addArgument( "-g1" );
        }
        if( m_optimize )
        {
            cmd.addArgument( "-O" );
        }

        /**
         * gcj should be set for generate class.
         */
        cmd.addArgument( "-C" );

        addCurrentCompilerArgs( cmd );

        return cmd;
    }
}
