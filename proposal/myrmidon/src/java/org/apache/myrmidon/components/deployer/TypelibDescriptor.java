/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

/**
 * A descriptor from a typelib.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class TypelibDescriptor
{
    private final String m_url;

    protected TypelibDescriptor( final String url )
    {
        m_url = url;
    }

    /**
     * Returns the descriptor URL.
     */
    public String getUrl()
    {
        return m_url;
    }
}
