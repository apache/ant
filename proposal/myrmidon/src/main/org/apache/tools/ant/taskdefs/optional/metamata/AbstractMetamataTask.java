/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.ExecuteStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

/**
 * Somewhat abstract framework to be used for other metama 2.0 tasks. This
 * should include, audit, metrics, cover and mparse. For more information, visit
 * the website at <a href="http://www.metamata.com">www.metamata.com</a>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public abstract class AbstractMetamataTask extends Task
{

    //--------------------------- ATTRIBUTES -----------------------------------

    /**
     * The user classpath to be provided. It matches the -classpath of the
     * command line. The classpath must includes both the <tt>.class</tt> and
     * the <tt>.java</tt> files for accurate audit.
     */
    protected Path classPath = null;

    /**
     * the path to the source file
     */
    protected Path sourcePath = null;

    /**
     * Metamata home directory. It will be passed as a <tt>metamata.home</tt>
     * property and should normally matches the environment property <tt>
     * META_HOME</tt> set by the Metamata installer.
     */
    protected File metamataHome = null;

    /**
     * the command line used to run MAudit
     */
    protected CommandlineJava cmdl = new CommandlineJava();

    /**
     * the set of files to be audited
     */
    protected ArrayList fileSets = new ArrayList();

    /**
     * the options file where are stored the command line options
     */
    protected File optionsFile = null;

    // this is used to keep track of which files were included. It will
    // be set when calling scanFileSets();
    protected Hashtable includedFiles = null;

    public AbstractMetamataTask()
    {
    }

    /**
     * initialize the task with the classname of the task to run
     *
     * @param className Description of Parameter
     */
    protected AbstractMetamataTask( String className )
    {
        cmdl.setVm( "java" );
        cmdl.setClassname( className );
    }

    /**
     * convenient method for JDK 1.1. Will copy all elements from src to dest
     *
     * @param dest The feature to be added to the AllArrayList attribute
     * @param files The feature to be added to the AllArrayList attribute
     */
    protected final static void addAllArrayList( ArrayList dest, Iterator files )
    {
        while( files.hasNext() )
        {
            dest.add( files.next() );
        }
    }

    protected final static File createTmpFile()
    {
        // must be compatible with JDK 1.1 !!!!
        final long rand = ( new Random( System.currentTimeMillis() ) ).nextLong();
        File file = new File( "metamata" + rand + ".tmp" );
        return file;
    }

    /**
     * -mx or -Xmx depending on VM version
     *
     * @param max The new Maxmemory value
     */
    public void setMaxmemory( String max )
    {
        if( Project.getJavaVersion().startsWith( "1.1" ) )
        {
            createJvmarg().setValue( "-mx" + max );
        }
        else
        {
            createJvmarg().setValue( "-Xmx" + max );
        }
    }

    /**
     * the metamata.home property to run all tasks.
     *
     * @param metamataHome The new Metamatahome value
     */
    public void setMetamatahome( final File metamataHome )
    {
        this.metamataHome = metamataHome;
    }

    /**
     * The java files or directory to be audited
     *
     * @param fs The feature to be added to the FileSet attribute
     */
    public void addFileSet( FileSet fs )
    {
        fileSets.add( fs );
    }

    /**
     * user classpath
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( classPath == null )
        {
            classPath = new Path( getProject() );
        }
        return classPath;
    }

    /**
     * Creates a nested jvmarg element.
     *
     * @return Description of the Returned Value
     */
    public Commandline.Argument createJvmarg()
    {
        return cmdl.createVmArgument();
    }

    /**
     * create the source path for this task
     *
     * @return Description of the Returned Value
     */
    public Path createSourcepath()
    {
        if( sourcePath == null )
        {
            sourcePath = new Path( getProject() );
        }
        return sourcePath;
    }

    /**
     * execute the command line
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        try
        {
            setUp();
            ExecuteStreamHandler handler = createStreamHandler();
            execute0( handler );
        }
        finally
        {
            cleanUp();
        }
    }

    //--------------------- PRIVATE/PROTECTED METHODS --------------------------

    /**
     * check the options and build the command line
     *
     * @exception TaskException Description of Exception
     */
    protected void setUp()
        throws TaskException
    {
        checkOptions();

        // set the classpath as the jar file
        File jar = getMetamataJar( metamataHome );
        final Path classPath = cmdl.createClasspath( getProject() );
        classPath.createPathElement().setLocation( jar );

        // set the metamata.home property
        final Commandline.Argument vmArgs = cmdl.createVmArgument();
        vmArgs.setValue( "-Dmetamata.home=" + metamataHome.getAbsolutePath() );

        // retrieve all the files we want to scan
        includedFiles = scanFileSets();
        log( includedFiles.size() + " files added for audit", Project.MSG_VERBOSE );

        // write all the options to a temp file and use it ro run the process
        ArrayList options = getOptions();
        optionsFile = createTmpFile();
        generateOptionsFile( optionsFile, options );
        Commandline.Argument args = cmdl.createArgument();
        args.setLine( "-arguments " + optionsFile.getAbsolutePath() );
    }

    /**
     * return the location of the jar file used to run
     *
     * @param home Description of Parameter
     * @return The MetamataJar value
     */
    protected final File getMetamataJar( File home )
    {
        return new File( new File( home.getAbsolutePath() ), "lib/metamata.jar" );
    }

    protected Hashtable getFileMapping()
    {
        return includedFiles;
    }

    /**
     * return all options of the command line as string elements
     *
     * @return The Options value
     */
    protected abstract ArrayList getOptions();

    /**
     * validate options set
     *
     * @exception TaskException Description of Exception
     */
    protected void checkOptions()
        throws TaskException
    {
        // do some validation first
        if( metamataHome == null || !metamataHome.exists() )
        {
            throw new TaskException( "'metamatahome' must point to Metamata home directory." );
        }
        metamataHome = resolveFile( metamataHome.getPath() );
        File jar = getMetamataJar( metamataHome );
        if( !jar.exists() )
        {
            throw new TaskException( jar + " does not exist. Check your metamata installation." );
        }
    }

    /**
     * clean up all the mess that we did with temporary objects
     */
    protected void cleanUp()
    {
        if( optionsFile != null )
        {
            optionsFile.delete();
            optionsFile = null;
        }
    }

    /**
     * create a stream handler that will be used to get the output since
     * metamata tools do not report with convenient files such as XML.
     *
     * @return Description of the Returned Value
     */
    protected abstract ExecuteStreamHandler createStreamHandler();

    /**
     * execute the process with a specific handler
     *
     * @param handler Description of Parameter
     * @exception TaskException Description of Exception
     */
    protected void execute0( ExecuteStreamHandler handler )
        throws TaskException
    {
        final Execute process = new Execute( handler );
        log( cmdl.toString(), Project.MSG_VERBOSE );
        process.setCommandline( cmdl.getCommandline() );
        try
        {
            if( process.execute() != 0 )
            {
                throw new TaskException( "Metamata task failed." );
            }
        }
        catch( IOException e )
        {
            throw new TaskException( "Failed to launch Metamata task: " + e );
        }
    }

    protected void generateOptionsFile( File tofile, ArrayList options )
        throws TaskException
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter( tofile );
            PrintWriter pw = new PrintWriter( fw );
            final int size = options.size();
            for( int i = 0; i < size; i++ )
            {
                pw.println( options.get( i ) );
            }
            pw.flush();
        }
        catch( IOException e )
        {
            throw new TaskException( "Error while writing options file " + tofile, e );
        }
        finally
        {
            if( fw != null )
            {
                try
                {
                    fw.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }
    }

    /**
     * @return the list of .java files (as their absolute path) that should be
     *      audited.
     */
    protected Hashtable scanFileSets()
    {
        Hashtable files = new Hashtable();
        for( int i = 0; i < fileSets.size(); i++ )
        {
            FileSet fs = (FileSet)fileSets.get( i );
            DirectoryScanner ds = fs.getDirectoryScanner( getProject() );
            ds.scan();
            String[] f = ds.getIncludedFiles();
            log( i + ") Adding " + f.length + " files from directory " + ds.getBasedir(), Project.MSG_VERBOSE );
            for( int j = 0; j < f.length; j++ )
            {
                String pathname = f[ j ];
                if( pathname.endsWith( ".java" ) )
                {
                    File file = new File( ds.getBasedir(), pathname );
                    //                  file = project.resolveFile(file.getAbsolutePath());
                    String classname = pathname.substring( 0, pathname.length() - ".java".length() );
                    classname = classname.replace( File.separatorChar, '.' );
                    files.put( file.getAbsolutePath(), classname );// it's a java file, add it.
                }
            }
        }
        return files;
    }

}
