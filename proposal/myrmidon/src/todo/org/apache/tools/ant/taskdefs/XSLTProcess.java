/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;

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

public class XSLTProcess extends MatchingTask implements XSLTLogger
{

    private File destDir = null;

    private File baseDir = null;

    private String xslFile = null;

    private String targetExtension = ".html";
    private Vector params = new Vector();

    private File inFile = null;

    private File outFile = null;
    private Path classpath = null;
    private boolean stylesheetLoaded = false;

    private boolean force = false;

    private String outputtype = null;

    private FileUtils fileUtils;
    private XSLTLiaison liaison;

    private String processor;

    /**
     * Creates a new XSLTProcess Task.
     */
    public XSLTProcess()
    {
        fileUtils = FileUtils.newFileUtils();
    }//-- setForce

    /**
     * Set the base directory.
     *
     * @param dir The new Basedir value
     */
    public void setBasedir( File dir )
    {
        baseDir = dir;
    }

    /**
     * Set the classpath to load the Processor through (attribute).
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
    {
        createClasspath().append( classpath );
    }

    /**
     * Set the classpath to load the Processor through via reference
     * (attribute).
     *
     * @param r The new ClasspathRef value
     */
    public void setClasspathRef( Reference r )
    {
        createClasspath().setRefid( r );
    }//-- setSourceDir

    /**
     * Set the destination directory into which the XSL result files should be
     * copied to
     *
     * @param dir The new Destdir value
     */
    public void setDestdir( File dir )
    {
        destDir = dir;
    }//-- setDestDir

    /**
     * Set the desired file extension to be used for the target
     *
     * @param name the extension to use
     */
    public void setExtension( String name )
    {
        targetExtension = name;
    }//-- execute

    /**
     * Set whether to check dependencies, or always generate.
     *
     * @param force The new Force value
     */
    public void setForce( boolean force )
    {
        this.force = force;
    }

    /**
     * Sets an input xml file to be styled
     *
     * @param inFile The new In value
     */
    public void setIn( File inFile )
    {
        this.inFile = inFile;
    }

    /**
     * Sets an out file
     *
     * @param outFile The new Out value
     */
    public void setOut( File outFile )
    {
        this.outFile = outFile;
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
        this.outputtype = type;
    }

    public void setProcessor( String processor )
    {
        this.processor = processor;
    }//-- setDestDir

    /**
     * Sets the file to use for styling relative to the base directory of this
     * task.
     *
     * @param xslFile The new Style value
     */
    public void setStyle( String xslFile )
    {
        this.xslFile = xslFile;
    }

    /**
     * Set the classpath to load the Processor through (nested element).
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( classpath == null )
        {
            classpath = new Path( project );
        }
        return classpath.createPath();
    }

    public Param createParam()
    {
        Param p = new Param();
        params.addElement( p );
        return p;
    }//-- XSLTProcess

    /**
     * Executes the task.
     *
     * @exception BuildException Description of Exception
     */

    public void execute()
        throws BuildException
    {
        DirectoryScanner scanner;
        String[] list;
        String[] dirs;

        if( xslFile == null )
        {
            throw new BuildException( "no stylesheet specified" );
        }

        if( baseDir == null )
        {
            baseDir = resolveFile( "." );
        }

        liaison = getLiaison();

        // check if liaison wants to log errors using us as logger
        if( liaison instanceof XSLTLoggerAware )
        {
            ( (XSLTLoggerAware)liaison ).setLogger( this );
        }

        log( "Using " + liaison.getClass().toString(), Project.MSG_VERBOSE );

        File stylesheet = resolveFile( xslFile );

        // if we have an in file and out then process them
        if( inFile != null && outFile != null )
        {
            process( inFile, outFile, stylesheet );
            return;
        }

        /*
         * if we get here, in and out have not been specified, we are
         * in batch processing mode.
         */
        //-- make sure Source directory exists...
        if( destDir == null )
        {
            String msg = "destdir attributes must be set!";
            throw new BuildException( msg );
        }
        scanner = getDirectoryScanner( baseDir );
        log( "Transforming into " + destDir, Project.MSG_INFO );

        // Process all the files marked for styling
        list = scanner.getIncludedFiles();
        for( int i = 0; i < list.length; ++i )
        {
            process( baseDir, list[ i ], destDir, stylesheet );
        }

        // Process all the directoried marked for styling
        dirs = scanner.getIncludedDirectories();
        for( int j = 0; j < dirs.length; ++j )
        {
            list = new File( baseDir, dirs[ j ] ).list();
            for( int i = 0; i < list.length; ++i )
                process( baseDir, list[ i ], destDir, stylesheet );
        }
    }

