/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.xml;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PathUtil;

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
    extends MatchingTask
{
    private File m_destDir;
    private File m_baseDir;
    private String m_xslFile;
    private String m_targetExtension = ".html";
    private ArrayList m_params = new ArrayList();
    private File m_inFile;
    private File m_outFile;
    private Path m_classpath;
    private boolean m_stylesheetLoaded;
    private boolean m_force;
    private String m_outputtype;
    private XSLTLiaison m_liaison;
    private String m_processor;

    /**
     * Set the base directory.
     *
     * @param dir The new Basedir value
     */
    public void setBasedir( File dir )
    {
        m_baseDir = dir;
    }

    /**
     * Set the classpath to load the Processor through (attribute).
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
        throws TaskException
    {
        createClasspath().append( classpath );
    }

    /**
     * Set the destination directory into which the XSL result files should be
     * copied to
     *
     * @param dir The new Destdir value
     */
    public void setDestdir( File dir )
    {
        m_destDir = dir;
    }

    /**
     * Set the desired file extension to be used for the target
     *
     * @param name the extension to use
     */
    public void setExtension( String name )
    {
        m_targetExtension = name;
    }

    /**
     * Set whether to check dependencies, or always generate.
     *
     * @param force The new Force value
     */
    public void setForce( boolean force )
    {
        this.m_force = force;
    }

    /**
     * Sets an input xml file to be styled
     *
     * @param inFile The new In value
     */
    public void setIn( File inFile )
    {
        this.m_inFile = inFile;
    }

    /**
     * Sets an out file
     *
     * @param outFile The new Out value
     */
    public void setOut( File outFile )
    {
        this.m_outFile = outFile;
    }

    /**
     * Set the output type to use for the transformation. Only "xml" (the
     * default) is guaranteed to work for all parsers. Xalan2 also supports
     * "html" and "text".
     *
     * @param type the output method to use
     */
    public void setOutputtype( String type )
    {
        this.m_outputtype = type;
    }

    public void setProcessor( String processor )
    {
        this.m_processor = processor;
    }//-- setDestDir

    /**
     * Sets the file to use for styling relative to the base directory of this
     * task.
     *
     * @param xslFile The new Style value
     */
    public void setStyle( String xslFile )
    {
        this.m_xslFile = xslFile;
    }

    /**
     * Set the classpath to load the Processor through (nested element).
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = new Path();
        }
        Path path1 = m_classpath;
        final Path path = new Path();
        path1.addPath( path );
        return path;
    }

    public XSLTParam createParam()
    {
        XSLTParam p = new XSLTParam();
        m_params.add( p );
        return p;
    }//-- XSLTProcess

    /**
     * Executes the task.
     *
     * @exception TaskException Description of Exception
     */

    public void execute()
        throws TaskException
    {
        DirectoryScanner scanner;
        String[] list;
        String[] dirs;

        if( m_xslFile == null )
        {
            throw new TaskException( "no stylesheet specified" );
        }

        if( m_baseDir == null )
        {
            m_baseDir = getBaseDirectory();
        }

        m_liaison = getLiaison();

        // check if liaison wants to log errors using us as logger
        setupLogger( m_liaison );

        getLogger().debug( "Using " + m_liaison.getClass().toString() );
        File stylesheet = resolveFile( m_xslFile );

        // if we have an in file and out then process them
        if( m_inFile != null && m_outFile != null )
        {
            process( m_inFile, m_outFile, stylesheet );
            return;
        }

        /*
         * if we get here, in and out have not been specified, we are
         * in batch processing mode.
         */
        //-- make sure Source directory exists...
        if( m_destDir == null )
        {
            String msg = "destdir attributes must be set!";
            throw new TaskException( msg );
        }
        scanner = getDirectoryScanner( m_baseDir );
        getLogger().info( "Transforming into " + m_destDir );

        // Process all the files marked for styling
        list = scanner.getIncludedFiles();
        for( int i = 0; i < list.length; ++i )
        {
            process( m_baseDir, list[ i ], m_destDir, stylesheet );
        }

        // Process all the directoried marked for styling
        dirs = scanner.getIncludedDirectories();
        for( int j = 0; j < dirs.length; ++j )
        {
            list = new File( m_baseDir, dirs[ j ] ).list();
            for( int i = 0; i < list.length; ++i )
            {
                process( m_baseDir, list[ i ], m_destDir, stylesheet );
            }
        }
    }

    protected XSLTLiaison getLiaison()
        throws TaskException
    {
        // if processor wasn't specified, see if TraX is available.  If not,
        // default it to xslp or xalan, depending on which is in the classpath
        if( m_liaison == null )
        {
            if( m_processor != null )
            {
                try
                {
                    resolveProcessor( m_processor );
                }
                catch( Exception e )
                {
                    throw new TaskException( "Error", e );
                }
            }
            else
            {
                try
                {
                    resolveProcessor( "trax" );
                }
                catch( Throwable e1 )
                {
                    try
                    {
                        resolveProcessor( "xalan" );
                    }
                    catch( Throwable e2 )
                    {
                        try
                        {
                            resolveProcessor( "adaptx" );
                        }
                        catch( Throwable e3 )
                        {
                            try
                            {
                                resolveProcessor( "xslp" );
                            }
                            catch( Throwable e4 )
                            {
                                e4.printStackTrace();
                                e3.printStackTrace();
                                e2.printStackTrace();
                                throw new TaskException( "Error", e1 );
                            }
                        }
                    }
                }
            }
        }
        return m_liaison;
    }

    /**
     * Loads the stylesheet and set xsl:param parameters.
     *
     * @param stylesheet Description of Parameter
     * @exception TaskException Description of Exception
     */
    protected void configureLiaison( File stylesheet )
        throws TaskException
    {
        if( m_stylesheetLoaded )
        {
            return;
        }
        m_stylesheetLoaded = true;

        try
        {
            getLogger().info( "Loading stylesheet " + stylesheet );
            m_liaison.setStylesheet( stylesheet );
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

                m_liaison.addParam( name, expression );
            }
        }
        catch( final Exception e )
        {
            getLogger().info( "Failed to read stylesheet " + stylesheet );
            throw new TaskException( e.getMessage(), e );
        }
    }

    private void ensureDirectoryFor( File targetFile )
        throws TaskException
    {
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

    /**
     * Load named class either via the system classloader or a given custom
     * classloader.
     *
     * @param classname Description of Parameter
     * @return Description of the Returned Value
     * @exception Exception Description of Exception
     */
    private Class loadClass( String classname )
        throws Exception
    {
        if( m_classpath == null )
        {
            return Class.forName( classname );
        }
        else
        {
            final URL[] urls = PathUtil.toURLs( m_classpath );
            final ClassLoader classLoader = new URLClassLoader( urls );
            return classLoader.loadClass( classname );
        }
    }

    /**
     * Processes the given input XML file and stores the result in the given
     * resultFile.
     *
     * @param baseDir Description of Parameter
     * @param xmlFile Description of Parameter
     * @param destDir Description of Parameter
     * @param stylesheet Description of Parameter
     * @exception TaskException Description of Exception
     */
    private void process( File baseDir, String xmlFile, File destDir,
                          File stylesheet )
        throws TaskException
    {

        String fileExt = m_targetExtension;
        File outFile = null;
        File inFile = null;

        try
        {
            long styleSheetLastModified = stylesheet.lastModified();
            inFile = new File( baseDir, xmlFile );
            int dotPos = xmlFile.lastIndexOf( '.' );
            if( dotPos > 0 )
            {
                outFile = new File( destDir, xmlFile.substring( 0, xmlFile.lastIndexOf( '.' ) ) + fileExt );
            }
            else
            {
                outFile = new File( destDir, xmlFile + fileExt );
            }
            if( m_force ||
                inFile.lastModified() > outFile.lastModified() ||
                styleSheetLastModified > outFile.lastModified() )
            {
                ensureDirectoryFor( outFile );
                getLogger().info( "Processing " + inFile + " to " + outFile );

                configureLiaison( stylesheet );
                m_liaison.transform( inFile, outFile );
            }
        }
        catch( Exception ex )
        {
            // If failed to process document, must delete target document,
            // or it will not attempt to process it the second time
            getLogger().info( "Failed to process " + inFile );
            if( outFile != null )
            {
                outFile.delete();
            }

            throw new TaskException( "Error", ex );
        }

    }//-- processXML

    private void process( File inFile, File outFile, File stylesheet )
        throws TaskException
    {
        try
        {
            final long styleSheetLastModified = stylesheet.lastModified();
            getLogger().debug( "In file " + inFile + " time: " + inFile.lastModified() );
            getLogger().debug( "Out file " + outFile + " time: " + outFile.lastModified() );
            getLogger().debug( "Style file " + m_xslFile + " time: " + styleSheetLastModified );

            if( m_force ||
                inFile.lastModified() > outFile.lastModified() ||
                styleSheetLastModified > outFile.lastModified() )
            {
                ensureDirectoryFor( outFile );
                getLogger().info( "Processing " + inFile + " to " + outFile );
                configureLiaison( stylesheet );
                m_liaison.transform( inFile, outFile );
            }
        }
        catch( Exception ex )
        {
            getLogger().info( "Failed to process " + inFile );
            if( outFile != null )
            {
                outFile.delete();
            }
            throw new TaskException( "Error", ex );
        }
    }

    /**
     * Load processor here instead of in setProcessor - this will be called from
     * within execute, so we have access to the latest classpath.
     *
     * @param proc Description of Parameter
     * @exception Exception Description of Exception
     */
    private void resolveProcessor( String proc )
        throws Exception
    {
        if( proc.equals( "trax" ) )
        {
            final Class clazz =
                loadClass( "org.apache.tools.ant.taskdefs.optional.TraXLiaison" );
            m_liaison = (XSLTLiaison)clazz.newInstance();
        }
        else
        {
            m_liaison = (XSLTLiaison)loadClass( proc ).newInstance();
        }
    }

}
