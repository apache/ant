/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.java;

import org.apache.myrmidon.framework.file.FileList;
import org.apache.myrmidon.framework.file.Path;
import org.apache.myrmidon.framework.FileSet;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.aut.nativelib.Os;
import java.util.Locale;
import java.io.File;

/**
 * A FileList that evaluates to the runtime class-path for this JVM.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="path" name="java-runtime"
 */
public class JavaRuntimeClassPath
    implements FileList
{
    /**
     * Returns the files in this list.
     *
     * @param context the context to use to evaluate the list.
     * @return The names of the files in this list.  All names are absolute paths.
     */
    public String[] listFiles( final TaskContext context )
        throws TaskException
    {
        final Path path = new Path();

        if( System.getProperty( "java.vendor" ).toLowerCase( Locale.US ).indexOf( "microsoft" ) >= 0 )
        {
            // Pull in *.zip from packages directory
            FileSet msZipFiles = new FileSet();
            msZipFiles.setDir( new File( System.getProperty( "java.home" ) + File.separator + "Packages" ) );
            msZipFiles.setIncludes( "*.ZIP" );
            path.addFileset( msZipFiles );
        }
        else if( "Kaffe".equals( System.getProperty( "java.vm.name" ) ) )
        {
            FileSet kaffeJarFiles = new FileSet();
            kaffeJarFiles.setDir( new File( System.getProperty( "java.home" )
                                            + File.separator + "share"
                                            + File.separator + "kaffe" ) );

            kaffeJarFiles.setIncludes( "*.jar" );
            path.addFileset( kaffeJarFiles );
        }
        else if( Os.isFamily( Os.OS_FAMILY_OSX ) )
        {
            // MacOS X
            final String classDir = System.getProperty( "java.home" ) +
                File.separator + ".." + File.separator + "Classes";
            final File classes = new File( classDir, "classes.jar" );
            path.addLocation( classes );
            final File ui = new File( classDir, "ui.jar" );
            path.addLocation( ui );
        }
        else
        {
            // JDK > 1.1 sets java.home to the JRE directory.
            final String rt = System.getProperty( "java.home" ) +
                File.separator + "lib" + File.separator + "rt.jar";
            final File rtJar = new File( rt );
            path.addLocation( rtJar );
        }

        return path.listFiles( context );
    }
}
