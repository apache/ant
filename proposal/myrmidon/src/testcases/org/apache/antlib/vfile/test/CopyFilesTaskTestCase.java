/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile.test;

import java.io.File;
import org.apache.myrmidon.AbstractProjectTest;

/**
 * Test cases for the <v-copy> task.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class CopyFilesTaskTestCase
    extends AbstractProjectTest
{
    public CopyFilesTaskTestCase( String name )
    {
        super( name );
    }

    /**
     * A simple smoke test.
     */
    public void testCopy() throws Exception
    {
        final File projectFile = getTestResource( "copy.ant" );
        executeTarget( projectFile, "copy" );
    }
}
