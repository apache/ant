/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile.selectors;

import org.apache.aut.vfs.FileObject;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskException;

/**
 * A file selector that selects files based on their name.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="v-file-selector" name="name"
 */
public class NameFileSelector
    extends AbstractNameFileSelector
{
    private static final Resources REZ
        = ResourceManager.getPackageResources( NameFileSelector.class );

    /**
     * Returns the name to match against.
     */
    protected String getNameForMatch( final String path,
                                      final FileObject file )
        throws TaskException
    {
        if( path == null )
        {
            final String message = REZ.getString( "namefileselector.no-path.error" );
            throw new TaskException( message );
        }
        return path;
    }
}
