/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.AbstractMatchingTask;
import org.apache.myrmidon.framework.FileSet;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.ScannerUtil;

/**
 * A Task to process via XSLT a set of XML documents. This is useful for
 * building views of XML based documentation. arguments:
 * <ul>
 *   <li> basedir
 *   <li> destdir
 *   <li> style
 *   <li> includes
 *   <li> excludes
 * </ul>
 * Of these arguments, the <b>sourcedir</b> and <b>destdir</b> are required. <p>
 *
 * This task will recursively scan the sourcedir and destdir looking for XML
 * documents to process via XSLT. Any other files, such as images, or html files
 * in the source directory will be copied into the destination directory.
 *
 * @author <a href="mailto:kvisco@exoffice.com">Keith Visco</a>
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 * @author <a href="stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class XSLTProcess
    extends AbstractMatchingTask
{
    private File m_destdir;
    private File m_basedir;
    private String m_targetExtension = ".html";
    private ArrayList m_params = new ArrayList();
    private File m_in;
    private File m_out;
    private Path m_classpath;
    private boolean m_force;
    private File m_stylesheet;

    private boolean m_processorPrepared;
    private TransformerFactory m_transformerFactory;
    private Transformer m_transformer;

    /**
     * Set the base directory.
     */
    public void setBasedir( final File basedir )
    {
        m_basedir = basedir;
    }

    /**
     * Set the classpath to load the Processor through (attribute).
     */
    public void setClasspath( final Path classpath )
        throws TaskException
    {
        addClasspath( classpath );
    }

    /**
     * Set the destination directory into which the XSL result files should be
     * copied to
     */
    public void setDestdir( final File destdir )
    {
        m_destdir = destdir;
    }

    /**
     * Set the desired file extension to be used for the target
     */
    public void setExtension( final String targetExtension )
    {
        m_targetExtension = targetExtension;
    }

    /**
     * Set whether to check dependencies, or always generate.
     */
    public void setForce( final boolean force )
    {
        m_force = force;
    }

    /**
     * Sets an input xml file to be styled
     */
    public void setIn( final File in )
    {
        m_in = in;
    }

    /**
     * Sets an out file
     */
    public void setOut( final File out )
    {
        m_out = out;
    }

    /**
     * Sets the file to use for styling relative to the base directory of this
     * task.
     */
    public void setStyle( final File stylesheet )
    {
        m_stylesheet = stylesheet;
    }

    /**
     * Set the classpath to load the Processor through (nested element).
     */
    public void addClasspath( final Path path )
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = new Path();
        }
        m_classpath.addPath( path );
    }

    public void addParam( final XSLTParam param )
    {
        m_params.add( param );
    }

    public void execute()
        throws TaskException
    {
        validate();

        final FileSet fileSet = getFileSet();
        fileSet.setDir( m_basedir );
        final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );

        prepareProcessor();

        // if we have an in file and out then process them
        if( m_in != null && m_out != null )
        {
            processSingleFile( m_in, m_out );
            return;
        }

        final String message = "Transforming into " + m_destdir;
        getLogger().info( message );

        // Process all the files marked for styling
        processFiles( scanner );

        // Process all the directoried marked for styling
        processDirs( scanner );
    }

    private void validate()
        throws TaskException
    {
        if( null == m_stylesheet )
        {
            final String message = "no stylesheet specified";
            throw new TaskException( message );
        }

        if( null == m_basedir )
        {
            m_basedir = getBaseDirectory();
        }

        //-- make sure Source directory exists...
        if( null == m_destdir )
        {
            final String message = "destdir attributes must be set!";
            throw new TaskException( message );
        }
    }

    private void processDirs( final DirectoryScanner scanner )
        throws TaskException
    {
        final String[] dirs = scanner.getIncludedDirectories();
        for( int i = 0; i < dirs.length; i++ )
        {
            final String[] list = new File( m_basedir, dirs[ i ] ).list();
            for( int j = 0; j < list.length; j++ )
            {
                process( m_basedir, list[ j ], m_destdir );
            }
        }
    }

    private void processFiles( final DirectoryScanner scanner )
        throws TaskException
    {
        final String[] list = scanner.getIncludedFiles();
        for( int i = 0; i < list.length; ++i )
        {
            process( m_basedir, list[ i ], m_destdir );
        }
    }

    /**
     * Create transformer factory, loads the stylesheet and set xsl:param parameters.
     */
    protected void prepareProcessor()
        throws TaskException
    {
        if( m_processorPrepared )
        {
            return;
        }
        m_processorPrepared = true;

        //Note the next line should use the specified Classpath
        //and load the class dynaically
        m_transformerFactory = TransformerFactory.newInstance();
        m_transformerFactory.setErrorListener( new TraxErrorListener( true ) );
        //m_transformer.setOutputProperty( OutputKeys.METHOD, m_type );

        try
        {
            getLogger().info( "Loading stylesheet " + m_stylesheet );
            specifyStylesheet();
            specifyParams();
        }
        catch( final Exception e )
        {
            final String message = "Failed to read stylesheet " + m_stylesheet;
            getLogger().info( message );
            throw new TaskException( e.getMessage(), e );
        }
    }

    private void specifyStylesheet()
        throws Exception
    {
        final FileInputStream xslStream = new FileInputStream( m_stylesheet );
        try
        {
            final StreamSource source = new StreamSource( xslStream );
            source.setSystemId( getSystemId( m_stylesheet ) );
            final Templates template = m_transformerFactory.newTemplates( source );
            m_transformer = template.newTransformer();
            m_transformer.setErrorListener( new TraxErrorListener( true ) );
        }
        finally
        {
            IOUtil.shutdownStream( xslStream );
        }
    }

    private void specifyParams() throws TaskException
    {
        final Iterator params = m_params.iterator();
        while( params.hasNext() )
        {
            final XSLTParam param = (XSLTParam)params.next();

            final String expression = param.getExpression();
            if( expression == null )
            {
                throw new TaskException( "Expression attribute is missing." );
            }

            final String name = param.getName();
            if( name == null )
            {
                throw new TaskException( "Name attribute is missing." );
            }

            m_transformer.setParameter( name, expression );
        }
    }

    /**
     * Processes the given input XML file and stores the result in the given
     * resultFile.
     */
    private void process( final File baseDir, final String xmlFile, final File destDir )
        throws TaskException
    {
        final String filename = FileUtil.removeExtension( xmlFile );

        final File in = new File( baseDir, xmlFile );
        final File out = new File( destDir, filename + m_targetExtension );

        processFile( in, out );
    }

    private void processFile( final File in, final File out )
        throws TaskException
    {
        final long styleSheetLastModified = m_stylesheet.lastModified();
        try
        {
            if( m_force ||
                in.lastModified() > out.lastModified() ||
                styleSheetLastModified > out.lastModified() )
            {
                ensureDirectoryFor( out );

                final String notice = "Processing " + in + " to " + out;
                getLogger().info( notice );
                transform( in, out );
            }
        }
        catch( final Exception e )
        {
            // If failed to process document, must delete target document,
            // or it will not attempt to process it the second time
            final String message = "Failed to process " + in;
            getLogger().info( message );
            if( out != null )
            {
                out.delete();
            }

            throw new TaskException( e.getMessage(), e );
        }
    }

    private void processSingleFile( final File in, final File out )
        throws TaskException
    {
        final long styleSheetLastModified = m_stylesheet.lastModified();
        getLogger().debug( "In file " + in + " time: " + in.lastModified() );
        getLogger().debug( "Out file " + out + " time: " + out.lastModified() );
        getLogger().debug( "Style file " + m_stylesheet + " time: " + styleSheetLastModified );

        processFile( in, out );
    }

    private void transform( final File in, final File out )
        throws Exception
    {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try
        {
            fis = new FileInputStream( in );
            fos = new FileOutputStream( out );
            final StreamSource source = new StreamSource( fis, getSystemId( in ) );
            final StreamResult result = new StreamResult( fos );

            m_transformer.transform( source, result );
        }
        finally
        {
            IOUtil.shutdownStream( fis );
            IOUtil.shutdownStream( fos );
        }
    }

    private String getSystemId( final File file )
        throws IOException
    {
        return file.getCanonicalFile().toURL().toExternalForm();
    }

    private void ensureDirectoryFor( final File targetFile )
        throws TaskException
    {
        //In future replace me with
        //FileUtil.forceMkdir( targetFile.getParent() );
        File directory = new File( targetFile.getParent() );
        if( !directory.exists() )
        {
            if( !directory.mkdirs() )
            {
                throw new TaskException( "Unable to create directory: " +
                                         directory.getAbsolutePath() );
            }
        }
    }
}
