/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.java;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.nativelib.Execute;
import org.apache.myrmidon.framework.java.JavaRuntimeClassPath;
import org.apache.myrmidon.framework.file.Path;
import org.apache.myrmidon.framework.file.FileListUtil;

/**
 * An adaptor for the jikes compiler.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @author skanthak@muehlheim.de
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="java-compiler" name="jikes"
 */
public class JikesAdaptor
    extends ExternalCompilerAdaptor
{
    /**
     * Builds the command-line to execute the compiler.
     */
    protected void buildCommandLine( final Execute exe, final File tempFile )
        throws TaskException
    {
        final Path classpath = new Path();

        // Add the destination directory
        classpath.addLocation( getDestDir() );

        // Add the compile classpath
        classpath.add( getClassPath() );

        // If the user has set JIKESPATH we should add the contents as well
        String jikesPath = System.getProperty( "jikes.class.path" );
        if( jikesPath != null )
        {
            classpath.add( jikesPath );
        }

        // Add the runtime
        classpath.add( new JavaRuntimeClassPath() );

        // Build the command line
        exe.setExecutable( "jikes" );

        if( isDeprecation() )
        {
            exe.addArgument( "-deprecation" );
        }

        if( isDebug() )
        {
            exe.addArgument( "-g" );
        }

        exe.addArgument( "-d" );
        exe.addArgument( getDestDir() );

        exe.addArgument( "-classpath" );
        exe.addArgument( FileListUtil.formatPath( classpath, getContext() ) );

        // TODO - make this configurable
        exe.addArgument( "+E" );

        exe.addArgument( "@" + tempFile.getAbsolutePath() );
    }
}
