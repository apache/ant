/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.ParserAdapter;

/**
 * The <code>XMLValidateTask</code> checks that an XML document is valid, with a
 * SAX validating parser.
 *
 * @author Raphael Pierquin <a href="mailto:raphael.pierquin@agisphere.com">
 *      raphael.pierquin@agisphere.com</a>
 */
public class XMLValidateTask extends Task
{

    /**
     * The default implementation parser classname used by the task to process
     * validation.
     */
    // The crimson implementation is shipped with ant.
    public static String DEFAULT_XML_READER_CLASSNAME = "org.apache.crimson.parser.XMLReaderImpl";

    protected static String INIT_FAILED_MSG = "Could not start xml validation: ";

    // ant task properties
    // defaults
    protected boolean failOnError = true;
    protected boolean warn = true;
    protected boolean lenient = false;
    protected String readerClassName = DEFAULT_XML_READER_CLASSNAME;

    protected File file = null;// file to be validated
    protected Vector filesets = new Vector();

    /**
     * the parser is viewed as a SAX2 XMLReader. If a SAX1 parser is specified,
     * it's wrapped in an adapter that make it behave as a XMLReader. a more
     * 'standard' way of doing this would be to use the JAXP1.1 SAXParser
     * interface.
     */
    protected XMLReader xmlReader = null;// XMLReader used to validation process
    protected ValidatorErrorHandler errorHandler
        = new ValidatorErrorHandler();// to report sax parsing errors
    protected Hashtable features = new Hashtable();

    /**
     * The list of configured DTD locations
     */
    public Vector dtdLocations = new Vector();// sets of file to be validated
    protected Path classpath;

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
    public void setClassName( String className )
    {

        readerClassName = className;
    }

    /**
     * Specify the classpath to be searched to load the parser (optional)
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
        throws TaskException
    {
        if( this.classpath == null )
        {
            this.classpath = classpath;
        }
        else
        {
            this.classpath.append( classpath );
        }
    }

    /**
     * @param r The new ClasspathRef value
     * @see #setClasspath
     */
    public void setClasspathRef( Reference r )
        throws TaskException
    {
        createClasspath().setRefid( r );
    }

    /**
     * Specify how parser error are to be handled. <p>
     *
     * If set to <code>true</code> (default), throw a TaskException if the
     * parser yields an error.
     *
     * @param fail The new FailOnError value
     */
    public void setFailOnError( boolean fail )
    {

        failOnError = fail;
    }

