/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.junit;

import java.io.File;
import java.util.ArrayList;

/**
 * Baseclass for BatchTest and JUnitTest.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public abstract class BaseTest
{
    protected boolean m_haltOnError = false;
    protected boolean m_haltOnFail = false;
    protected boolean m_filtertrace = true;
    protected boolean fork = false;
    protected String ifProperty = null;
    protected String unlessProperty = null;
    protected ArrayList formatters = new ArrayList();
    /**
     * destination directory
     */
    protected File destDir = null;
    protected String errorProperty;

    protected String failureProperty;

    public void setErrorProperty( String errorProperty )
    {
        this.errorProperty = errorProperty;
    }

    public void setFailureProperty( String failureProperty )
    {
        this.failureProperty = failureProperty;
    }

    public void setFiltertrace( boolean value )
    {
        m_filtertrace = value;
    }

    public void setFork( boolean value )
    {
        fork = value;
    }

    public void setHaltonerror( boolean value )
    {
        m_haltOnError = value;
    }

    public void setHaltonfailure( boolean value )
    {
        m_haltOnFail = value;
    }

    public void setIf( String propertyName )
    {
        ifProperty = propertyName;
    }

    /**
     * Sets the destination directory.
     *
     * @param destDir The new Todir value
     */
    public void setTodir( File destDir )
    {
        this.destDir = destDir;
    }

    public void setUnless( String propertyName )
    {
        unlessProperty = propertyName;
    }

    public java.lang.String getErrorProperty()
    {
        return errorProperty;
    }

    public java.lang.String getFailureProperty()
    {
        return failureProperty;
    }

    public boolean getFiltertrace()
    {
        return m_filtertrace;
    }

    public boolean getFork()
    {
        return fork;
    }

    public boolean getHaltonerror()
    {
        return m_haltOnError;
    }

    public boolean getHaltonfailure()
    {
        return m_haltOnFail;
    }

    /**
     * @return the destination directory as an absolute path if it exists
     *      otherwise return <tt>null</tt>
     */
    public String getTodir()
    {
        if( destDir != null )
        {
            return destDir.getAbsolutePath();
        }
        return null;
    }

    public void addFormatter( FormatterElement elem )
    {
        formatters.add( elem );
    }
}
