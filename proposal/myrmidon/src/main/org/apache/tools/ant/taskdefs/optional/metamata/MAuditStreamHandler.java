/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.api.TaskException;
import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.tools.ant.taskdefs.exec.ExecuteStreamHandler;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This is a very bad stream handler for the MAudit task. All report to stdout
 * that does not match a specific report pattern is dumped to the Ant output as
 * warn level. The report that match the pattern is stored in a map with the key
 * being the filepath that caused the error report. <p>
 *
 * The limitation with the choosen implementation is clear:
 * <ul>
 *   <li> it does not handle multiline report( message that has \n ). the part
 *   until the \n will be stored and the other part (which will not match the
 *   pattern) will go to Ant output in Warn level.
 *   <li> it does not report error that goes to stderr.
 * </ul>
 *
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
class MAuditStreamHandler
    extends AbstractLogEnabled
    implements ExecuteStreamHandler, ExecOutputHandler
{
    public void setProcessInputStream( OutputStream os )
        throws IOException
    {
    }

    public void setProcessErrorStream( InputStream is )
        throws IOException
    {
    }

    public void setProcessOutputStream( InputStream is )
        throws TaskException, IOException
    {
    }

    public void start()
        throws IOException
    {
    }

    /**
     * this is where the XML output will go, should mostly be a file the caller
     * is responsible for flushing and closing this stream
     */
    private OutputStream m_xmlOut;

    /**
     * the multimap. The key in the map is the filepath that caused the audit
     * error and the value is a vector of MAudit.Violation entries.
     */
    private Hashtable m_auditedFiles = new Hashtable();

    /**
     * matcher that will be used to extract the info from the line
     */
    private RegexpMatcher m_matcher;

    private Hashtable m_fileMapping;

    MAuditStreamHandler( Hashtable fileMapping, OutputStream xmlOut )
        throws TaskException
    {
        m_fileMapping = fileMapping;
        m_xmlOut = xmlOut;
        /**
         * the matcher should be the Oro one. I don't know about the other one
         */
        m_matcher = ( new RegexpMatcherFactory() ).newRegexpMatcher();
        m_matcher.setPattern( MAudit.AUDIT_PATTERN );
    }

    private static final DocumentBuilder getDocumentBuilder()
    {
        try
        {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch( ParserConfigurationException pce )
        {
            throw new ExceptionInInitializerError( pce );
        }
    }

    /**
     * Pretty dangerous business here. It serializes what was extracted from the
     * MAudit output and write it to the output.
     */
    public void stop()
    {
        // serialize the content as XML, move this to another method
        // this is the only code that could be needed to be overrided
        Document doc = getDocumentBuilder().newDocument();
        Element rootElement = doc.createElement( "classes" );
        final Iterator keys = m_auditedFiles.keySet().iterator();
        rootElement.setAttribute( "audited", String.valueOf( m_fileMapping.size() ) );
        rootElement.setAttribute( "reported", String.valueOf( m_auditedFiles.size() ) );
        int errors = 0;
        while( keys.hasNext() )
        {
            String filepath = (String)keys.next();
            ArrayList v = (ArrayList)m_auditedFiles.get( filepath );
            String fullclassname = (String)m_fileMapping.get( filepath );
            if( fullclassname == null )
            {
                getLogger().warn( "Could not find class mapping for " + filepath );
                continue;
            }
            int pos = fullclassname.lastIndexOf( '.' );
            String pkg = ( pos == -1 ) ? "" : fullclassname.substring( 0, pos );
            String clazzname = ( pos == -1 ) ? fullclassname : fullclassname.substring( pos + 1 );
            Element clazz = doc.createElement( "class" );
            clazz.setAttribute( "package", pkg );
            clazz.setAttribute( "name", clazzname );
            clazz.setAttribute( "violations", String.valueOf( v.size() ) );
            errors += v.size();
            for( int i = 0; i < v.size(); i++ )
            {
                Violation violation = (Violation)v.get( i );
                Element error = doc.createElement( "violation" );
                error.setAttribute( "line", String.valueOf( violation.getLine() ) );
                error.setAttribute( "message", violation.getError() );
                clazz.appendChild( error );
            }
            rootElement.appendChild( clazz );
        }
        rootElement.setAttribute( "violations", String.valueOf( errors ) );

        // now write it to the outputstream, not very nice code
        if( m_xmlOut != null )
        {
            Writer wri = null;
            try
            {
                wri = new OutputStreamWriter( m_xmlOut, "UTF-8" );
                wri.write( "<?xml version=\"1.0\"?>\n" );
                ( new DOMElementWriter() ).write( rootElement, wri, 0, "  " );
                wri.flush();
            }
            catch( IOException exc )
            {
                getLogger().error( "Unable to write log file" );
            }
            finally
            {
                if( m_xmlOut != System.out && m_xmlOut != System.err )
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
     * add a violation entry for the file
     */
    protected void addViolationEntry( String file, Violation entry )
    {
        ArrayList violations = (ArrayList)m_auditedFiles.get( file );
        if( violations == null )
        {
            // if there is no decl for this file yet, create it.
            violations = new ArrayList();
            m_auditedFiles.put( file, violations );
        }
        violations.add( entry );
    }

    /**
     * Receive notification about the process writing
     * to standard error.
     */
    public void stderr( String line )
    {
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     */
    public void stdout( final String line )
    {
        // we suppose here that there is only one report / line.
        // There will obviouslly be a problem if the message is on several lines...

        final ArrayList matches = getGroups( line );
        if( matches != null )
        {
            final String file = (String)matches.get( 1 );
            final int lineNum = Integer.parseInt( (String)matches.get( 2 ) );
            final String msg = (String)matches.get( 3 );
            final Violation violation = new Violation( msg, lineNum );
            addViolationEntry( file, violation );
        }
        else
        {
            // this doesn't match..report it as info, it could be
            // either the copyright, summary or a multiline message (damn !)
            getLogger().info( line );
        }
    }

    private ArrayList getGroups( final String line )
    {
        try
        {
            return m_matcher.getGroups( line );
        }
        catch( final TaskException te )
        {
            getLogger().error( "Failed to process matcher", te );
            return new ArrayList();
        }
    }
}
