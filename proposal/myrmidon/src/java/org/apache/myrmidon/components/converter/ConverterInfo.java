/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.converter;

/**
 * This info represents meta-information about a converter.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class ConverterInfo
{
    private final String            m_source;
    private final String            m_destination;

    public ConverterInfo( final String source, final String destination )
    {
        m_source = source;
        m_destination = destination;
    } 

    /**
     * Retrieve the source type from which it can convert.
     * NB: Should this be an array ????
     *
     * @return the classname from which object produced
     */
    public String getSource()
    {
        return m_source;
    }

    /**
     * Retrieve the type to which the converter converts.
     * NB: Should this be an array ????
     *
     * @return the classname of the produced object
     */
    public String getDestination()
    {
        return m_destination;
    }
}

