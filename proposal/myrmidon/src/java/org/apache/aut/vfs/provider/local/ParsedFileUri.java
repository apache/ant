/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.local;

import org.apache.aut.vfs.provider.ParsedUri;

/**
 * A parsed file URI.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class ParsedFileUri extends ParsedUri
{
    private String m_rootFile;

    public String getRootFile()
    {
        return m_rootFile;
    }

    public void setRootFile( final String rootPrefix )
    {
        m_rootFile = rootPrefix;
    }
}
