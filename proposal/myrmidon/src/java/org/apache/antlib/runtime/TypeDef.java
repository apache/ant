/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.runtime;

import org.apache.myrmidon.framework.AbstractTypeDef;

/**
 * Task to define a type.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class TypeDef
    extends AbstractTypeDef
{
    private String m_type;

    public void setType( final String type )
    {
        m_type = type;
    }

    protected String getRoleShorthand()
    {
        return m_type;
    }
}
