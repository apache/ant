/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.sitraka;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.aut.nativelib.ExecManager;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.EnumeratedAttribute;
import org.apache.tools.todo.types.Path;
import org.w3c.dom.Document;

/**
 * Convenient task to run the snapshot merge utility for JProbe Coverage 3.0.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class CovReport
    extends AbstractTask
{
    /*
     * jpcoverport [options] -output=file -snapshot=snapshot.jpc
     * jpcovreport [options] [-paramfile=file] -output=<fileName> -snapshot=<fileName>
     * Generate a report based on the indicated snapshot
     * -paramfile=file
     * A text file containing the report generation options.
     * -format=(html|text|xml) defaults to html
     * The format of the generated report.
     * -type=(executive|summary|detailed|verydetailed) defaults to detailed
     * The type of report to be generated. For -format=xml,
     * use -type=verydetailed to include source code lines.
     * Note: A very detailed report can be VERY large.
     * -percent=num            Min 1 Max 101 Default 101
     * An integer representing a percentage of coverage.
     * Only methods with test case coverage less than the
     * percentage are included in reports.
     * -filters=string
     * A comma-separated list of filters in the form
     * <package>.<class>:V, where V can be I for Include or
     * E for Exclude. For the default package, omit <package>.
     * -filters_method=string
     * Optional. A comma-separated list of methods that
     * correspond one-to-one with the entries in -filters.
     * -output=string  Must be specified
     * The absolute path and file name for the generated
     * report file.
     * -snapshot=string        Must be specified
     * The absolute path and file name of the snapshot file.
     * -inc_src_text=(on|off)  defaults to on
     * Include text of the source code lines.
     * Only applies for -format=xml and -type=verydetailed.
     * -sourcepath=string      defaults to .
     * A semicolon-separated list of source paths.
     * *
     * ** coverage home,  mandatory
     */
    private File home = null;

    /**
     * format of generated report, optional
     */
    private String format = null;

    /**
     * the name of the output snapshot, mandatory
     */
    private File tofile = null;

    /**
     * type of report, optional
     */
    private String type = null;

    /**
     * threshold value for printing methods, optional
     */
    private Integer percent = null;

    /**
     * comma separated list of filters (???)
     */
    private String filters = null;

    /**
     * name of the snapshot file to create report from
     */
    private File snapshot = null;

    /**
     * sourcepath to use
     */
    private Path sourcePath = null;

    /**
     * include the text for each line of code (xml report verydetailed)
     */
    private boolean includeSource = true;

    private Path coveragePath = null;

    /**
     */
    private Reference reference = null;

    public CovReport()
    {
    }

    /**
     * set the filters
     *
     * @param values The new Filters value
     */
    public void setFilters( String values )
    {
        this.filters = values;
    }

    /**
     * set the format of the report html|text|xml
     *
     * @param value The new Format value
     */
    public void setFormat( ReportFormat value )
    {
        this.format = value.getValue();
    }

    /**
     * Set the coverage home. it must point to JProbe coverage directories where
     * are stored native libraries and jars.
     *
     * @param value The new Home value
     */
    public void setHome( File value )
    {
        this.home = value;
    }

    /**
     * include source code lines. XML report only
     *
     * @param value The new Includesource value
     */
    public void setIncludesource( boolean value )
    {
        this.includeSource = value;
    }

    /**
     * sets the threshold printing method 0-100
     *
     * @param value The new Percent value
     */
    public void setPercent( Integer value )
    {
        this.percent = value;
    }

    public void setSnapshot( File value )
    {
        this.snapshot = value;
    }

    /**
     * Set the output snapshot file
     *
     * @param value The new Tofile value
     */
    public void setTofile( File value )
    {
        this.tofile = value;
    }

    /**
     * sets the report type executive|summary|detailed|verydetailed
     *
     * @param value The new Type value
     */
    public void setType( ReportType value )
    {
        this.type = value.getValue();
    }

    //@todo to remove
    public Path createCoveragepath()
    {
        if( coveragePath == null )
        {
            coveragePath = new Path();
        }
        Path path1 = coveragePath;
        final Path path = new Path();
        path1.addPath( path );
        return path;
    }

    public Reference createReference()
    {
        if( reference == null )
        {
            reference = new Reference();
        }
        return reference;
    }

    public Path createSourcepath()
    {
        if( sourcePath == null )
        {
            sourcePath = new Path();
        }
        Path path1 = sourcePath;
        final Path path = new Path();
        path1.addPath( path );
        return path;
    }

    public void execute()
        throws TaskException
    {
        checkOptions();
        try
        {
            Commandline cmdl = new Commandline();
            // we need to run Coverage from his directory due to dll/jar issues
            cmdl.setExecutable( new File( home, "jpcovreport" ).getAbsolutePath() );
            String[] params = getParameters();
            for( int i = 0; i < params.length; i++ )
            {
                cmdl.addArgument( params[ i ] );
            }

            // use the custom handler for stdin issues
            final ExecManager execManager = (ExecManager)getService( ExecManager.class );
            final Execute exe = new Execute( execManager );
            getContext().debug( cmdl.toString() );
            exe.setCommandline( cmdl );
            int exitValue = exe.execute();
            if( exitValue != 0 )
            {
                throw new TaskException( "JProbe Coverage Report failed (" + exitValue + ")" );
            }
            getContext().debug( "coveragePath: " + coveragePath );
            getContext().debug( "format: " + format );
            if( reference != null && "xml".equals( format ) )
            {
                reference.createEnhancedXMLReport();
            }

        }
        catch( IOException e )
        {
            throw new TaskException( "Failed to execute JProbe Coverage Report.", e );
        }
    }

    protected String[] getParameters()
        throws TaskException
    {
        ArrayList v = new ArrayList();
        if( format != null )
        {
            v.add( "-format=" + format );
        }
        if( type != null )
        {
            v.add( "-type=" + type );
        }
        if( percent != null )
        {
            v.add( "-percent=" + percent );
        }
        if( filters != null )
        {
            v.add( "-filters=" + filters );
        }
        v.add( "-output=" + getContext().resolveFile( tofile.getPath() ) );
        v.add( "-snapshot=" + getContext().resolveFile( snapshot.getPath() ) );
        // as a default -sourcepath use . in JProbe, so use project .
        if( sourcePath == null )
        {
            sourcePath = new Path();
            Path path1 = sourcePath;
            final Path path = new Path();
            path1.addPath( path );
            path.setLocation( getBaseDirectory() );
        }
        v.add( "-sourcepath=" + sourcePath );

        if( "verydetailed".equalsIgnoreCase( format ) && "xml".equalsIgnoreCase( type ) )
        {
            v.add( "-inc_src_text=" + ( includeSource ? "on" : "off" ) );
        }

        return (String[])v.toArray( new String[ v.size() ] );
    }

    /**
     * check for mandatory options
     *
     * @exception TaskException Description of Exception
     */
    protected void checkOptions()
        throws TaskException
    {
        if( tofile == null )
        {
            throw new TaskException( "'tofile' attribute must be set." );
        }
        if( snapshot == null )
        {
            throw new TaskException( "'snapshot' attribute must be set." );
        }
        if( home == null )
        {
            throw new TaskException( "'home' attribute must be set to JProbe home directory" );
        }
        home = new File( home, "coverage" );
        File jar = new File( home, "coverage.jar" );
        if( !jar.exists() )
        {
            throw new TaskException( "Cannot find Coverage directory: " + home );
        }
        if( reference != null && !"xml".equals( format ) )
        {
            getContext().info( "Ignored reference. It cannot be used in non XML report." );
            reference = null;// nullify it so that there is no ambiguity
        }

    }

    public static class ReportFormat extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"html", "text", "xml"};
        }
    }

    public static class ReportType extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"executive", "summary", "detailed", "verydetailed"};
        }
    }

    public class Reference
    {
        protected Path classPath;
        protected ReportFilters filters;

        public Path createClasspath()
        {
            if( classPath == null )
            {
                classPath = new Path();
            }
            Path path1 = classPath;
            final Path path = new Path();
            path1.addPath( path );
            return path;
        }

        public ReportFilters createFilters()
        {
            if( filters == null )
            {
                filters = new ReportFilters();
            }
            return filters;
        }

        protected void createEnhancedXMLReport()
            throws TaskException
        {
            // we need a classpath element
            if( classPath == null )
            {
                throw new TaskException( "Need a 'classpath' element." );
            }
            // and a valid one...
            String[] paths = classPath.list();
            if( paths.length == 0 )
            {
                throw new TaskException( "Coverage path is invalid. It does not contain any existing path." );
            }
            // and we need at least one filter include/exclude.
            if( filters == null || filters.size() == 0 )
            {
                createFilters();
                getContext().debug( "Adding default include filter to *.*()" );
                Include include = new Include();
                filters.addInclude( include );
            }
            try
            {
                getContext().debug( "Creating enhanced XML report" );
                XMLReport report = new XMLReport( CovReport.this, tofile );
                report.setReportFilters( filters );
                report.setJProbehome( new File( home.getParent() ) );
                Document doc = report.createDocument( paths );
                TransformerFactory tfactory = TransformerFactory.newInstance();
                Transformer transformer = tfactory.newTransformer();
                transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
                transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
                Source src = new DOMSource( doc );
                Result res = new StreamResult( "file:///" + tofile.toString() );
                transformer.transform( src, res );
            }
            catch( Exception e )
            {
                throw new TaskException( "Error while performing enhanced XML report from file " + tofile, e );
            }
        }
    }
}