    protected XSLTLiaison getLiaison()
    {
        // if processor wasn't specified, see if TraX is available.  If not,
        // default it to xslp or xalan, depending on which is in the classpath
        if( liaison == null )
        {
            if( processor != null )
            {
                try
                {
                    resolveProcessor( processor );
                }
                catch( Exception e )
                {
                    throw new BuildException( "Error", e );
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
                                throw new BuildException( "Error", e1 );
                            }
                        }
                    }
                }
            }
        }
        return liaison;
    }

    /**
     * Loads the stylesheet and set xsl:param parameters.
     *
     * @param stylesheet Description of Parameter
     * @exception BuildException Description of Exception
     */
    protected void configureLiaison( File stylesheet )
        throws BuildException
    {
        if( stylesheetLoaded )
        {
            return;
        }
        stylesheetLoaded = true;

        try
        {
            log( "Loading stylesheet " + stylesheet, Project.MSG_INFO );
            liaison.setStylesheet( stylesheet );
            for( Enumeration e = params.elements(); e.hasMoreElements(); )
            {
                Param p = (Param)e.nextElement();
                liaison.addParam( p.getName(), p.getExpression() );
            }
        }
        catch( Exception ex )
        {
            log( "Failed to read stylesheet " + stylesheet, Project.MSG_INFO );
            throw new BuildException( "Error", ex );
        }
    }

    private void ensureDirectoryFor( File targetFile )
        throws BuildException
    {
        File directory = new File( targetFile.getParent() );
        if( !directory.exists() )
        {
            if( !directory.mkdirs() )
            {
                throw new BuildException( "Unable to create directory: "
                                          + directory.getAbsolutePath() );
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
        if( classpath == null )
        {
            return Class.forName( classname );
        }
        else
        {
            AntClassLoader al = new AntClassLoader( project, classpath );
            Class c = al.loadClass( classname );
            AntClassLoader.initializeClass( c );
            return c;
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
     * @exception BuildException Description of Exception
     */
    private void process( File baseDir, String xmlFile, File destDir,
                          File stylesheet )
        throws BuildException
    {

        String fileExt = targetExtension;
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
            if( force ||
                inFile.lastModified() > outFile.lastModified() ||
                styleSheetLastModified > outFile.lastModified() )
            {
                ensureDirectoryFor( outFile );
                log( "Processing " + inFile + " to " + outFile );

                configureLiaison( stylesheet );
                liaison.transform( inFile, outFile );
            }
        }
        catch( Exception ex )
        {
            // If failed to process document, must delete target document,
            // or it will not attempt to process it the second time
            log( "Failed to process " + inFile, Project.MSG_INFO );
            if( outFile != null )
            {
                outFile.delete();
            }

            throw new BuildException( "Error", ex );
        }

    }//-- processXML

    private void process( File inFile, File outFile, File stylesheet )
        throws BuildException
    {
        try
        {
            long styleSheetLastModified = stylesheet.lastModified();
            log( "In file " + inFile + " time: " + inFile.lastModified(), Project.MSG_DEBUG );
            log( "Out file " + outFile + " time: " + outFile.lastModified(), Project.MSG_DEBUG );
            log( "Style file " + xslFile + " time: " + styleSheetLastModified, Project.MSG_DEBUG );
            if( force ||
                inFile.lastModified() > outFile.lastModified() ||
                styleSheetLastModified > outFile.lastModified() )
            {
                ensureDirectoryFor( outFile );
                log( "Processing " + inFile + " to " + outFile, Project.MSG_INFO );
                configureLiaison( stylesheet );
                liaison.transform( inFile, outFile );
            }
        }
        catch( Exception ex )
        {
            log( "Failed to process " + inFile, Project.MSG_INFO );
            if( outFile != null )
                outFile.delete();
            throw new BuildException( "Error", ex );
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
            liaison = (XSLTLiaison)clazz.newInstance();
        }
        else if( proc.equals( "xalan" ) )
        {
            final Class clazz =
                loadClass( "org.apache.tools.ant.taskdefs.optional.XalanLiaison" );
            liaison = (XSLTLiaison)clazz.newInstance();
        }
        else
        {
            liaison = (XSLTLiaison)loadClass( proc ).newInstance();
        }
    }

    public class Param
    {
        private String name = null;
        private String expression = null;

        public void setExpression( String expression )
        {
            this.expression = expression;
        }

        public void setName( String name )
        {
            this.name = name;
        }

        public String getExpression()
            throws BuildException
        {
            if( expression == null )
                throw new BuildException( "Expression attribute is missing." );
            return expression;
        }

        public String getName()
            throws BuildException
        {
            if( name == null )
                throw new BuildException( "Name attribute is missing." );
            return name;
        }
    }

}//-- XSLTProcess
