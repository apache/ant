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
import java.io.OutputStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.LogOutputStream;
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
public class Pvcs extends org.apache.tools.ant.Task
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
    private String filenameFormat;
    private String force;
    private boolean ignorerc;
    private String label;
    private String lineStart;
    private String promotiongroup;
    private String pvcsProject;
    private ArrayList pvcsProjects;
    private String pvcsbin;
    private String repository;
    private boolean updateOnly;
    private String workspace;

    /**
     * Creates a Pvcs object
     */
    public Pvcs()
    {
        super();
        pvcsProject = null;
        pvcsProjects = new ArrayList();
        workspace = null;
        repository = null;
        pvcsbin = null;
        force = null;
        promotiongroup = null;
        label = null;
        ignorerc = false;
        updateOnly = false;
        lineStart = "\"P:";
        filenameFormat = "{0}_arc({1})";
    }

    public void setFilenameFormat( String f )
    {
        filenameFormat = f;
    }

    /**
     * Specifies the value of the force argument
     *
     * @param f The new Force value
     */
    public void setForce( String f )
    {
        if( f != null && f.equalsIgnoreCase( "yes" ) )
            force = "yes";
        else
            force = "no";
    }

    /**
     * If set to true the return value from executing the pvcs commands are
     * ignored.
     *
     * @param b The new IgnoreReturnCode value
     */
    public void setIgnoreReturnCode( boolean b )
    {
        ignorerc = b;
    }

    /**
     * Specifies the name of the label argument
     *
     * @param l The new Label value
     */
    public void setLabel( String l )
    {
        label = l;
    }

    public void setLineStart( String l )
    {
        lineStart = l;
    }

    /**
     * Specifies the name of the promotiongroup argument
     *
     * @param w The new Promotiongroup value
     */
    public void setPromotiongroup( String w )
    {
        promotiongroup = w;
    }

    /**
     * Specifies the location of the PVCS bin directory
     *
     * @param bin The new Pvcsbin value
     */
    public void setPvcsbin( String bin )
    {
        pvcsbin = bin;
    }

    /**
     * Specifies the name of the project in the PVCS repository
     *
     * @param prj String
     */
    public void setPvcsproject( String prj )
    {
        pvcsProject = prj;
    }

    /**
     * Specifies the network name of the PVCS repository
     *
     * @param repo String
     */
    public void setRepository( String repo )
    {
        repository = repo;
    }

    /**
     * If set to true files are gotten only if newer than existing local files.
     *
     * @param l The new UpdateOnly value
     */
    public void setUpdateOnly( boolean l )
    {
        updateOnly = l;
    }

    /**
     * Specifies the name of the workspace to store retrieved files
     *
     * @param ws String
     */
    public void setWorkspace( String ws )
    {
        workspace = ws;
    }

    public String getFilenameFormat()
    {
        return filenameFormat;
    }

    /**
     * Get value of force
     *
     * @return String
     */
    public String getForce()
    {
        return force;
    }

    /**
     * Get value of ignorereturncode
     *
     * @return String
     */
    public boolean getIgnoreReturnCode()
    {
        return ignorerc;
    }

    /**
     * Get value of label
     *
     * @return String
     */
    public String getLabel()
    {
        return label;
    }

    public String getLineStart()
    {
        return lineStart;
    }

    /**
     * Get value of promotiongroup
     *
     * @return String
     */
    public String getPromotiongroup()
    {
        return promotiongroup;
    }

    /**
     * Get name of the PVCS bin directory
     *
     * @return String
     */
    public String getPvcsbin()
    {
        return pvcsbin;
    }

    /**
     * Get name of the project in the PVCS repository
     *
     * @return String
     */
    public String getPvcsproject()
    {
        return pvcsProject;
    }

    /**
     * Get name of the project in the PVCS repository
     *
     * @return ArrayList
     */
    public ArrayList getPvcsprojects()
    {
        return pvcsProjects;
    }

    /**
     * Get network name of the PVCS repository
     *
     * @return String
     */
    public String getRepository()
    {
        return repository;
    }

    public boolean getUpdateOnly()
    {
        return updateOnly;
    }

    /**
     * Get name of the workspace to store the retrieved files
     *
     * @return String
     */
    public String getWorkspace()
    {
        return workspace;
    }

    /**
     * handles &lt;pvcsproject&gt; subelements
     *
     * @param p The feature to be added to the Pvcsproject attribute
     */
    public void addPvcsproject( PvcsProject p )
    {
        pvcsProjects.add( p );
    }

    /**
     * @exception org.apache.tools.ant.TaskException Something is stopping the
     *      build...
     */
    public void execute()
        throws TaskException
    {
        Project aProj = getProject();
        int result = 0;

        if( repository == null || repository.trim().equals( "" ) )
            throw new TaskException( "Required argument repository not specified" );

        // Check workspace exists
        // Launch PCLI listversionedfiles -z -aw
        // Capture output
        // build the command line from what we got the format is
        Commandline commandLine = new Commandline();
        commandLine.setExecutable( getExecutable( PCLI_EXE ) );

        commandLine.createArgument().setValue( "lvf" );
        commandLine.createArgument().setValue( "-z" );
        commandLine.createArgument().setValue( "-aw" );
        if( getWorkspace() != null )
            commandLine.createArgument().setValue( "-sp" + getWorkspace() );
        commandLine.createArgument().setValue( "-pr" + getRepository() );

        // default pvcs project is "/"
        if( getPvcsproject() == null && getPvcsprojects().isEmpty() )
            pvcsProject = "/";

        if( getPvcsproject() != null )
            commandLine.createArgument().setValue( getPvcsproject() );
        if( !getPvcsprojects().isEmpty() )
        {
            Iterator e = getPvcsprojects().iterator();
            while( e.hasNext() )
            {
                String projectName = ( (PvcsProject)e.next() ).getName();
                if( projectName == null || ( projectName.trim() ).equals( "" ) )
                    throw new TaskException( "name is a required attribute of pvcsproject" );
                commandLine.createArgument().setValue( projectName );
            }
        }

        File tmp = null;
        File tmp2 = null;
        try
        {
            Random rand = new Random( System.currentTimeMillis() );
            tmp = new File( "pvcs_ant_" + rand.nextLong() + ".log" );
            tmp2 = new File( "pvcs_ant_" + rand.nextLong() + ".log" );
            getLogger().debug( "Executing " + commandLine.toString() );
            result = runCmd( commandLine, new FileOutputStream( tmp ),
                             new LogOutputStream( getLogger(), true ) );
            if( result != 0 && !ignorerc )
            {
                String msg = "Failed executing: " + commandLine.toString();
                throw new TaskException( msg );
            }

            if( !tmp.exists() )
                throw new TaskException( "Communication between ant and pvcs failed. No output generated from executing PVCS commandline interface \"pcli\" and \"get\"" );

            // Create folders in workspace
            getLogger().info( "Creating folders" );
            createFolders( tmp );

            // Massage PCLI lvf output transforming '\' to '/' so get command works appropriately
            massagePCLI( tmp, tmp2 );

            // Launch get on output captured from PCLI lvf
            commandLine.clearArgs();
            commandLine.setExecutable( getExecutable( GET_EXE ) );

            if( getForce() != null && getForce().equals( "yes" ) )
                commandLine.createArgument().setValue( "-Y" );
            else
                commandLine.createArgument().setValue( "-N" );

            if( getPromotiongroup() != null )
                commandLine.createArgument().setValue( "-G" + getPromotiongroup() );
            else
            {
                if( getLabel() != null )
                    commandLine.createArgument().setValue( "-r" + getLabel() );
            }

            if( updateOnly )
            {
                commandLine.createArgument().setValue( "-U" );
            }

            commandLine.createArgument().setValue( "@" + tmp2.getAbsolutePath() );
            getLogger().info( "Getting files" );
            getLogger().debug( "Executing " + commandLine.toString() );
            final LogOutputStream output = new LogOutputStream( getLogger(), false );
            final LogOutputStream error = new LogOutputStream( getLogger(), true );
            result = runCmd( commandLine, output, error );

            if( result != 0 && !ignorerc )
            {
                String msg = "Failed executing: " + commandLine.toString() + ". Return code was " + result;
                throw new TaskException( msg );
            }

        }
        catch( FileNotFoundException e )
        {
            String msg = "Failed executing: " + commandLine.toString() + ". Exception: " + e.getMessage();
            throw new TaskException( msg );
        }
        catch( IOException e )
        {
            String msg = "Failed executing: " + commandLine.toString() + ". Exception: " + e.getMessage();
            throw new TaskException( msg );
        }
        catch( ParseException e )
        {
            String msg = "Failed executing: " + commandLine.toString() + ". Exception: " + e.getMessage();
            throw new TaskException( msg );
        }
        finally
        {
            if( tmp != null )
            {
                tmp.delete();
            }
            if( tmp2 != null )
            {
                tmp2.delete();
            }
        }
    }

    protected int runCmd( Commandline cmd, OutputStream output, OutputStream error )
        throws TaskException
    {
        try
        {
            final Execute exe = new Execute();
            exe.setOutput( output );
            exe.setError( error );
            exe.setWorkingDirectory( getBaseDirectory() );
            exe.setCommandline( cmd.getCommandline() );
            return exe.execute();
        }
        catch( java.io.IOException e )
        {
            String msg = "Failed executing: " + cmd.toString() + ". Exception: " + e.getMessage();
            throw new TaskException( msg );
        }
    }

    private String getExecutable( String exe )
    {
        StringBuffer correctedExe = new StringBuffer();
        if( getPvcsbin() != null )
            if( pvcsbin.endsWith( File.separator ) )
                correctedExe.append( pvcsbin );
            else
                correctedExe.append( pvcsbin ).append( File.separator );
        return correctedExe.append( exe ).toString();
    }

    /**
     * Parses the file and creates the folders specified in the output section
     *
     * @param file Description of Parameter
     * @exception IOException Description of Exception
     * @exception ParseException Description of Exception
     */
    private void createFolders( File file )
        throws IOException, ParseException
    {
        BufferedReader in = new BufferedReader( new FileReader( file ) );
        MessageFormat mf = new MessageFormat( getFilenameFormat() );
        String line = in.readLine();
        while( line != null )
        {
            getLogger().debug( "Considering \"" + line + "\"" );
            if( line.startsWith( "\"\\" ) ||
                line.startsWith( "\"/" ) ||
                line.startsWith( getLineStart() ) )
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
     *
     * @param in Description of Parameter
     * @param out Description of Parameter
     * @exception FileNotFoundException Description of Exception
     * @exception IOException Description of Exception
     */
    private void massagePCLI( File in, File out )
        throws FileNotFoundException, IOException
    {
        BufferedReader inReader = new BufferedReader( new FileReader( in ) );
        BufferedWriter outWriter = new BufferedWriter( new FileWriter( out ) );
        String s = null;
        while( ( s = inReader.readLine() ) != null )
        {
            String sNormal = s.replace( '\\', '/' );
            outWriter.write( sNormal );
            outWriter.newLine();
        }
        inReader.close();
        outWriter.close();
    }
}
