/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types.converters;

import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.FileList;
import org.apache.tools.todo.types.PathUtil;

/**
 * Converters from FileList to String.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:converter source="org.apache.tools.todo.types.FileList" destination="java.lang.String"
 */
public class FileListToStringConverter
    extends AbstractConverter
{
    public FileListToStringConverter()
    {
        super( FileList.class, String.class );
    }

    protected Object convert( final Object original, final Object context )
        throws ConverterException
    {
        try
        {
            final TaskContext taskContext = (TaskContext)context;
            final FileList fileList = (FileList)original;
            final String[] files = fileList.listFiles( taskContext );
            return PathUtil.formatPath( files );
        }
        catch( final TaskException e )
        {
            throw new ConverterException( e.getMessage(), e );
        }
    }
}
