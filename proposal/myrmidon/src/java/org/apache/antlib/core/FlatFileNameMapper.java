/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.FileNameMapper;

/**
 * Implementation of FileNameMapper that always returns the source file name
 * without any leading directory information. <p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 *
 * @ant:type type="mapper" name="flatten"
 */
public class FlatFileNameMapper
    extends PrefixFileNameMapper
    implements FileNameMapper
{

    /**
     * Returns an one-element array containing the source file name without any
     * leading directory information.
     *
     * @param sourceFileName Description of Parameter
     * @return Description of the Returned Value
     */
    public String[] mapFileName( final String sourceFileName, TaskContext context )
        throws TaskException
    {
        final String baseName = FileUtil.removePath( sourceFileName, '/' );
        return super.mapFileName( baseName, context );
    }
}
