/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.builder;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handler that reacts to PIs before first element.
 * Have to do it this way as there doesn't seem to be a *safe* way
 * of redirecting content handlers at runtime while using transformers.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class ReactorPIHandler
    extends DefaultHandler
{
    private ArrayList    m_targets    = new ArrayList();
    private ArrayList    m_data       = new ArrayList();

    public int getPICount()
    {
        return m_targets.size();
    }

    public String getTarget( final int index )
    {
        return (String)m_targets.get( index );
    }

    public String getData( final int index )
    {
        return (String)m_data.get( index );
    }

    public void processingInstruction( final String target, final String data )
        throws SAXException
    {
        m_targets.add( target );
        m_data.add( data );
    }
    
    public void startElement( final String uri,
                              final String localName,
                              final String qName,
                              final Attributes atts )
        throws SAXException
    {
        //Workaround to stop SAX pipeline
        throw new StopParsingException();
    }
}
