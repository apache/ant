/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.extensions;

import java.util.ArrayList;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.DataType;

/**
 * The Extension set lists a set of "Optional Packages" /
 * "Extensions".
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant.data-type name="extension-set"
 */
public class ExtensionSet
    implements DataType
{
    /**
     * ExtensionAdapter objects representing extensions.
     */
    private final ArrayList m_extensions = new ArrayList();

    /**
     * Filesets specifying all the extensions wanted.
     */
    private final ArrayList m_extensionsFilesets = new ArrayList();

    /**
     * Adds an extension that this library requires.
     *
     * @param extensionAdapter an extension that this library requires.
     */
    public void addExtension( final ExtensionAdapter extensionAdapter )
    {
        m_extensions.add( extensionAdapter );
    }

    /**
     * Adds a set of files about which extensions data will be extracted.
     *
     * @param fileSet a set of files about which extensions data will be extracted.
     */
    public void addLibfileset( final LibFileSet fileSet )
    {
        m_extensionsFilesets.add( fileSet );
    }

    /**
     * Extract a set of Extension objects from the ExtensionSet.
     *
     * @throws TaskException if an error occurs
     */
    public Extension[] toExtensions()
        throws TaskException
    {
        final ArrayList extensions = ExtensionUtil.toExtensions( m_extensions );
        ExtensionUtil.extractExtensions( extensions, m_extensionsFilesets );
        return (Extension[])extensions.toArray( new Extension[ extensions.size() ] );
    }

}
