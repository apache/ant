/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.xml;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.myrmidon.api.TaskContext;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/*
 * ValidatorErrorHandler role :
 * <ul>
 * <li> log SAX parse exceptions,
 * <li> remember if an error occured
 * </ul>
 */
final class ValidatorErrorHandler
    implements ErrorHandler
{
    private final boolean m_warn;
    private final TaskContext m_context;
    private boolean m_failed;

    protected ValidatorErrorHandler( final boolean warn, final TaskContext context )
    {
        m_warn = warn;
        m_context = context;
    }

    public void error( final SAXParseException spe )
    {
        m_failed = true;
        m_context.error( getMessage( spe ), spe );
    }

    public void fatalError( final SAXParseException spe )
    {
        m_failed = true;
        m_context.error( getMessage( spe ), spe );
    }

    public void warning( final SAXParseException spe )
    {
        // depending on implementation, XMLReader can yield hips of warning,
        // only output then if user explicitely asked for it
        if( m_warn )
        {
            m_context.warn( getMessage( spe ), spe );
        }
    }

    protected void reset()
    {
        m_failed = false;
    }

    // did an error happen during last parsing ?
    protected boolean getFailure()
    {
        return m_failed;
    }

    private String getMessage( final SAXParseException spe )
    {
        final String systemID = spe.getSystemId();
        if( null != systemID )
        {
            final int line = spe.getLineNumber();
            final int col = spe.getColumnNumber();

            try
            {
                //Build a message using standard compiler
                //error format
                return new URL( systemID ).getFile() +
                    ( line == -1 ? "" : ( ":" + line +
                    ( col == -1 ? "" : ( ":" + col ) ) ) ) +
                    ": " + spe.getMessage();
            }
            catch( final MalformedURLException mue )
            {
            }
        }
        return spe.getMessage();
    }
}
