/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.pvcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.aut.nativelib.ExecManager;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.exec.Execute2;
import org.apache.tools.ant.types.Commandline;

/**
 * A task that fetches source files from a PVCS archive <b>19-04-2001</b> <p>
 *
 * The task now has a more robust parser. It allows for platform independant
 * file paths and supports file names with <i>()</i> . Thanks to Erik Husby for
 * bringing the bug to my attention. <b>27-04-2001</b> <p>
 *
 * UNC paths are now handled properly. Fix provided by Don Jeffery. He also
 * added an <i>UpdateOnly</i> flag that, when true, conditions the PVCS get
 * using the -U option to only update those files that have a modification time
 * (in PVCS) that is newer than the existing workfile.
 *
 * @author <a href="mailto:tchristensen@nordija.com">Thomas Christensen</a>
 * @author <a href="mailto:donj@apogeenet.com">Don Jeffery</a>
 * @author <a href="snewton@standard.com">Steven E. Newton</a>
 */
public class Pvcs
    extends AbstractTask
    implements ExecOutputHandler
{
    /**
     * Constant for the thing to execute
     */
    private final static String PCLI_EXE = "pcli";

    /**
     * Constant for the PCLI listversionedfiles recursive i a format "get"
     * understands
     */
    private final static String PCLI_LVF_ARGS = "lvf -z -aw";

    /**
     * Constant for the thing to execute
     */
    private final static String GET_EXE = "get";
    private String m_filenameFormat;
    private boolean m_force;
    private boolean m_ignoreReturnCode;
    private String m_label;
    private String m_lineStart;
    private String m_promotiongroup;
    private String m_pvcsProject;
    private ArrayList m_pvcsProjects;
    private String m_pvcsbin;
    private String m_repository;
    private boolean m_updateOnly;
    private String m_workspace;
    private FileOutputStream m_output;

    /**
     * Creates a Pvcs object
     */
    public Pvcs()
    {
        m_pvcsProjects = new ArrayList();
        m_lineStart = "\"P:";
        m_filenameFormat = "{0}_arc({1})";
    }

    public void setFilenameFormat( final String filenameFormat )
    {
        m_filenameFormat = filenameFormat;
    }

    /**
     * Specifies the value of the force argument
     */
    public void setForce( final boolean force )
    {
        m_force = force;
    }

    /**
     * If set to true the return value from executing the pvcs commands are
     * ignored.
     */
    public void setIgnoreReturnCode( final boolean ignoreReturnCode )
    {
        m_ignoreReturnCode = ignoreReturnCode;
    }

    /**
     * Specifies the name of the label argument
     */
    public void setLabel( final String label )
    {
        m_label = label;
    }

    public void setLineStart( final String lineStart )
    {
        m_lineStart = lineStart;
    }

    /**
     * Specifies the name of the promotiongroup argument
     */
    public void setPromotiongroup( final String promotiongroup )
    {
        m_promotiongroup = promotiongroup;
    }

    /**
     * Specifies the location of the PVCS bin directory
     */
    public void setPvcsbin( final String pvcsbin )
    {
        m_pvcsbin = pvcsbin;
    }

    /**
     * Specifies the name of the project in the PVCS repository
     */
    public void setPvcsproject( final String pvcsProject )
    {
        m_pvcsProject = pvcsProject;
    }

    /**
     * Specifies the network name of the PVCS repository
     */
    public void setRepository( final String repository )
    {
        m_repository = repository;
    }

    /**
     * If set to true files are gotten only if newer than existing local files.
     */
    public void setUpdateOnly( final boolean updateOnly )
    {
        m_updateOnly = updateOnly;
    }

    /**
     * Specifies the name of the workspace to store retrieved files
     */
    public void setWorkspace( final String workspace )
    {
        m_workspace = workspace;
    }

    /**
     * handles &lt;pvcsproject&gt; subelements
     */
    public void addPvcsproject( final PvcsProject pvcsProject )
    {
        m_pvcsProjects.add( pvcsProject );
    }

    public void execute()
        throws TaskException
    {
        int result = 0;

        validate();

        final File filelist = getFileList();

        final Commandline cmd = buildGetCommand( filelist );
        getLogger().info( "Getting files" );
        getLogger().debug( "Executing " + cmd.toString() );
        try
        {
            final ExecManager execManager = (ExecManager)getService( ExecManager.class );
            final Execute2 exe = new Execute2( execManager );
            setupLogger( exe );
            exe.setWorkingDirectory( getBaseDirectory() );
            exe.setCommandline( cmd );
            result = exe.execute();
            checkResultCode( result, cmd );
        }
        catch( IOException e )
        {
            String msg = "Failed executing: " + cmd.toString() + ". Exception: " + e.getMessage();
            throw new TaskException( msg );
        }
        finally
        {
            if( filelist != null )
            {
                filelist.delete();
            }
        }
    }

    private Commandline buildGetCommand( final File filelist )
    {
        final Commandline cmd = new Commandline();
        cmd.setExecutable( getExecutable( GET_EXE ) );

        if( m_force )
        {
            cmd.addArgument( "-Y" );
        }
        else
        {
            cmd.addArgument( "-N" );
        }

        if( null != m_promotiongroup )
        {
            cmd.addArgument( "-G" + m_promotiongroup );
        }
        else if( null != m_label )
        {
            cmd.addArgument( "-r" + m_label );
        }

        if( m_updateOnly )
        {
            cmd.addArgument( "-U" );
        }

        cmd.addArgument( "@" + filelist.getAbsolutePath() );
        return cmd;
    }

    private void checkResultCode( final int result, final Commandline cmd )
        throws TaskException
    {
        if( result != 0 && !m_ignoreReturnCode )
        {
            final String message = "Failed executing: " + cmd.toString() +
                ". Return code was " + result;
            throw new TaskException( message );
        }
    }

    private File getFileList()
        throws TaskException
    {
        // Check workspace exists
        // Launch PCLI listversionedfiles -z -aw
        // Capture output
        // build the command line from what we got the format is
        final Commandline cmd = buildPCLICommand();
        getLogger().debug( "Executing " + cmd.toString() );

        File tmp = null;

        try
        {
            tmp = File.createTempFile( "pvcs_ant_", ".log" );
            final File fileList = File.createTempFile( "pvcs_ant_", ".log" );

            final ExecManager execManager = (ExecManager)getService( ExecManager.class );
            final Execute2 exe = new Execute2( execManager );
            setupLogger( exe );
            exe.setExecOutputHandler( this );
            m_output = new FileOutputStream( tmp );
            exe.setWorkingDirectory( getBaseDirectory() );
            exe.setCommandline( cmd );
            final int result = exe.execute();
            checkResultCode( result, cmd );

            if( !tmp.exists() )
            {
                final String message = "Communication between ant and pvcs failed. No output " +
                    "generated from executing PVCS commandline interface \"pcli\" and \"get\"";
                throw new TaskException( message );
            }

            // Create folders in workspace
            getLogger().info( "Creating folders" );
            createFolders( tmp );

            // Massage PCLI lvf output transforming '\' to '/' so get command works appropriately
            massagePCLI( tmp, fileList );
            return fileList;
        }
        catch( final ParseException pe )
        {
            final String message = "Failed executing: " +
                cmd.toString() + ". Exception: " + pe.getMessage();
            throw new TaskException( message );
        }
        catch( final IOException ioe )
        {
            final String message = "Failed executing: " +
                cmd.toString() + ". Exception: " + ioe.getMessage();
            throw new TaskException( message );
        }
        finally
        {
            IOUtil.shutdownStream( m_output );
            if( null != tmp )
            {
                tmp.delete();
            }
        }
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     */
    public void stdout( final String line )
    {
        try
        {
            m_output.write( line.getBytes() );
        }
        catch( final IOException ioe )
        {
            final String message = "Failed to write to output stream";
            getLogger().error( message );
        }
    }

    /**
     * Receive notification about the process writing
     * to standard error.
     */
    public void stderr( final String line )
    {
        getLogger().warn( line );
    }

    private Commandline buildPCLICommand()
        throws TaskException
    {
        final Commandline cmd = new Commandline();
        cmd.setExecutable( getExecutable( PCLI_EXE ) );

        cmd.addArgument( "lvf" );
        cmd.addArgument( "-z" );
        cmd.addArgument( "-aw" );
        if( m_workspace != null )
        {
            cmd.addArgument( "-sp" + m_workspace );
        }
        cmd.addArgument( "-pr" + m_repository );

        if( m_pvcsProject != null )
        {
            cmd.addArgument( m_pvcsProject );
        }

        if( !m_pvcsProjects.isEmpty() )
        {
            Iterator e = m_pvcsProjects.iterator();
            while( e.hasNext() )
            {
                final PvcsProject project = (PvcsProject)e.next();
                final String name = project.getName();
                if( name == null || ( name.trim() ).equals( "" ) )
                {
                    final String message = "name is a required attribute of pvcsproject";
                    throw new TaskException( message );
                }
                cmd.addArgument( name );
            }
        }
        return cmd;
    }

    private void validate()
        throws TaskException
    {
        if( m_repository == null || m_repository.trim().equals( "" ) )
        {
            throw new TaskException( "Required argument repository not specified" );
        }

        // default pvcs project is "/"
        if( m_pvcsProject == null && m_pvcsProjects.isEmpty() )
        {
            m_pvcsProject = "/";
        }
    }

    private String getExecutable( final String exe )
    {
        final StringBuffer correctedExe = new StringBuffer();
        if( null != m_pvcsbin )
        {
            if( m_pvcsbin.endsWith( File.separator ) )
            {
                correctedExe.append( m_pvcsbin );
            }
            else
            {
                correctedExe.append( m_pvcsbin ).append( File.separator );
            }
        }
        return correctedExe.append( exe ).toString();
    }

    /**
     * Parses the file and creates the folders specified in the output section
     */
    private void createFolders( final File file )
        throws IOException, ParseException
    {
        final BufferedReader in = new BufferedReader( new FileReader( file ) );
        final MessageFormat mf = new MessageFormat( m_filenameFormat );
        String line = in.readLine();
        while( line != null )
        {
            getLogger().debug( "Considering \"" + line + "\"" );
            if( line.startsWith( "\"\\" ) ||
                line.startsWith( "\"/" ) ||
                line.startsWith( m_lineStart ) )
            {
                Object[] objs = mf.parse( line );
                String f = (String)objs[ 1 ];
                // Extract the name of the directory from the filename
                int index = f.lastIndexOf( File.separator );
                if( index > -1 )
                {
                    File dir = new File( f.substring( 0, index ) );
                    if( !dir.exists() )
                    {
                        getLogger().debug( "Creating " + dir.getAbsolutePath() );
                        if( dir.mkdirs() )
                        {
                            getLogger().info( "Created " + dir.getAbsolutePath() );
                        }
                        else
                        {
                            getLogger().info( "Failed to create " + dir.getAbsolutePath() );
                        }
                    }
                    else
                    {
                        getLogger().debug( dir.getAbsolutePath() + " exists. Skipping" );
                    }
                }
                else
                {
                    final String message = "File separator problem with " + line;
                    getLogger().warn( message );
                }
            }
            else
            {
                getLogger().debug( "Skipped \"" + line + "\"" );
            }
            line = in.readLine();
        }
    }

    /**
     * Simple hack to handle the PVCS command-line tools botch when handling UNC
     * notation.
     */
    private void massagePCLI( final File in, final File out )
        throws FileNotFoundException, IOException
    {
        final BufferedReader inReader = new BufferedReader( new FileReader( in ) );
        final BufferedWriter outWriter = new BufferedWriter( new FileWriter( out ) );
        String s = null;
        while( ( s = inReader.readLine() ) != null )
        {
            final String sNormal = s.replace( '\\', '/' );
            outWriter.write( sNormal );
            outWriter.newLine();
        }
        inReader.close();
        outWriter.close();
    }
}
