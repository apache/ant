/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.compilers;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

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
public class Jikes extends DefaultCompilerAdapter
{

    /**
     * Performs a compile using the Jikes compiler from IBM.. Mostly of this
     * code is identical to doClassicCompile() However, it does not support all
     * options like bootclasspath, extdirs, deprecation and so on, because there
     * is no option in jikes and I don't understand what they should do. It has
     * been successfully tested with jikes >1.10
     *
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     * @author skanthak@muehlheim.de
     */
    public boolean execute()
        throws BuildException
    {
        attributes.log( "Using jikes compiler", Project.MSG_VERBOSE );

        Path classpath = new Path( project );

        // Jikes doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if( bootclasspath != null )
        {
            classpath.append( bootclasspath );
        }

        // Jikes doesn't support an extension dir (-extdir)
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

        // Jikes has no option for source-path so we
        // will add it to classpath.
        classpath.append( src );

        // if the user has set JIKESPATH we should add the contents as well
        String jikesPath = System.getProperty( "jikes.class.path" );
        if( jikesPath != null )
        {
            classpath.append( new Path( project, jikesPath ) );
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable( "jikes" );

        if( deprecation == true )
            cmd.createArgument().setValue( "-deprecation" );

        if( destDir != null )
        {
            cmd.createArgument().setValue( "-d" );
            cmd.createArgument().setFile( destDir );
        }

        cmd.createArgument().setValue( "-classpath" );
        cmd.createArgument().setPath( classpath );

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
            cmd.createArgument().setValue( "-O" );
        }
        if( verbose )
        {
            cmd.createArgument().setValue( "-verbose" );
        }
        if( depend )
        {
            cmd.createArgument().setValue( "-depend" );
        }
        /**
         * XXX Perhaps we shouldn't use properties for these three options
         * (emacs mode, warnings and pedantic), but include it in the javac
         * directive?
         */

        /**
         * Jikes has the nice feature to print error messages in a form readable
         * by emacs, so that emacs can directly set the cursor to the place,
         * where the error occured.
         */
        String emacsProperty = project.getProperty( "build.compiler.emacs" );
        if( emacsProperty != null && Project.toBoolean( emacsProperty ) )
        {
            cmd.createArgument().setValue( "+E" );
        }

        if( attributes.getNowarn() )
        {
            /*
             * FIXME later
             *
             * let the magic property win over the attribute for backwards
             * compatibility
             */
            cmd.createArgument().setValue( "-nowarn" );
        }

        /**
         * Jikes can issue pedantic warnings.
         */
        String pedanticProperty = project.getProperty( "build.compiler.pedantic" );
        if( pedanticProperty != null && Project.toBoolean( pedanticProperty ) )
        {
            cmd.createArgument().setValue( "+P" );
        }

        /**
         * Jikes supports something it calls "full dependency checking", see the
         * jikes documentation for differences between -depend and +F.
         */
        String fullDependProperty = project.getProperty( "build.compiler.fulldepend" );
        if( fullDependProperty != null && Project.toBoolean( fullDependProperty ) )
        {
            cmd.createArgument().setValue( "+F" );
        }

        addCurrentCompilerArgs( cmd );

        int firstFileName = cmd.size();
        logAndAddFilesToCompile( cmd );

        return executeExternalCompile( cmd.getCommandline(), firstFileName ) == 0;
    }

}
