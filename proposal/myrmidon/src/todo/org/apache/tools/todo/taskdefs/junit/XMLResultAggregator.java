/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.FileSet;
import org.apache.tools.todo.types.ScannerUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <p>
 *
 * This is an helper class that will aggregate all testsuites under a specific
 * directory and create a new single document. It is not particulary clean but
 * should be helpful while I am thinking about another technique. <p>
 *
 * The main problem is due to the fact that a JVM can be forked for a testcase
 * thus making it impossible to aggregate all testcases since the listener is
 * (obviously) in the forked JVM. A solution could be to write a TestListener
 * that will receive events from the TestRunner via sockets. This is IMHO the
 * simplest way to do it to avoid this file hacking thing.
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class XMLResultAggregator
    extends AbstractTask
    implements XMLConstants
{
    /**
     * the default directory: <tt>.</tt> . It is resolved from the project
     * directory
     */
    public final static String DEFAULT_DIR = ".";

    /**
     * the default file name: <tt>TESTS-TestSuites.xml</tt>
     */
    public final static String DEFAULT_FILENAME = "TESTS-TestSuites.xml";

    /**
     * the list of all filesets, that should contains the xml to aggregate
     */
    protected ArrayList filesets = new ArrayList();

    protected ArrayList transformers = new ArrayList();

    /**
     * the directory to write the file to
     */
    protected File toDir;

    /**
     * the name of the result file
     */
    protected String toFile;

    /**
     * Create a new document builder. Will issue an <tt>
     * ExceptionInitializerError</tt> if something is going wrong. It is fatal
     * anyway.
     *
     * @return a new document builder to create a DOM
     * @todo factorize this somewhere else. It is duplicated code.
     */
    private static DocumentBuilder getDocumentBuilder()
    {
        try
        {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch( Exception exc )
        {
            throw new ExceptionInInitializerError( exc );
        }
    }

    /**
     * Set the destination directory where the results should be written. If not
     * set if will use {@link #DEFAULT_DIR}. When given a relative directory it
     * will resolve it from the project directory.
     *
     * @param value the directory where to write the results, absolute or
     *      relative.
     */
    public void setTodir( File value )
    {
        toDir = value;
    }

    /**
     * Set the name of the file aggregating the results. It must be relative
     * from the <tt>todir</tt> attribute. If not set it will use {@link
     * #DEFAULT_FILENAME}
     *
     * @param value the name of the file.
     * @see #setTodir(File)
     */
    public void setTofile( String value )
    {
        toFile = value;
    }

    /**
     * Add a new fileset containing the xml results to aggregate
     *
     * @param fs the new fileset of xml results.
     */
    public void addFileSet( FileSet fs )
    {
        filesets.add( fs );
    }

    public AggregateTransformer createReport()
    {
        AggregateTransformer transformer = new AggregateTransformer( getContext() );
        transformers.add( transformer );
        return transformer;
    }

    /**
     * Aggregate all testsuites into a single document and write it to the
     * specified directory and file.
     *
     * @throws TaskException thrown if there is a serious error while writing
     *      the document.
     */
    public void execute()
        throws TaskException
    {
        final Element rootElement = createDocument();
        File destFile = getDestinationFile();
        // write the document
        try
        {
            writeDOMTree( rootElement.getOwnerDocument(), destFile );
        }
        catch( IOException e )
        {
            throw new TaskException( "Unable to write test aggregate to '" + destFile + "'", e );
        }
        // apply transformation
        Iterator enum = transformers.iterator();
        while( enum.hasNext() )
        {
            AggregateTransformer transformer =
                (AggregateTransformer)enum.next();
            transformer.setXmlDocument( rootElement.getOwnerDocument() );
            transformer.transform();
        }
    }

    /**
     * Get the full destination file where to write the result. It is made of
     * the <tt>todir</tt> and <tt>tofile</tt> attributes.
     *
     * @return the destination file where should be written the result file.
     */
    protected File getDestinationFile()
        throws TaskException
    {
        if( toFile == null )
        {
            toFile = DEFAULT_FILENAME;
        }
        if( toDir == null )
        {
            toDir = getContext().resolveFile( DEFAULT_DIR );
        }
        return new File( toDir, toFile );
    }

    /**
     * Get all <code>.xml</code> files in the fileset.
     *
     * @return all files in the fileset that end with a '.xml'.
     */
    protected File[] getFiles()
        throws TaskException
    {
        final ArrayList v = new ArrayList();
        final int size = filesets.size();
        for( int i = 0; i < size; i++ )
        {
            final FileSet fileSet = (FileSet)filesets.get( i );
            final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
            scanner.scan();
            final String[] includes = scanner.getIncludedFiles();
            for( int j = 0; j < includes.length; j++ )
            {
                final String pathname = includes[ j ];
                if( pathname.endsWith( ".xml" ) )
                {
                    File file = new File( scanner.getBasedir(), pathname );
                    file = getContext().resolveFile( file.getPath() );
                    v.add( file );
                }
            }
        }

        return (File[])v.toArray( new File[ v.size() ] );
    }

    /**
     * <p>
     *
     * Add a new testsuite node to the document. The main difference is that it
     * split the previous fully qualified name into a package and a name. <p>
     *
     * For example: <tt>org.apache.Whatever</tt> will be split into <tt>
     * org.apache</tt> and <tt>Whatever</tt> .
     *
     * @param root the root element to which the <tt>testsuite</tt> node should
     *      be appended.
     * @param testsuite the element to append to the given root. It will
     *      slightly modify the original node to change the name attribute and
     *      add a package one.
     */
    protected void addTestSuite( Element root, Element testsuite )
    {
        String fullclassname = testsuite.getAttribute( ATTR_NAME );
        int pos = fullclassname.lastIndexOf( '.' );

        // a missing . might imply no package at all. Don't get fooled.
        String pkgName = ( pos == -1 ) ? "" : fullclassname.substring( 0, pos );
        String classname = ( pos == -1 ) ? fullclassname : fullclassname.substring( pos + 1 );
        Element copy = (Element)DOMUtil.importNode( root, testsuite );

        // modify the name attribute and set the package
        copy.setAttribute( ATTR_NAME, classname );
        copy.setAttribute( ATTR_PACKAGE, pkgName );
    }

    /**
     * <p>
     *
     * Create a DOM tree. Has 'testsuites' as firstchild and aggregates all
     * testsuite results that exists in the base directory.
     *
     * @return the root element of DOM tree that aggregates all testsuites.
     */
    protected Element createDocument()
        throws TaskException
    {
        // create the dom tree
        DocumentBuilder builder = getDocumentBuilder();
        Document doc = builder.newDocument();
        Element rootElement = doc.createElement( TESTSUITES );
        doc.appendChild( rootElement );

        // get all files and add them to the document
        final File[] files = getFiles();
        for( int i = 0; i < files.length; i++ )
        {
            try
            {
                getContext().debug( "Parsing file: '" + files[ i ] + "'" );
                //XXX there seems to be a bug in xerces 1.3.0 that doesn't like file object
                // will investigate later. It does not use the given directory but
                // the vm dir instead ? Works fine with crimson.
                Document testsuiteDoc = builder.parse( "file:///" + files[ i ].getAbsolutePath() );
                Element elem = testsuiteDoc.getDocumentElement();
                // make sure that this is REALLY a testsuite.
                if( TESTSUITE.equals( elem.getNodeName() ) )
                {
                    addTestSuite( rootElement, elem );
                }
                else
                {
                    // issue a warning.
                    getContext().warn( "the file " + files[ i ] + " is not a valid testsuite XML document" );
                }
            }
            catch( SAXException e )
            {
                // a testcase might have failed and write a zero-length document,
                // It has already failed, but hey.... mm. just put a warning
                getContext().warn( "The file " + files[ i ] + " is not a valid XML document. It is possibly corrupted." );
                getContext().debug( ExceptionUtil.printStackTrace( e ) );
            }
            catch( IOException e )
            {
                getContext().error( "Error while accessing file " + files[ i ] + ": " + e.getMessage() );
            }
        }
        return rootElement;
    }

    //----- from now, the methods are all related to DOM tree manipulation

    /**
     * Write the DOM tree to a file.
     *
     * @param doc the XML document to dump to disk.
     * @param file the filename to write the document to. Should obviouslly be a
     *      .xml file.
     * @throws IOException thrown if there is an error while writing the
     *      content.
     */
    protected void writeDOMTree( Document doc, File file )
        throws IOException
    {
        OutputStream out = new FileOutputStream( file );
        PrintWriter wri = new PrintWriter( new OutputStreamWriter( out, "UTF8" ) );
        wri.write( "<?xml version=\"1.0\"?>\n" );
        ( new DOMElementWriter() ).write( doc.getDocumentElement(), wri, 0, "  " );
        wri.flush();
        wri.close();
        // writers do not throw exceptions, so check for them.
        if( wri.checkError() )
        {
            throw new IOException( "Error while writing DOM content" );
        }
    }

}
