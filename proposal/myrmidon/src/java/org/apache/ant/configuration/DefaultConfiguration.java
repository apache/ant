/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.configuration;

import java.util.Iterator;

/**
 * Fork of Avalon code that will be folded back when they get equivelent facilties.
 * Note that the code is different package so it should not cause any issues.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultConfiguration
    extends org.apache.avalon.DefaultConfiguration
    implements Configuration
{
    public DefaultConfiguration( final String localname, final String location )
    {
        super( localname, location );
    }

    /**
     * Retrieve a list of all child names.
     *
     * @return the child names
     */
    public Iterator getChildren()
    {
        if( null == m_children ) return EMPTY_ITERATOR;
        else return m_children.iterator();
    }
    
    /**
     * Retrieve a list of all attribute names.
     *
     * @return the attribute names
     */
    public Iterator getAttributeNames()
    {
        if( null == m_attributes ) return EMPTY_ITERATOR;
        else return m_attributes.keySet().iterator();
    }
}
