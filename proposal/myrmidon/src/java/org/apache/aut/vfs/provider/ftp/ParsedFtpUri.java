/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.ftp;

import org.apache.aut.vfs.provider.ParsedUri;

/**
 * A parsed FTP URI.
 *
 * @author Adam Murdoch
 */
public class ParsedFtpUri extends ParsedUri
{
    private String m_userName;
    private String m_password;

    public String getUserName()
    {
        return m_userName;
    }

    public void setUserName( String userName )
    {
        m_userName = userName;
    }

    public String getPassword()
    {
        return m_password;
    }

    public void setPassword( String password )
    {
        m_password = password;
    }
}
