/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.libs.core;

import java.io.File;
import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.converter.AbstractConverter;
import org.apache.myrmidon.converter.ConverterException;

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
        throws ConverterException
    {
        try
        {
            final TaskContext taskContext = (TaskContext)context;
            return taskContext.resolveFile( (String)original );
        }
        catch( final TaskException te )
        {
            throw new ConverterException( "Error resolving file during conversion", te );
        }
    }
}

