/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Transform a JUnit xml report. The default transformation generates an html
 * report in either framed or non-framed style. The non-framed style is
 * convenient to have a concise report via mail, the framed report is much more
 * convenient if you want to browse into different packages or testcases since
 * it is a Javadoc like report.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class AggregateTransformer
{

    public final static String FRAMES = "frames";

    public final static String NOFRAMES = "noframes";

    /**
     * XML Parser factory
     */
    protected final static DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();

    /**
     * the xml document to process
     */
    protected Document document;

    /**
     * the format to use for the report. Must be <tt>FRAMES</tt> or <tt>NOFRAMES
     * </tt>
     */
    protected String format;

    /**
     * the style directory. XSLs should be read from here if necessary
     */
    protected File styleDir;

    /**
     * Task
     */
    protected Task task;

    /**
     * the destination directory, this is the root from where html should be
     * generated
     */
    protected File toDir;

    public AggregateTransformer( Task task )
    {
        this.task = task;
    }

    /**
     * set the extension of the output files
     *
     * @param ext The new Extension value
     */
    public void setExtension( String ext )
    {
        task.log( "extension is not used anymore", Project.MSG_WARN );
    }

    public void setFormat( Format format )
    {
        this.format = format.getValue();
    }

    /**
     * set the style directory. It is optional and will override the default xsl
     * used.
     *
     * @param styledir the directory containing the xsl files if the user would
     *      like to override with its own style.
     */
    public void setStyledir( File styledir )
    {
        this.styleDir = styledir;
    }

    /**
     * set the destination directory
     *
     * @param todir The new Todir value
     */
    public void setTodir( File todir )
    {
        this.toDir = todir;
    }

    public void setXmlDocument( Document doc )
    {
        this.document = doc;
    }

    public void transform()
        throws TaskException
    {
        checkOptions();
        final long t0 = System.currentTimeMillis();
        try
        {
            Element root = document.getDocumentElement();
            XalanExecutor executor = XalanExecutor.newInstance( this );
            executor.execute();
        }
        catch( Exception e )
        {
            throw new TaskException( "Errors while applying transformations", e );
        }
        final long dt = System.currentTimeMillis() - t0;
        //task.getLogger().info( "Transform time: " + dt + "ms" );
    }

    /**
     * Set the xml file to be processed. This is a helper if you want to set the
     * file directly. Much more for testing purposes.
     *
     * @param xmlfile xml file to be processed
     * @exception TaskException Description of Exception
     */
    protected void setXmlfile( File xmlfile )
        throws TaskException
    {
        try
        {
            DocumentBuilder builder = dbfactory.newDocumentBuilder();
            InputStream in = new FileInputStream( xmlfile );
            try
            {
                Document doc = builder.parse( in );
                setXmlDocument( doc );
            }
            finally
            {
                in.close();
            }
        }
        catch( Exception e )
        {
            throw new TaskException( "Error while parsing document: " + xmlfile, e );
        }
    }

    /**
     * Get the systemid of the appropriate stylesheet based on its name and
     * styledir. If no styledir is defined it will load it as a java resource in
     * the xsl child package, otherwise it will get it from the given directory.
     *
     * @return The StylesheetSystemId value
     * @throws IOException thrown if the requested stylesheet does not exist.
     */
    protected String getStylesheetSystemId()
        throws IOException
    {
        String xslname = "junit-frames.xsl";
        if( NOFRAMES.equals( format ) )
        {
            xslname = "junit-noframes.xsl";
        }
        URL url = null;
        if( styleDir == null )
        {
            url = getClass().getResource( "xsl/" + xslname );
            if( url == null )
            {
                throw new FileNotFoundException( "Could not find jar resource " + xslname );
            }
        }
        else
        {
            File file = new File( styleDir, xslname );
            if( !file.exists() )
            {
                throw new FileNotFoundException( "Could not find file '" + file + "'" );
            }
            url = new URL( "file", "", file.getAbsolutePath() );
        }
        return url.toExternalForm();
    }

    /**
     * check for invalid options
     *
     * @exception TaskException Description of Exception
     */
    protected void checkOptions()
        throws TaskException
    {
        // set the destination directory relative from the project if needed.
        if( toDir == null )
        {
            toDir = FileUtils.newFileUtils().resolveFile( task.getProject().getBaseDir(), "." );
        }
        else if( !toDir.isAbsolute() )
        {
            toDir = FileUtils.newFileUtils().
                resolveFile( task.getProject().getBaseDir(), toDir.getPath() );
        }
    }

    public static class Format extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{FRAMES, NOFRAMES};
        }
    }

}
