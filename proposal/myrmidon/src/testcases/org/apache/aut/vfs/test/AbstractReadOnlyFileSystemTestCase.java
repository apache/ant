/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.test;

import org.apache.aut.vfs.test.AbstractFileSystemTestCase;

/**
 * File system tests which check that a read-only file system cannot be
 * changed.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public abstract class AbstractReadOnlyFileSystemTestCase extends AbstractFileSystemTestCase
{
    public AbstractReadOnlyFileSystemTestCase( String name )
    {
        super( name );
    }
}
