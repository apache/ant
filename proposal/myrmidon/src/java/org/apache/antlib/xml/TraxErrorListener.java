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
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

final class TraxErrorListener
    extends AbstractLogEnabled
    implements ErrorListener
{
    private final boolean m_warn;
    private boolean m_failed;

    protected TraxErrorListener( final boolean warn )
    {
        m_warn = warn;
    }

    public void error( final TransformerException te )
    {
        m_failed = true;
        getLogger().error( getMessage( te ), te );
    }

    public void fatalError( final TransformerException te )
    {
        m_failed = true;
        getLogger().error( getMessage( te ), te );
    }

    public void warning( final TransformerException te )
    {
        // depending on implementation, XMLReader can yield hips of warning,
        // only output then if user explicitely asked for it
        if( m_warn )
        {
            getLogger().warn( getMessage( te ), te );
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

    private String getMessage( final TransformerException te )
    {
        final SourceLocator locator = te.getLocator();
        final String systemID = locator.getSystemId();
        if( null != systemID )
        {
            final int line = locator.getLineNumber();
            final int column = locator.getColumnNumber();

            try
            {
                //Build a message using standard compiler
                //error format
                return new URL( systemID ).getFile() +
                    ( line == -1 ? "" : ( ":" + line +
                    ( column == -1 ? "" : ( ":" + column ) ) ) ) +
                    ": " + te.getMessage();
            }
            catch( final MalformedURLException mue )
            {
            }
        }
        return te.getMessage();
    }
}
