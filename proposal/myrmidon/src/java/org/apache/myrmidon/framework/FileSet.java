/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.io.File;

/**
 * A FileSet represents a set of files selected by patterns with a
 * specified root.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 *
 * @ant:data-type name="fileset"
 */
public class FileSet
    extends AbstractFileSet
{
    private File m_dir;

    /**
     * Specify the base directory at which the file set is rooted.
     */
    public final void setDir( File dir )
    {
        m_dir = dir;
    }

    public final File getDir()
    {
        return m_dir;
    }
}
