/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.cvslib;

import org.apache.myrmidon.api.TaskException;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.i18n.ResourceManager;

/**
 * Represents a CVS user with a userID and a full name.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @version $Revision$ $Date$
 */
public class CvsUser
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( CvsUser.class );

    private String m_userID;
    private String m_displayName;

    public void setDisplayname( final String displayName )
    {
        m_displayName = displayName;
    }

    public void setUserid( final String userID )
    {
        m_userID = userID;
    }

    String getUserID()
    {
        return m_userID;
    }

    String getDisplayname()
    {
        return m_displayName;
    }

    void validate()
        throws TaskException
    {
        if( null == m_userID )
        {
            final String message = REZ.getString( "changelog.nouserid.error" );
            throw new TaskException( message );
        }
        if( null == m_displayName )
        {
            final String message =
                REZ.getString( "changelog.nodisplayname.error", m_userID );
            throw new TaskException( message );
        }
    }
}
