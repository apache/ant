/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PathUtil;
import org.apache.tools.ant.types.ScannerUtil;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.ParserAdapter;

/**
 * The <code>XMLValidateTask</code> checks that an XML document is valid, with a
 * SAX validating parser.
 *
 * @author Raphael Pierquin <a href="mailto:raphael.pierquin@agisphere.com">
 *      raphael.pierquin@agisphere.com</a>
 */
public class XMLValidateTask
    extends AbstractTask
{
    /**
     * The default implementation parser classname used by the task to process
     * validation.
     */
    // The crimson implementation is shipped with ant.
    public static String DEFAULT_XML_READER_CLASSNAME = "org.apache.crimson.parser.XMLReaderImpl";

    private static String INIT_FAILED_MSG = "Could not start xml validation: ";

    // ant task properties
    // defaults
    private boolean m_warn = true;
    private boolean m_lenient;
    private String m_readerClassName = DEFAULT_XML_READER_CLASSNAME;
    private File m_file;// file to be validated
    private ArrayList m_filesets = new ArrayList();

    /**
     * the parser is viewed as a SAX2 XMLReader. If a SAX1 parser is specified,
     * it's wrapped in an adapter that make it behave as a XMLReader. a more
     * 'standard' way of doing this would be to use the JAXP1.1 SAXParser
     * interface.
     */
    private XMLReader m_xmlReader;// XMLReader used to validation process

    /**
     * to report sax parsing errors
     */
    private ValidatorErrorHandler m_errorHandler;
    private Hashtable m_features = new Hashtable();

    /**
     * The list of configured DTD locations
     */
    private ArrayList m_dtdLocations = new ArrayList();// sets of file to be validated
    private Path m_classpath;

    /**
     * Specify the class name of the SAX parser to be used. (optional)
     *
     * @param className should be an implementation of SAX2 <code>org.xml.sax.XMLReader</code>
     *      or SAX2 <code>org.xml.sax.Parser</code>. <p>
     *
     *      if className is an implementation of <code>org.xml.sax.Parser</code>
     *      , {@link #setLenient(boolean)}, will be ignored. <p>
     *
     *      if not set, the default {@link #DEFAULT_XML_READER_CLASSNAME} will
     *      be used.
     * @see org.xml.sax.XMLReader
     * @see org.xml.sax.Parser
     */
    public void setClassName( final String className )
    {
        m_readerClassName = className;
    }

    /**
     * Specify the classpath to be searched to load the parser (optional)
     */
    public void setClasspath( final Path classpath )
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = classpath;
        }
        else
        {
            m_classpath.append( classpath );
        }
    }

    /**
     * specifify the file to be checked
     *
     * @param file The new File value
     */
    public void setFile( File file )
    {
        m_file = file;
    }

    /**
     * Specify whether the parser should be validating. Default is <code>true</code>
     * . <p>
     *
     * If set to false, the validation will fail only if the parsed document is
     * not well formed XML. <p>
     *
     * this option is ignored if the specified class with {@link
     * #setClassName(String)} is not a SAX2 XMLReader.
     */
    public void setLenient( final boolean bool )
    {
        m_lenient = bool;
    }

    /**
     * Specify how parser error are to be handled. <p>
     *
     * If set to <code>true
     *</true>
     *(default), log a warn message for each SAX warn event.
     */
    public void setWarn( final boolean warn )
    {
        m_warn = warn;
    }

    /**
     * specifify a set of file to be checked
     */
    public void addFileset( final FileSet fileSet )
    {
        m_filesets.add( fileSet );
    }

    /**
     * @see #setClasspath
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

    /**
     * Create a DTD location record. This stores the location of a DTD. The DTD
     * is identified by its public Id. The location may either be a file
     * location or a resource location.
     *
     * @return Description of the Returned Value
     */
    public DTDLocation createDTD()
    {
        final DTDLocation dtdLocation = new DTDLocation();
        m_dtdLocations.add( dtdLocation );
        return dtdLocation;
    }

    public void execute()
        throws TaskException
    {
        int fileProcessed = 0;
        final int size = m_filesets.size();
        if( m_file == null && ( size == 0 ) )
        {
            final String message = "Specify at least one source - a file or a fileset.";
            throw new TaskException( message );
        }

        initValidator();

        if( m_file != null )
        {
            if( m_file.exists() && m_file.canRead() && m_file.isFile() )
            {
                doValidate( m_file );
                fileProcessed++;
            }
            else
            {
                final String message = "File " + m_file + " cannot be read";
                throw new TaskException( message );
            }
        }

        for( int i = 0; i < size; i++ )
        {
            final FileSet fileSet = (FileSet)m_filesets.get( i );
            final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
            final String[] files = scanner.getIncludedFiles();

            for( int j = 0; j < files.length; j++ )
            {
                final File srcFile = new File( fileSet.getDir(), files[ j ] );
                doValidate( srcFile );
                fileProcessed++;
            }
        }
        final String message = fileProcessed + " file(s) have been successfully validated.";
        getLogger().info( message );
    }

    private EntityResolver buildEntityResolver()
    {
        final LocalResolver resolver = new LocalResolver();
        setupLogger( resolver );

        final int size = m_dtdLocations.size();
        for( int i = 0; i < size; i++ )
        {
            final DTDLocation location = (DTDLocation)m_dtdLocations.get( i );
            resolver.registerDTD( location );
        }

        return resolver;
    }

    /*
     * set a feature on the parser.
     * TODO: find a way to set any feature from build.xml
     */
    private boolean setFeature( final String feature,
                                final boolean value,
                                final boolean warn )
    {
        boolean toReturn = false;
        try
        {
            m_xmlReader.setFeature( feature, value );
            toReturn = true;
        }
        catch( SAXNotRecognizedException e )
        {
            final String message = "Could not set feature '" + feature + "' because the parser doesn't recognize it";
            if( warn )
            {
                getLogger().warn( message );
            }
        }
        catch( SAXNotSupportedException e )
        {
            final String message = "Could not set feature '" + feature + "' because the parser doesn't support it";
            if( warn )
            {
                getLogger().warn( message );
            }
        }
        return toReturn;
    }

    /*
     * parse the file
     */
    private void doValidate( File afile )
        throws TaskException
    {
        try
        {
            getLogger().debug( "Validating " + afile.getName() + "... " );
            m_errorHandler.reset();
            InputSource is = new InputSource( new FileReader( afile ) );
            String uri = "file:" + afile.getAbsolutePath().replace( '\\', '/' );
            for( int index = uri.indexOf( '#' ); index != -1;
                 index = uri.indexOf( '#' ) )
            {
                uri = uri.substring( 0, index ) + "%23" + uri.substring( index + 1 );
            }
            is.setSystemId( uri );
            m_xmlReader.parse( is );
        }
        catch( SAXException ex )
        {
            final String message = "Could not validate document " + afile;
            throw new TaskException( message );
        }
        catch( IOException ex )
        {
            final String message = "Could not validate document " + afile;
            throw new TaskException( message, ex );
        }

        if( m_errorHandler.getFailure() )
        {
            final String message = afile + " is not a valid XML document.";
            throw new TaskException( message );
        }
    }

    /**
     * init the parser : load the parser class, and set features if necessary
     */
    private void initValidator()
        throws TaskException
    {
        try
        {
            // load the parser class
            // with JAXP, we would use a SAXParser factory
            Class readerClass = null;
            if( m_classpath != null )
            {
                final URL[] urls = PathUtil.toURLs( m_classpath );
                final ClassLoader classLoader = new URLClassLoader( urls );
                readerClass = classLoader.loadClass( m_readerClassName );
            }
            else
            {
                readerClass = Class.forName( m_readerClassName );
            }

            // then check it implements XMLReader
            if( XMLReader.class.isAssignableFrom( readerClass ) )
            {

                m_xmlReader = (XMLReader)readerClass.newInstance();
                getLogger().debug( "Using SAX2 reader " + m_readerClassName );
            }
            else
            {

                // see if it is a SAX1 Parser
                if( Parser.class.isAssignableFrom( readerClass ) )
                {
                    Parser parser = (Parser)readerClass.newInstance();
                    m_xmlReader = new ParserAdapter( parser );
                    getLogger().debug( "Using SAX1 parser " + m_readerClassName );
                }
                else
                {
                    throw new TaskException( INIT_FAILED_MSG
                                             + m_readerClassName
                                             + " implements nor SAX1 Parser nor SAX2 XMLReader." );
                }
            }
        }
        catch( ClassNotFoundException e )
        {
            throw new TaskException( INIT_FAILED_MSG + m_readerClassName, e );
        }
        catch( InstantiationException e )
        {
            throw new TaskException( INIT_FAILED_MSG + m_readerClassName, e );
        }
        catch( IllegalAccessException e )
        {
            throw new TaskException( INIT_FAILED_MSG + m_readerClassName, e );
        }

        m_xmlReader.setEntityResolver( buildEntityResolver() );

        m_errorHandler = new ValidatorErrorHandler( m_warn );
        setupLogger( m_errorHandler );
        m_xmlReader.setErrorHandler( m_errorHandler );

        if( !( m_xmlReader instanceof ParserAdapter ) )
        {
            // turn validation on
            if( !m_lenient )
            {
                boolean ok = setFeature( "http://xml.org/sax/features/validation", true, true );
                if( !ok )
                {
                    throw new TaskException( INIT_FAILED_MSG
                                             + m_readerClassName
                                             + " doesn't provide validation" );
                }
            }
            // set other features
            Enumeration enum = m_features.keys();
            while( enum.hasMoreElements() )
            {
                String featureId = (String)enum.nextElement();
                setFeature( featureId, ( (Boolean)m_features.get( featureId ) ).booleanValue(), true );
            }
        }
    }

}
