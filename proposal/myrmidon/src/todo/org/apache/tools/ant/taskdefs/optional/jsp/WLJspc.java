/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.jsp;//java imports

import java.io.File;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;

/**
 * Class to precompile JSP's using weblogic's jsp compiler (weblogic.jspc)
 *
 * @author <a href="mailto:avik@aviksengupta.com">Avik Sengupta</a>
 *      http://www.webteksoftware.com Tested only on Weblogic 4.5.1 - NT4.0 and
 *      Solaris 5.7 required attributes src : root of source tree for JSP, ie,
 *      the document root for your weblogic server dest : root of destination
 *      directory, what you have set as WorkingDir in the weblogic properties
 *      package : start package name under which your JSP's would be compiled
 *      other attributes classpath A classpath should be set which contains the
 *      weblogic classes as well as all application classes referenced by the
 *      JSP. The system classpath is also appended when the jspc is called, so
 *      you may choose to put everything in the classpath while calling Ant.
 *      However, since presumably the JSP's will reference classes being build
 *      by Ant, it would be better to explicitly add the classpath in the task
 *      The task checks timestamps on the JSP's and the generated classes, and
 *      compiles only those files that have changed. It follows the weblogic
 *      naming convention of putting classes in <b> _dirName/_fileName.class for
 *      dirname/fileName.jsp </b> Limitation: It compiles the files thru the
 *      Classic compiler only. Limitation: Since it is my experience that
 *      weblogic jspc throws out of memory error on being given too many files
 *      at one go, it is called multiple times with one jsp file each. <pre>
 * example
 * &lt;target name="jspcompile" depends="compile"&gt;
 *   &lt;wljspc src="c:\\weblogic\\myserver\\public_html" dest="c:\\weblogic\\myserver\\serverclasses" package="myapp.jsp"&gt;
 *   &lt;classpath&gt;
 *          &lt;pathelement location="${weblogic.classpath}" /&gt;
 *           &lt;pathelement path="${compile.dest}" /&gt;
 *      &lt;/classpath&gt;
 *
 *   &lt;/wljspc&gt;
 * &lt;/target&gt;
 * </pre>
 */

public class WLJspc extends MatchingTask
{//classpath used to compile the jsp files.
    //private String compilerPath; //fully qualified name for the compiler executable

    private String pathToPackage = "";
    private Vector filesToDo = new Vector();//package under which resultant classes will reside
    private Path compileClasspath;
    //TODO Test on other versions of weblogic
    //TODO add more attributes to the task, to take care of all jspc options
    //TODO Test on Unix

    private File destinationDirectory;// root of source files tree
    private String destinationPackage;//root of compiled files tree
    private File sourceDirectory;

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
        throws TaskException
    {
        if( compileClasspath == null )
        {
            compileClasspath = classpath;
        }
        else
        {
            compileClasspath.append( classpath );
        }
    }

    /**
     * Set the directory containing the source jsp's
     *
     * @param dirName the directory containg the source jsp's
     */
    public void setDest( File dirName )
    {
        destinationDirectory = dirName;
    }

    /**
     * Set the package under which the compiled classes go
     *
     * @param packageName the package name for the clases
     */
    public void setPackage( String packageName )
    {

        destinationPackage = packageName;
    }

    /**
     * Set the directory containing the source jsp's
     *
     * @param dirName the directory containg the source jsp's
     */
    public void setSrc( File dirName )
    {

        sourceDirectory = dirName;
    }

