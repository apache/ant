/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import org.apache.ant.convert.ConverterFactory;
import org.apache.ant.convert.DefaultConverterEngine;

public class TaskletConverterEngine
    extends DefaultConverterEngine
{
    /**
     * Set the ConverterFactory.
     * Package access intended.
     */
    void setConverterFactory( final ConverterFactory converterFactory )
    {
        m_converterFactory = converterFactory;
    }

    protected ConverterFactory createConverterFactory()
    {
        return m_converterFactory;
    }
}
