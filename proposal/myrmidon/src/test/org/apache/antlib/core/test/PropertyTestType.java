/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core.test;

import org.apache.myrmidon.framework.DataType;

/**
 * A test data-type used by the property tests.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:data-type name="property-test-type"
 */
public class PropertyTestType
    implements DataType
{
    private String m_value;

    public void setValue( final String value )
    {
        m_value = value;
    }

    /**
     * Used in the test project file to check the value has been set.
     */
    public String toString()
    {
        return "value=[" + m_value + "]";
    }
}