    /**
     * specifify the file to be checked
     *
     * @param file The new File value
     */
    public void setFile( File file )
    {
        this.file = file;
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
     *
     * @param bool The new Lenient value
     */
    public void setLenient( boolean bool )
    {

        lenient = bool;
    }

    /**
     * Specify how parser error are to be handled. <p>
     *
     * If set to <code>true
     *</true>
     *(default), log a warn message for each SAX warn event.
     *
     * @param bool The new Warn value
     */
    public void setWarn( boolean bool )
    {

        warn = bool;
    }

    /**
     * specifify a set of file to be checked
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        filesets.addElement( set );
    }

    /**
     * @return Description of the Returned Value
     * @see #setClasspath
     */
    public Path createClasspath()
        throws TaskException
    {
        if( this.classpath == null )
        {
            this.classpath = new Path( project );
        }
        return this.classpath.createPath();
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
        DTDLocation dtdLocation = new DTDLocation();
        dtdLocations.addElement( dtdLocation );

        return dtdLocation;
    }

    public void execute()
        throws TaskException
    {

        int fileProcessed = 0;
        if( file == null && ( filesets.size() == 0 ) )
        {
            throw new TaskException( "Specify at least one source - a file or a fileset." );
        }

        initValidator();

        if( file != null )
        {
            if( file.exists() && file.canRead() && file.isFile() )
            {
                doValidate( file );
                fileProcessed++;
            }
            else
            {
                String errorMsg = "File " + file + " cannot be read";
                if( failOnError )
                    throw new TaskException( errorMsg );
                else
                    log( errorMsg, Project.MSG_ERR );
            }
        }

        for( int i = 0; i < filesets.size(); i++ )
        {

            FileSet fs = (FileSet)filesets.elementAt( i );
            DirectoryScanner ds = fs.getDirectoryScanner( project );
            String[] files = ds.getIncludedFiles();

            for( int j = 0; j < files.length; j++ )
            {
                File srcFile = new File( fs.getDir( project ), files[ j ] );
                doValidate( srcFile );
                fileProcessed++;
            }
        }
        log( fileProcessed + " file(s) have been successfully validated." );
    }

    protected EntityResolver getEntityResolver()
    {
        LocalResolver resolver = new LocalResolver();

        for( Enumeration i = dtdLocations.elements(); i.hasMoreElements(); )
        {
            DTDLocation location = (DTDLocation)i.nextElement();
            resolver.registerDTD( location );
        }
        return resolver;
    }

    /*
     * set a feature on the parser.
     * TODO: find a way to set any feature from build.xml
     */
    private boolean setFeature( String feature, boolean value, boolean warn )
    {

        boolean toReturn = false;
        try
        {
            xmlReader.setFeature( feature, value );
            toReturn = true;
        }
        catch( SAXNotRecognizedException e )
        {
            if( warn )
                log( "Could not set feature '"
                     + feature
                     + "' because the parser doesn't recognize it",
                     Project.MSG_WARN );
        }
        catch( SAXNotSupportedException e )
        {
            if( warn )
                log( "Could not set feature '"
                     + feature
                     + "' because the parser doesn't support it",
                     Project.MSG_WARN );
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
            log( "Validating " + afile.getName() + "... ", Project.MSG_VERBOSE );
            errorHandler.init( afile );
            InputSource is = new InputSource( new FileReader( afile ) );
            String uri = "file:" + afile.getAbsolutePath().replace( '\\', '/' );
            for( int index = uri.indexOf( '#' ); index != -1;
                 index = uri.indexOf( '#' ) )
            {
                uri = uri.substring( 0, index ) + "%23" + uri.substring( index + 1 );
            }
            is.setSystemId( uri );
            xmlReader.parse( is );
        }
        catch( SAXException ex )
        {
            if( failOnError )
                throw new TaskException( "Could not validate document " + afile );
        }
        catch( IOException ex )
        {
            throw new TaskException( "Could not validate document " + afile, ex );
        }

        if( errorHandler.getFailure() )
        {
            if( failOnError )
                throw new TaskException( afile + " is not a valid XML document." );
            else
                log( afile + " is not a valid XML document", Project.MSG_ERR );
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
            //Class readerImpl = null;
            //Class parserImpl = null;
            if( classpath != null )
            {
                AntClassLoader loader = new AntClassLoader( project, classpath );
                //                loader.addSystemPackageRoot("org.xml"); // needed to avoid conflict
                readerClass = loader.loadClass( readerClassName );
                AntClassLoader.initializeClass( readerClass );
            }
            else
                readerClass = Class.forName( readerClassName );

            // then check it implements XMLReader
            if( XMLReader.class.isAssignableFrom( readerClass ) )
            {

                xmlReader = (XMLReader)readerClass.newInstance();
                log( "Using SAX2 reader " + readerClassName, Project.MSG_VERBOSE );
            }
            else
            {

                // see if it is a SAX1 Parser
                if( Parser.class.isAssignableFrom( readerClass ) )
                {
                    Parser parser = (Parser)readerClass.newInstance();
                    xmlReader = new ParserAdapter( parser );
                    log( "Using SAX1 parser " + readerClassName, Project.MSG_VERBOSE );
                }
                else
                {
                    throw new TaskException( INIT_FAILED_MSG
                                             + readerClassName
                                             + " implements nor SAX1 Parser nor SAX2 XMLReader." );
                }
            }
        }
        catch( ClassNotFoundException e )
        {
            throw new TaskException( INIT_FAILED_MSG + readerClassName, e );
        }
        catch( InstantiationException e )
        {
            throw new TaskException( INIT_FAILED_MSG + readerClassName, e );
        }
        catch( IllegalAccessException e )
        {
            throw new TaskException( INIT_FAILED_MSG + readerClassName, e );
        }

        xmlReader.setEntityResolver( getEntityResolver() );
        xmlReader.setErrorHandler( errorHandler );

        if( !( xmlReader instanceof ParserAdapter ) )
        {
            // turn validation on
            if( !lenient )
            {
                boolean ok = setFeature( "http://xml.org/sax/features/validation", true, true );
                if( !ok )
                {
                    throw new TaskException( INIT_FAILED_MSG
                                             + readerClassName
                                             + " doesn't provide validation" );
                }
            }
            // set other features
            Enumeration enum = features.keys();
            while( enum.hasMoreElements() )
            {
                String featureId = (String)enum.nextElement();
                setFeature( featureId, ( (Boolean)features.get( featureId ) ).booleanValue(), true );
            }
        }
    }

    public static class DTDLocation
    {
        private String publicId = null;
        private String location = null;

        public void setLocation( String location )
        {
            this.location = location;
        }

        public void setPublicId( String publicId )
        {
            this.publicId = publicId;
        }

        public String getLocation()
        {
            return location;
        }

        public String getPublicId()
        {
            return publicId;
        }
    }

    /*
     * ValidatorErrorHandler role :
     * <ul>
     * <li> log SAX parse exceptions,
     * <li> remember if an error occured
     * </ul>
     */
    protected class ValidatorErrorHandler implements ErrorHandler
    {

        protected File currentFile = null;
        protected String lastErrorMessage = null;
        protected boolean failed = false;

        // did an error happen during last parsing ?
        public boolean getFailure()
        {

            return failed;
        }

        public void error( SAXParseException exception )
        {
            failed = true;
            doLog( exception, Project.MSG_ERR );
        }

        public void fatalError( SAXParseException exception )
        {
            failed = true;
            doLog( exception, Project.MSG_ERR );
        }

        public void init( File file )
        {
            currentFile = file;
            failed = false;
        }

        public void warning( SAXParseException exception )
        {
            // depending on implementation, XMLReader can yield hips of warning,
            // only output then if user explicitely asked for it
            if( warn )
                doLog( exception, Project.MSG_WARN );
        }

        private String getMessage( SAXParseException e )
        {
            String sysID = e.getSystemId();
            if( sysID != null )
            {
                try
                {
                    int line = e.getLineNumber();
                    int col = e.getColumnNumber();
                    return new URL( sysID ).getFile() +
                        ( line == -1 ? "" : ( ":" + line +
                        ( col == -1 ? "" : ( ":" + col ) ) ) ) +
                        ": " + e.getMessage();
                }
                catch( MalformedURLException mfue )
                {
                }
            }
            return e.getMessage();
        }

        private void doLog( SAXParseException e, int logLevel )
        {

            log( getMessage( e ), logLevel );
        }
    }

    private class LocalResolver
        implements EntityResolver
    {
        private Hashtable fileDTDs = new Hashtable();
        private Hashtable resourceDTDs = new Hashtable();
        private Hashtable urlDTDs = new Hashtable();

        public LocalResolver()
        {
        }

        public void registerDTD( String publicId, String location )
        {
            if( location == null )
            {
                return;
            }

            File fileDTD = new File( location );
            if( fileDTD.exists() )
            {
                if( publicId != null )
                {
                    fileDTDs.put( publicId, fileDTD );
                    log( "Mapped publicId " + publicId + " to file " + fileDTD, Project.MSG_VERBOSE );
                }
                return;
            }

            if( LocalResolver.this.getClass().getResource( location ) != null )
            {
                if( publicId != null )
                {
                    resourceDTDs.put( publicId, location );
                    log( "Mapped publicId " + publicId + " to resource " + location, Project.MSG_VERBOSE );
                }
            }

            try
            {
                if( publicId != null )
                {
                    URL urldtd = new URL( location );
                    urlDTDs.put( publicId, urldtd );
                }
            }
            catch( java.net.MalformedURLException e )
            {
                //ignored
            }
        }

        public void registerDTD( DTDLocation location )
        {
            registerDTD( location.getPublicId(), location.getLocation() );
        }

        public InputSource resolveEntity( String publicId, String systemId )
            throws SAXException
        {
            File dtdFile = (File)fileDTDs.get( publicId );
            if( dtdFile != null )
            {
                try
                {
                    log( "Resolved " + publicId + " to local file " + dtdFile, Project.MSG_VERBOSE );
                    return new InputSource( new FileInputStream( dtdFile ) );
                }
                catch( FileNotFoundException ex )
                {
                    // ignore
                }
            }

            String dtdResourceName = (String)resourceDTDs.get( publicId );
            if( dtdResourceName != null )
            {
                InputStream is = this.getClass().getResourceAsStream( dtdResourceName );
                if( is != null )
                {
                    log( "Resolved " + publicId + " to local resource " + dtdResourceName, Project.MSG_VERBOSE );
                    return new InputSource( is );
                }
            }

            URL dtdUrl = (URL)urlDTDs.get( publicId );
            if( dtdUrl != null )
            {
                try
                {
                    InputStream is = dtdUrl.openStream();
                    log( "Resolved " + publicId + " to url " + dtdUrl, Project.MSG_VERBOSE );
                    return new InputSource( is );
                }
                catch( IOException ioe )
                {
                    //ignore
                }
            }

            log( "Could not resolve ( publicId: " + publicId + ", systemId: " + systemId + ") to a local entity",
                 Project.MSG_INFO );

            return null;
        }
    }
}