    /**
     * Maybe creates a nested classpath element.
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( compileClasspath == null )
        {
            compileClasspath = new Path( project );
        }
        return compileClasspath;
    }

    public void execute()
        throws TaskException
    {
        if( !destinationDirectory.isDirectory() )
        {
            throw new TaskException( "destination directory " + destinationDirectory.getPath() +
                                     " is not valid" );
        }

        if( !sourceDirectory.isDirectory() )
        {
            throw new TaskException( "src directory " + sourceDirectory.getPath() +
                                     " is not valid" );
        }

        if( destinationPackage == null )
        {
            throw new TaskException( "package attribute must be present." );
        }

        String systemClassPath = System.getProperty( "java.class.path" );

        pathToPackage = this.destinationPackage.replace( '.', File.separatorChar );
        // get all the files in the sourceDirectory
        DirectoryScanner ds = super.getDirectoryScanner( sourceDirectory );

        //use the systemclasspath as well, to include the ant jar
        if( compileClasspath == null )
        {
            compileClasspath = new Path( project );
        }

        compileClasspath.append( Path.systemClasspath );
        String[] files = ds.getIncludedFiles();

        //Weblogic.jspc calls System.exit() ... have to fork
        // Therefore, takes loads of time
        // Can pass directories at a time (*.jsp) but easily runs out of memory on hefty dirs
        // (even on  a Sun)
        Java helperTask = (Java)project.createTask( "java" );
        helperTask.setFork( true );
        helperTask.setClassname( "weblogic.jspc" );
        String[] args = new String[ 12 ];

        File jspFile = null;
        String parents = "";
        String arg = "";
        int j = 0;
        //XXX  this array stuff is a remnant of prev trials.. gotta remove.
        args[ j++ ] = "-d";
        args[ j++ ] = destinationDirectory.getAbsolutePath().trim();
        args[ j++ ] = "-docroot";
        args[ j++ ] = sourceDirectory.getAbsolutePath().trim();
        args[ j++ ] = "-keepgenerated";//TODO: Parameterise ??
        //Call compiler as class... dont want to fork again
        //Use classic compiler -- can be parameterised?
        args[ j++ ] = "-compilerclass";
        args[ j++ ] = "sun.tools.javac.Main";
        //Weblogic jspc does not seem to work unless u explicitly set this...
        // Does not take the classpath from the env....
        // Am i missing something about the Java task??
        args[ j++ ] = "-classpath";
        args[ j++ ] = compileClasspath.toString();

        this.scanDir( files );
        log( "Compiling " + filesToDo.size() + " JSP files" );

        for( int i = 0; i < filesToDo.size(); i++ )
        {
            //XXX
            // All this to get package according to weblogic standards
            // Can be written better... this is too hacky!
            // Careful.. similar code in scanDir , but slightly different!!
            jspFile = new File( (String)filesToDo.elementAt( i ) );
            args[ j ] = "-package";
            parents = jspFile.getParent();
            if( ( parents != null ) && ( !( "" ).equals( parents ) ) )
            {
                parents = this.replaceString( parents, File.separator, "_." );
                args[ j + 1 ] = destinationPackage + "." + "_" + parents;
            }
            else
            {
                args[ j + 1 ] = destinationPackage;
            }

            args[ j + 2 ] = sourceDirectory + File.separator + (String)filesToDo.elementAt( i );
            arg = "";

            for( int x = 0; x < 12; x++ )
            {
                arg += " " + args[ x ];
            }

            System.out.println( "arg = " + arg );

            helperTask.clearArgs();
            helperTask.setArgs( arg );
            helperTask.setClasspath( compileClasspath );
            if( helperTask.executeJava() != 0 )
            {
                log( files[ i ] + " failed to compile", Project.MSG_WARN );
            }
        }
    }

    protected String replaceString( String inpString, String escapeChars, String replaceChars )
    {
        String localString = "";
        int numTokens = 0;
        StringTokenizer st = new StringTokenizer( inpString, escapeChars, true );
        numTokens = st.countTokens();
        for( int i = 0; i < numTokens; i++ )
        {
            String test = st.nextToken();
            test = ( test.equals( escapeChars ) ? replaceChars : test );
            localString += test;
        }
        return localString;
    }

    protected void scanDir( String files[] )
    {

        long now = ( new Date() ).getTime();
        File jspFile = null;
        String parents = null;
        String pack = "";
        for( int i = 0; i < files.length; i++ )
        {
            File srcFile = new File( this.sourceDirectory, files[ i ] );
            //XXX
            // All this to convert source to destination directory according to weblogic standards
            // Can be written better... this is too hacky!
            jspFile = new File( files[ i ] );
            parents = jspFile.getParent();
            int loc = 0;

            if( ( parents != null ) && ( !( "" ).equals( parents ) ) )
            {
                parents = this.replaceString( parents, File.separator, "_/" );
                pack = pathToPackage + File.separator + "_" + parents;
            }
            else
            {
                pack = pathToPackage;
            }

            String filePath = pack + File.separator + "_";
            int startingIndex
                = files[ i ].lastIndexOf( File.separator ) != -1 ? files[ i ].lastIndexOf( File.separator ) + 1 : 0;
            int endingIndex = files[ i ].indexOf( ".jsp" );
            if( endingIndex == -1 )
            {
                break;
            }

            filePath += files[ i ].substring( startingIndex, endingIndex );
            filePath += ".class";
            File classFile = new File( this.destinationDirectory, filePath );

            if( srcFile.lastModified() > now )
            {
                log( "Warning: file modified in the future: " +
                     files[ i ], Project.MSG_WARN );
            }
            if( srcFile.lastModified() > classFile.lastModified() )
            {
                //log("Files are" + srcFile.getAbsolutePath()+" " +classFile.getAbsolutePath());
                filesToDo.addElement( files[ i ] );
                log( "Recompiling File " + files[ i ], Project.MSG_VERBOSE );
            }
        }
    }
}
