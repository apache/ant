/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import java.net.URL;

/**
 * This info represents meta-information about a converter.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultConverterInfo
    implements ConverterInfo
{
    protected final String            m_source;
    protected final String            m_destination;
    protected final String            m_classname;
    protected final URL               m_location;

    public DefaultConverterInfo( final String source, 
                                 final String destination,
                                 final String classname,
                                 final URL location )
    {
        m_source = source;
        m_destination = destination;
        m_classname = classname;
        m_location = location;
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
    
    /**
     * Retrieve classname for concerter.
     *
     * @return the taskname
     */
    public String getClassname()
    {
        return m_classname;
    }

    /**
     * Retrieve location of task library where task is contained.
     *
     * @return the location of task library
     */
    public URL getLocation()
    {
        return m_location;
    }
}

