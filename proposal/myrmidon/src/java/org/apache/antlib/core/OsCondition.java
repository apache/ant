/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.aut.nativelib.Os;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.conditions.Condition;

/**
 * Condition to check the current OS.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 *
 * @ant.type type="condition" name="os"
 */
public class OsCondition
    implements Condition
{
    private String m_family;
    private String m_name;
    private String m_version;
    private String m_arch;

    /**
     * Sets the desired OS family type
     *
     * @param family The OS family type desired.
     */
    public void setFamily( final String family )
    {
        m_family = family;
    }

    /**
     * Sets the desired OS name
     *
     * @param name   The OS name
     */
    public void setName( final String name )
    {
        m_name = name;
    }

    /**
     * Sets the desired OS architecture
     *
     * @param arch   The OS architecture
     */
    public void setArch( final String arch )
    {
        m_arch = arch;
    }

    /**
     * Sets the desired OS version
     *
     * @param version   The OS version
     */
    public void setVersion( final String version )
    {
        m_version = version;
    }

    /**
     * Evaluates this condition.
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        return Os.isOs( m_family, m_name, m_arch, m_version );
    }
}
