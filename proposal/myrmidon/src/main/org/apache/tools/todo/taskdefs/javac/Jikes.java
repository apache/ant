/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.javac;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.Path;
import org.apache.tools.todo.util.FileUtils;
import org.apache.tools.todo.taskdefs.javac.DefaultCompilerAdapter;

/**
 * The implementation of the jikes compiler. This is primarily a cut-and-paste
 * from the original javac task before it was refactored.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */
public class Jikes
    extends DefaultCompilerAdapter
{

    /**
     * Performs a compile using the Jikes compiler from IBM.. Mostly of this
     * code is identical to doClassicCompile() However, it does not support all
     * options like bootclasspath, extdirs, deprecation and so on, because there
     * is no option in jikes and I don't understand what they should do. It has
     * been successfully tested with jikes >1.10
     *
     * @return Description of the Returned Value
     * @exception org.apache.myrmidon.api.TaskException Description of Exception
     * @author skanthak@muehlheim.de
     */
    public boolean execute()
        throws TaskException
    {
        getTaskContext().debug( "Using jikes compiler" );

        Path classpath = new Path();

        // Jikes doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if( m_bootclasspath != null )
        {
            classpath.append( m_bootclasspath );
        }

        // Jikes doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        addExtdirs( classpath );

        if( ( m_bootclasspath == null ) || ( m_bootclasspath.size() == 0 ) )
        {
            // no bootclasspath, therefore, get one from the java runtime
            m_includeJavaRuntime = true;
        }
        else
        {
            // there is a bootclasspath stated.  By default, the
            // includeJavaRuntime is false.  If the user has stated a
            // bootclasspath and said to include the java runtime, it's on
            // their head!
        }
        classpath.append( getCompileClasspath() );

        // Jikes has no option for source-path so we
        // will add it to classpath.
        classpath.append( src );

        // if the user has set JIKESPATH we should add the contents as well
        String jikesPath = System.getProperty( "jikes.class.path" );
        if( jikesPath != null )
        {
            classpath.append( new Path( jikesPath ) );
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable( "jikes" );

        if( m_deprecation == true )
        {
            cmd.addArgument( "-deprecation" );
        }

        if( m_destDir != null )
        {
            cmd.addArgument( "-d" );
            cmd.addArgument( m_destDir );
        }

        cmd.addArgument( "-classpath" );
        cmd.addArguments( FileUtils.translateCommandline( classpath ) );

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
            cmd.addArgument( "-O" );
        }
        if( m_verbose )
        {
            cmd.addArgument( "-verbose" );
        }
        if( m_depend )
        {
            cmd.addArgument( "-depend" );
        }

        if( m_attributes.getNowarn() )
        {
            /*
             * FIXME later
             *
             * let the magic property win over the attribute for backwards
             * compatibility
             */
            cmd.addArgument( "-nowarn" );
        }

        addCurrentCompilerArgs( cmd );

        int firstFileName = cmd.size();
        logAndAddFilesToCompile( cmd );

        return executeExternalCompile( cmd.getCommandline(), firstFileName ) == 0;
    }

}
