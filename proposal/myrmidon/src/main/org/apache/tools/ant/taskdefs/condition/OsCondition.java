/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.condition;

import java.util.Locale;
import org.apache.aut.nativelib.Os;
import org.apache.myrmidon.api.TaskException;

/**
 * Condition to check the current OS.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class OsCondition
    extends ConditionBase
    implements Condition
{
    private String m_family;
    private String m_name;
    private String m_version;
    private String m_arch;

    /**
     * Sets the desired OS family type
     *
     * @param f      The OS family type desired<br />
     *               Possible values:<br />
     *               <ul><li>dos</li>
     *               <li>mac</li>
     *               <li>netware</li>
     *               <li>os/2</li>
     *               <li>unix</li>
     *               <li>windows</li></ul>
     */
    public void setFamily( final String family )
    {
        m_family = family.toLowerCase( Locale.US );
    }

    /**
     * Sets the desired OS name
     *
     * @param name   The OS name
     */
    public void setName( final String name )
    {
        m_name = name.toLowerCase( Locale.US );
    }

    /**
     * Sets the desired OS architecture
     *
     * @param arch   The OS architecture
     */
    public void setArch( final String arch )
    {
        m_arch = arch.toLowerCase( Locale.US );
    }

    /**
     * Sets the desired OS version
     *
     * @param version   The OS version
     */
    public void setVersion( String version )
    {
        this.m_version = version.toLowerCase( Locale.US );
    }

    /**
     * Determines if the OS on which Ant is executing matches the type of
     * that set in setFamily.
     * @see Os#setFamily(String)
     */
    public boolean eval()
        throws TaskException
    {
        return Os.isOs( m_family, m_name, m_arch, m_version );
    }
}
