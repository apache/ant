/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Path;

/**
 * Calculates global complexity and quality metrics on Java source code. You
 * will not be able to use this task with the evaluation version since as of
 * Metamata 2.0, Metrics does not support command line :-( For more information,
 * visit the website at <a href="http://www.metamata.com">www.metamata.com</a>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class MMetrics extends AbstractMetamataTask
{
    /*
     * The command line options as of Metamata 2.0 are as follows:
     * Usage
     * mmetrics <option>... <path>...
     * Parameters
     * path              File or directory to measure.
     * Options
     * -arguments   -A   <file>      Includes command line arguments from file.
     * -classpath   -cp  <path>      Sets class path (also source path unless one
     * explicitly set). Overrides METAPATH/CLASSPATH.
     * -compilation-units            Measure compilation units.
     * -files                        Measure compilation units.
     * -format      -f   <format>    Sets output format, default output file type.
     * -help        -h               Prints help and exits.
     * -indent      -i   <string>    Sets string used to indent labels one level.
     * -methods                      Measure methods, types, and compilation units.
     * -output      -o   <file>      Sets output file name.
     * -quiet       -q               Suppresses copyright message.
     * -sourcepath       <path>      Sets source path. Overrides SOURCEPATH.
     * -types                        Measure types and compilation units.
     * -verbose     -v               Prints all messages.
     * -version     -V               Prints version and exits.
     * Format Options
     * comma csv                     Format output as comma-separated text.
     * html htm                      Format output as an HTML table.
     * tab tab-separated tsv         Format output as tab-separated text.
     * text txt                      Format output as space-aligned text.
     */
    /**
     * the granularity mode. Should be one of 'files', 'methods' and 'types'.
     */
    protected String granularity = null;

    /**
     * the XML output file
     */
    protected File outFile = null;

    /**
     * the location of the temporary txt report
     */
    protected File tmpFile = createTmpFile();

    protected Path path = null;

    //--------------------------- PUBLIC METHODS -------------------------------

    /**
     * default constructor
     */
    public MMetrics()
    {
        super( "com.metamata.sc.MMetrics" );
    }

    /**
     * set the granularity of the audit. Should be one of 'files', 'methods' or
     * 'types'.
     *
     * @param granularity the audit reporting mode.
     */
    public void setGranularity( String granularity )
    {
        this.granularity = granularity;
    }

    /**
     * Set the output XML file
     *
     * @param file the xml file to write the XML report to.
     */
    public void setTofile( File file )
    {
        this.outFile = file;
    }

    /**
     * Set a new path (directory) to measure metrics from.
     *
     * @return the path instance to use.
     */
    public Path createPath()
    {
        if( path == null )
        {
            path = new Path( project );
        }
        return path;
    }

    protected Vector getOptions()
    {
        Vector options = new Vector( 512 );
        // there is a bug in Metamata 2.0 build 37. The sourcepath argument does
        // not work. So we will use the sourcepath prepended to classpath. (order
        // is important since Metamata looks at .class and .java)
        if( sourcePath != null )
        {
            sourcePath.append( classPath );// srcpath is prepended
            classPath = sourcePath;
            sourcePath = null;// prevent from using -sourcepath
        }

        // don't forget to modify the pattern if you change the options reporting
        if( classPath != null )
        {
            options.addElement( "-classpath" );
            options.addElement( classPath );
        }
        options.addElement( "-output" );
        options.addElement( tmpFile.toString() );

        options.addElement( "-" + granularity );

        // display the metamata copyright
        // options.addElement( "-quiet");
        options.addElement( "-format" );

        // need this because that's what the handler is using, it's
        // way easier to process than any other separator
        options.addElement( "tab" );

        // specify a / as the indent character, used by the handler.
        options.addElement( "-i" );
        options.addElement( "/" );

        // directories
        String[] dirs = path.list();
        for( int i = 0; i < dirs.length; i++ )
        {
            options.addElement( dirs[ i ] );
        }
        // files next.
        addAllVector( options, includedFiles.keys() );
        return options;
    }

    //------------------- PROTECTED / PRIVATE METHODS --------------------------


    // check for existing options and outfile, all other are optional
    protected void checkOptions()
        throws TaskException
    {
        super.checkOptions();

        if( !"files".equals( granularity ) && !"methods".equals( granularity )
            && !"types".equals( granularity ) )
        {
            throw new TaskException( "Metrics reporting granularity is invalid. Must be one of 'files', 'methods', 'types'" );
        }
        if( outFile == null )
        {
            throw new TaskException( "Output XML file must be set via 'tofile' attribute." );
        }
        if( path == null && fileSets.size() == 0 )
        {
            throw new TaskException( "Must set either paths (path element) or files (fileset element)" );
        }
        // I don't accept dirs and files at the same time, I cannot recognize the semantic in the result
        if( path != null && fileSets.size() > 0 )
        {
            throw new TaskException( "Cannot set paths (path element) and files (fileset element) at the same time" );
        }
    }

    /**
     * cleanup the temporary txt report
     *
     * @exception TaskException Description of Exception
     */
    protected void cleanUp()
        throws TaskException
    {
        try
        {
            super.cleanUp();
        }
        finally
        {
            if( tmpFile != null )
            {
                tmpFile.delete();
                tmpFile = null;
            }
        }
    }

    /**
     * if the report is transform via a temporary txt file we should use a a
     * normal logger here, otherwise we could use the metrics handler directly
     * to capture and transform the output on stdout to XML.
     *
     * @return Description of the Returned Value
     */
    protected ExecuteStreamHandler createStreamHandler()
    {
        // write the report directtly to an XML stream
        // return new MMetricsStreamHandler(this, xmlStream);
        return new LogStreamHandler( this, Project.MSG_INFO, Project.MSG_INFO );
    }

    protected void execute0( ExecuteStreamHandler handler )
        throws TaskException
    {
        super.execute0( handler );
        transformFile();
    }

    /**
     * transform the generated file via the handler This function can either be
     * called if the result is written to the output file via -output or we
     * could use the handler directly on stdout if not.
     *
     * @exception TaskException Description of Exception
     * @see #createStreamHandler()
     */
    protected void transformFile()
        throws TaskException
    {
        FileInputStream tmpStream = null;
        try
        {
            tmpStream = new FileInputStream( tmpFile );
        }
        catch( IOException e )
        {
            throw new TaskException( "Error reading temporary file: " + tmpFile, e );
        }
        FileOutputStream xmlStream = null;
        try
        {
            xmlStream = new FileOutputStream( outFile );
            ExecuteStreamHandler xmlHandler = new MMetricsStreamHandler( this, xmlStream );
            xmlHandler.setProcessOutputStream( tmpStream );
            xmlHandler.start();
            xmlHandler.stop();
        }
        catch( IOException e )
        {
            throw new TaskException( "Error creating output file: " + outFile, e );
        }
        finally
        {
            if( xmlStream != null )
            {
                try
                {
                    xmlStream.close();
                }
                catch( IOException ignored )
                {
                }
            }
            if( tmpStream != null )
            {
                try
                {
                    tmpStream.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }
    }

}
