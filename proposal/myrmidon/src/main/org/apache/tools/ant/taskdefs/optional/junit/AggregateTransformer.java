/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
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
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.w3c.dom.Document;

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
    private final static DocumentBuilderFactory c_dbfactory = DocumentBuilderFactory.newInstance();

    /**
     * the xml document to process
     */
    private Document m_document;

    /**
     * the format to use for the report. Must be <tt>FRAMES</tt> or <tt>NOFRAMES
     * </tt>
     */
    private String m_format;

    /**
     * the style directory. XSLs should be read from here if necessary
     */
    private File m_styleDir;

    private AbstractTask m_task;

    /**
     * the destination directory, this is the root from where html should be
     * generated
     */
    private File m_toDir;

    public AggregateTransformer( AbstractTask task )
    {
        m_task = task;
    }

    public void setFormat( Format format )
    {
        m_format = format.getValue();
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
        m_styleDir = styledir;
    }

    /**
     * set the destination directory
     *
     * @param todir The new Todir value
     */
    public void setTodir( File todir )
    {
        m_toDir = todir;
    }

    public void setXmlDocument( Document doc )
    {
        m_document = doc;
    }

    public void transform()
        throws TaskException
    {
        checkOptions();
        try
        {
            XalanExecutor executor = XalanExecutor.newInstance( this );
            executor.execute();
        }
        catch( Exception e )
        {
            throw new TaskException( "Errors while applying transformations", e );
        }
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
            DocumentBuilder builder = c_dbfactory.newDocumentBuilder();
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
        if( NOFRAMES.equals( m_format ) )
        {
            xslname = "junit-noframes.xsl";
        }
        URL url = null;
        if( m_styleDir == null )
        {
            url = getClass().getResource( "xsl/" + xslname );
            if( url == null )
            {
                throw new FileNotFoundException( "Could not find jar resource " + xslname );
            }
        }
        else
        {
            File file = new File( m_styleDir, xslname );
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
        if( m_toDir == null )
        {
            m_toDir = FileUtil.resolveFile( m_task.getBaseDirectory(), "." );
        }
        else if( !m_toDir.isAbsolute() )
        {
            m_toDir = FileUtil.resolveFile( m_task.getBaseDirectory(), m_toDir.getPath() );
        }
    }

    protected Document getDocument()
    {
        return m_document;
    }

    protected String getFormat()
    {
        return m_format;
    }

    protected File getToDir()
    {
        return m_toDir;
    }

    public static class Format extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{FRAMES, NOFRAMES};
        }
    }

}
