/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;

/**
 * A converter from {@link FileSet} to {@link FileList}.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:converter source="org.apache.antlib.vfile.FileSet"
 *                destination="org.apache.antlib.vfile.FileList"
 */
public class FileSetToFileListConverter
    extends AbstractConverter
{
    public FileSetToFileListConverter()
    {
        super( FileSet.class, FileList.class );
    }

    /**
     * Do the conversion.
     */
    protected Object convert( final Object original, final Object context )
        throws ConverterException
    {
        final FileSet src = (FileSet)original;
        return new FileSetAdaptor( src );
    }
}
