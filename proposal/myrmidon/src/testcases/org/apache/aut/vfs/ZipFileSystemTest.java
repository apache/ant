/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

import java.io.File;

/**
 * Tests for the Zip file system.
 *
 * @author Adam Murdoch
 */
public class ZipFileSystemTest extends ReadOnlyFileSystemTestBase
{
    public ZipFileSystemTest( String name )
    {
        super( name );
    }

    /**
     * Returns the URI for the base folder.
     */
    protected String getBaseFolderURI()
    {
        String zipFileName = System.getProperty( "test.zip.file" );
        String zipFile = new File( zipFileName ).getAbsolutePath();
        String uri = "zip:" + zipFile + "!basedir";
        return uri;
    }
}
