/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.io.File;
import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * String to file converter
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.converter source="java.lang.String" destination="java.io.File"
 */
public class StringToFileConverter
    extends AbstractConverter
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( StringToFileConverter.class );

    public StringToFileConverter()
    {
        super( String.class, File.class );
    }

    public Object convert( final Object object, final Object context )
        throws ConverterException
    {
        try
        {
            final TaskContext taskContext = (TaskContext)context;
            return taskContext.resolveFile( (String)object );
        }
        catch( final TaskException te )
        {
            final String message = REZ.getString( "convert.bad-file.error", object );
            throw new ConverterException( message, te );
        }
    }
}

