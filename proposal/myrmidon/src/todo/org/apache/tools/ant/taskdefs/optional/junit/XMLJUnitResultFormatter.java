/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.util.DOMElementWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Prints XML output of the test to a specified Writer.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:erik@hatcher.net">Erik Hatcher</a>
 * @see FormatterElement
 */

public class XMLJUnitResultFormatter implements JUnitResultFormatter, XMLConstants
{
    /**
     * Element for the current test.
     */
    private Hashtable testElements = new Hashtable();
    /**
     * Timing helper.
     */
    private Hashtable testStarts = new Hashtable();

    /**
     * The XML document.
     */
    private Document doc;
    /**
     * Where to write the log to.
     */
    private OutputStream out;
    /**
     * The wrapper for the whole testsuite.
     */
    private Element rootElement;

    public XMLJUnitResultFormatter()
    {
    }

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

    public void setOutput( OutputStream out )
    {
        this.out = out;
    }

    public void setSystemError( String out )
    {
        formatOutput( SYSTEM_ERR, out );
    }

    public void setSystemOutput( String out )
    {
        formatOutput( SYSTEM_OUT, out );
    }

    /**
     * Interface TestListener. <p>
     *
     * An error occured while running the test.
     *
     * @param test The feature to be added to the Error attribute
     * @param t The feature to be added to the Error attribute
     */
    public void addError( Test test, Throwable t )
    {
        formatError( ERROR, test, t );
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4. <p>
     *
     * A Test failed.
     *
     * @param test The feature to be added to the Failure attribute
     * @param t The feature to be added to the Failure attribute
     */
    public void addFailure( Test test, Throwable t )
    {
        formatError( FAILURE, test, t );
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4. <p>
     *
     * A Test failed.
     *
     * @param test The feature to be added to the Failure attribute
     * @param t The feature to be added to the Failure attribute
     */
    public void addFailure( Test test, AssertionFailedError t )
    {
        addFailure( test, (Throwable)t );
    }

    /**
     * Interface TestListener. <p>
     *
     * A Test is finished.
     *
     * @param test Description of Parameter
     */
    public void endTest( Test test )
    {
        Element currentTest = (Element)testElements.get( test );
        Long l = (Long)testStarts.get( test );
        currentTest.setAttribute( ATTR_TIME,
                                  "" + ( ( System.currentTimeMillis() - l.longValue() )
                                         / 1000.0 ) );
    }

    /**
     * The whole testsuite ended.
     *
     * @param suite Description of Parameter
     * @exception TaskException Description of Exception
     */
    public void endTestSuite( JUnitTest suite )
        throws TaskException
    {
        rootElement.setAttribute( ATTR_TESTS, "" + suite.runCount() );
        rootElement.setAttribute( ATTR_FAILURES, "" + suite.failureCount() );
        rootElement.setAttribute( ATTR_ERRORS, "" + suite.errorCount() );
        rootElement.setAttribute( ATTR_TIME, "" + ( suite.getRunTime() / 1000.0 ) );
        if( out != null )
        {
            Writer wri = null;
            try
            {
                wri = new OutputStreamWriter( out, "UTF8" );
                wri.write( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" );
                ( new DOMElementWriter() ).write( rootElement, wri, 0, "  " );
                wri.flush();
            }
            catch( IOException exc )
            {
                throw new TaskException( "Unable to write log file", exc );
            }
            finally
            {
                if( out != System.out && out != System.err )
                {
                    if( wri != null )
                    {
                        try
                        {
                            wri.close();
                        }
                        catch( IOException e )
                        {
                        }
                    }
                }
            }
        }
    }

    /**
     * Interface TestListener. <p>
     *
     * A new Test is started.
     *
     * @param t Description of Parameter
     */
    public void startTest( Test t )
    {
        testStarts.put( t, new Long( System.currentTimeMillis() ) );

        Element currentTest = doc.createElement( TESTCASE );
        currentTest.setAttribute( ATTR_NAME,
                                  JUnitVersionHelper.getTestCaseName( t ) );
        rootElement.appendChild( currentTest );
        testElements.put( t, currentTest );
    }

    /**
     * The whole testsuite started.
     *
     * @param suite Description of Parameter
     */
    public void startTestSuite( JUnitTest suite )
    {
        doc = getDocumentBuilder().newDocument();
        rootElement = doc.createElement( TESTSUITE );
        rootElement.setAttribute( ATTR_NAME, suite.getName() );

        // Output properties
        Element propsElement = doc.createElement( PROPERTIES );
        rootElement.appendChild( propsElement );
        Properties props = suite.getProperties();
        if( props != null )
        {
            Enumeration e = props.propertyNames();
            while( e.hasMoreElements() )
            {
                String name = (String)e.nextElement();
                Element propElement = doc.createElement( PROPERTY );
                propElement.setAttribute( ATTR_NAME, name );
                propElement.setAttribute( ATTR_VALUE, props.getProperty( name ) );
                propsElement.appendChild( propElement );
            }
        }
    }

    private void formatError( String type, Test test, Throwable t )
    {
        if( test != null )
        {
            endTest( test );
        }

        Element nested = doc.createElement( type );
        Element currentTest = null;
        if( test != null )
        {
            currentTest = (Element)testElements.get( test );
        }
        else
        {
            currentTest = rootElement;
        }

        currentTest.appendChild( nested );

        String message = t.getMessage();
        if( message != null && message.length() > 0 )
        {
            nested.setAttribute( ATTR_MESSAGE, t.getMessage() );
        }
        nested.setAttribute( ATTR_TYPE, t.getClass().getName() );

        String strace = JUnitTestRunner.getFilteredTrace( t );
        Text trace = doc.createTextNode( strace );
        nested.appendChild( trace );
    }

    private void formatOutput( String type, String output )
    {
        Element nested = doc.createElement( type );
        rootElement.appendChild( nested );
        Text content = doc.createTextNode( output );
        nested.appendChild( content );
    }

}// XMLJUnitResultFormatter
