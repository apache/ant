/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.basic;

import java.io.File;
import org.apache.ant.convert.AbstractConverter;
import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.api.TaskContext;

/**
 * String to file converter
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class StringToFileConverter
    extends AbstractConverter
{
    public StringToFileConverter()
    {
        super( String.class, File.class );
    }

    public Object convert( final Object original, final Context context )
        throws Exception
    {
        final TaskContext taskContext = (TaskContext)context;
        return taskContext.resolveFile( (String)original );
    }
}

