/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemManager;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.converter.AbstractConverter;
import org.apache.myrmidon.converter.ConverterException;

/**
 * Converts a String to a {@link FileObject}
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @ant:converter source="java.lang.String" destination="org.apache.aut.vfs.FileObject"
 */
public class StringToFileObjectConverter
    extends AbstractConverter
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( StringToFileObjectConverter.class );

    public StringToFileObjectConverter()
    {
        super( String.class, FileObject.class );
    }

    /**
     * Converts a String into a FileObject.
     */
    protected Object convert( final Object original, final Object context )
        throws ConverterException
    {
        final String uri = (String)original;
        final TaskContext taskContext = (TaskContext)context;

        try
        {
            final FileSystemManager manager =
                (FileSystemManager)taskContext.getService( FileSystemManager.class );

            // TODO - change TaskContext.getBaseDirectory() to return a FileObject
            return manager.resolveFile( taskContext.getBaseDirectory(), uri );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "bad-convert-string-to-file.error", uri );
            throw new ConverterException( message, e );
        }
    }
}
