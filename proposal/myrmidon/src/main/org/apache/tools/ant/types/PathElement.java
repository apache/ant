/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import org.apache.tools.ant.util.FileUtils;
import org.apache.avalon.framework.logger.Logger;

/**
 * Helper class, holds the nested <code>&lt;pathelement&gt;</code> values.
 */
public class PathElement
{
    private String m_path;

    public void setLocation( final File location )
    {
        m_path = FileUtils.translateFile( location.getAbsolutePath() );
    }

    public void setPath( String path )
    {
        m_path = path;
    }

    protected String[] getParts( final File baseDirectory, final Logger logger )
    {
        return FileUtils.translatePath( baseDirectory, m_path, logger );
    }
}
