/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;

/**
 * A ZipFileSet is a FileSet with extra attributes useful in the context of
 * Zip/Jar tasks. A ZipFileSet extends FileSets with the ability to extract a
 * subset of the entries of a Zip file for inclusion in another Zip file. It
 * also includes a prefix attribute which is prepended to each entry in the
 * output Zip file. At present, ZipFileSets are not surfaced in the public API.
 * FileSets nested in a Zip task are instantiated as ZipFileSets, and their
 * attributes are only recognized in the context of the the Zip task. It is not
 * possible to define a ZipFileSet outside of the Zip task and refer to it via a
 * refid. However a standard FileSet may be included by reference in the Zip
 * task, and attributes in the refering ZipFileSet can augment FileSet
 * definition.
 *
 * @author <a href="mailto:don@bea.com">Don Ferguson</a>
 */
public class ZipFileSet
    extends FileSet
{
    private File m_src;
    private String m_prefix = "";
    private String m_fullpath = "";

    /**
     * Set the full pathname of the single entry in this fileset.
     *
     * @param fullpath The new Fullpath value
     */
    public void setFullpath( final String fullpath )
    {
        m_fullpath = fullpath;
    }

    /**
     * Prepend this prefix to the path for each zip entry. Does not perform
     * reference test; the referenced file set can be augmented with a prefix.
     *
     * @param prefix The prefix to prepend to entries in the zip file.
     */
    public void setPrefix( final String prefix )
    {
        m_prefix = prefix;
    }

    /**
     * Set the source Zip file for the zipfileset. Prevents both "dir" and "src"
     * from being specified.
     *
     * @param srcFile The zip file from which to extract entries.
     */
    public void setSrc( final File src )
    {
        m_src = src;
    }

    /**
     * Return the full pathname of the single entry in this fileset.
     *
     * @return The Fullpath value
     */
    public String getFullpath()
    {
        return m_fullpath;
    }

    /**
     * Return the prefix prepended to entries in the zip file.
     *
     * @return The Prefix value
     */
    public String getPrefix()
    {
        return m_prefix;
    }

    /**
     * Get the zip file from which entries will be extracted. References are not
     * followed, since it is not possible to have a reference to a ZipFileSet,
     * only to a FileSet.
     *
     * @return The Src value
     */
    public File getSrc()
    {
        return m_src;
    }
}
