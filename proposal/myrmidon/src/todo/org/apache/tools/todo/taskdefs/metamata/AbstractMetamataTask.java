/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.metamata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import org.apache.aut.nativelib.ExecManager;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.todo.types.Argument;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.CommandlineJava;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.FileSet;
import org.apache.tools.todo.types.Path;
import org.apache.tools.todo.types.ScannerUtil;

/**
 * Somewhat abstract framework to be used for other metama 2.0 tasks. This
 * should include, audit, metrics, cover and mparse. For more information, visit
 * the website at <a href="http://www.metamata.com">www.metamata.com</a>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public abstract class AbstractMetamataTask
    extends AbstractTask
{
    /**
     * The user classpath to be provided. It matches the -classpath of the
     * command line. The classpath must includes both the <tt>.class</tt> and
     * the <tt>.java</tt> files for accurate audit.
     */
    private Path m_classPath;

    /**
     * the path to the source file
     */
    private Path m_sourcePath;

    /**
     * Metamata home directory. It will be passed as a <tt>metamata.home</tt>
     * property and should normally matches the environment property <tt>
     * META_HOME</tt> set by the Metamata installer.
     */
    private File m_metamataHome;

    /**
     * the command line used to run MAudit
     */
    private CommandlineJava m_cmdl = new CommandlineJava();

    /**
     * the set of files to be audited
     */
    private ArrayList m_fileSets = new ArrayList();

    /**
     * the options file where are stored the command line options
     */
    private File m_optionsFile;

    // this is used to keep track of which files were included. It will
    // be set when calling scanFileSets();
    private Hashtable m_includedFiles;

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
        m_cmdl.setVm( "java" );
        m_cmdl.setClassname( className );
    }

    /**
     * convenient method for JDK 1.1. Will copy all elements from src to dest
     *
     * @param dest The feature to be added to the AllArrayList attribute
     * @param files The feature to be added to the AllArrayList attribute
     */
    protected static final void addAllArrayList( ArrayList dest, Iterator files )
    {
        while( files.hasNext() )
        {
            dest.add( files.next() );
        }
    }

    protected static final File createTmpFile()
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
        m_cmdl.addVmArgument( "-Xmx" + max );
    }

    /**
     * the metamata.home property to run all tasks.
     */
    public void setMetamatahome( final File metamataHome )
    {
        this.m_metamataHome = metamataHome;
    }

    /**
     * The java files or directory to be audited
     */
    public void addFileSet( final FileSet fileSet )
    {
        m_fileSets.add( fileSet );
    }

    /**
     * user classpath
     */
    public Path createClasspath()
    {
        if( m_classPath == null )
        {
            m_classPath = new Path();
        }
        return m_classPath;
    }

    /**
     * Creates a nested jvmarg element.
     */
    public void addJvmarg( final Argument argument )
    {
        m_cmdl.addVmArgument( argument );
    }

    /**
     * create the source path for this task
     */
    public Path createSourcepath()
    {
        if( m_sourcePath == null )
        {
            m_sourcePath = new Path();
        }
        return m_sourcePath;
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
            execute0();
        }
        finally
        {
            cleanUp();
        }
    }

    /**
     * check the options and build the command line
     */
    protected void setUp()
        throws TaskException
    {
        validate();

        // set the classpath as the jar file
        File jar = getMetamataJar( m_metamataHome );
        final Path classPath = m_cmdl.createClasspath();
        classPath.addLocation( jar );

        // set the metamata.home property
        m_cmdl.addVmArgument( "-Dmetamata.home=" + m_metamataHome.getAbsolutePath() );

        // retrieve all the files we want to scan
        m_includedFiles = scanFileSets();
        getContext().debug( m_includedFiles.size() + " files added for audit" );

        // write all the options to a temp file and use it ro run the process
        ArrayList options = getOptions();
        m_optionsFile = createTmpFile();
        generateOptionsFile( m_optionsFile, options );
        m_cmdl.addArgument( "-arguments " );
        m_cmdl.addArgument( m_optionsFile.getAbsolutePath() );
    }

    /**
     * return the location of the jar file used to run
     */
    protected final File getMetamataJar( File home )
    {
        return new File( new File( home.getAbsolutePath() ), "lib/metamata.jar" );
    }

    protected Hashtable getFileMapping()
    {
        return m_includedFiles;
    }

    /**
     * return all options of the command line as string elements
     */
    protected abstract ArrayList getOptions()
        throws TaskException;

    /**
     * validate options set
     *
     * @exception TaskException Description of Exception
     */
    protected void validate()
        throws TaskException
    {
        // do some validation first
        if( m_metamataHome == null || !m_metamataHome.exists() )
        {
            throw new TaskException( "'metamatahome' must point to Metamata home directory." );
        }
        m_metamataHome = getContext().resolveFile( m_metamataHome.getPath() );
        File jar = getMetamataJar( m_metamataHome );
        if( !jar.exists() )
        {
            throw new TaskException( jar + " does not exist. Check your metamata installation." );
        }
    }

    /**
     * clean up all the mess that we did with temporary objects
     */
    protected void cleanUp()
        throws TaskException
    {
        if( m_optionsFile != null )
        {
            m_optionsFile.delete();
            m_optionsFile = null;
        }
    }

    /**
     * execute the process with a specific handler
     */
    protected void execute0()
        throws TaskException
    {
        final Execute exe = new Execute();
        getContext().debug( m_cmdl.toString() );
        final String[] commandline = m_cmdl.getCommandline();
        exe.setCommandline( new Commandline( commandline ) );
        exe.execute( getContext() );
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
        throws TaskException
    {
        Hashtable files = new Hashtable();
        for( int i = 0; i < m_fileSets.size(); i++ )
        {
            FileSet fs = (FileSet)m_fileSets.get( i );
            DirectoryScanner ds = ScannerUtil.getDirectoryScanner( fs );
            ds.scan();
            String[] f = ds.getIncludedFiles();
            getContext().debug( i + ") Adding " + f.length + " files from directory " + ds.getBasedir() );
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

    protected ArrayList getFileSets()
    {
        return m_fileSets;
    }

    protected Hashtable getIncludedFiles()
    {
        return m_includedFiles;
    }

    protected Path getClassPath()
    {
        return m_classPath;
    }

    protected void setClassPath( Path classPath )
    {
        m_classPath = classPath;
    }

    protected Path getSourcePath()
    {
        return m_sourcePath;
    }

    protected void setSourcePath( Path sourcePath )
    {
        m_sourcePath = sourcePath;
    }

}
