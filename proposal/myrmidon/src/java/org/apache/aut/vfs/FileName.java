/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * The interface is used to perform operations on a file name.  File names
 * are immutable, and work correctly as keys in hash tables.
 *
 * @see FileObject
 *
 * @author Adam Murdoch
 */
public interface FileName
{
    /**
     * Returns the base name of the file.  The base name of a file is the
     * last element of its name.  For example the base name of
     * <code>/somefolder/somefile</code> is <code>somefile</code>.
     *
     * <p>The root file of a file system has an empty base name.
     */
    String getBaseName();

    /**
     * Returns the absolute path of the file, within its file system.  This
     * path is normalised, so that <code>.</code> and <code>..</code> elements
     * have been removed.  Also, the path only contains <code>/</code> as its
     * separator character.  The path always starts with <code>/</code>
     *
     * <p>The root of a file system has <code>/</code> as its path.
     */
    String getPath();

    /**
     * Returns the absolute URI of the file.
     */
    String getURI();

    /**
     * Returns the name of the parent of the file.  The root of a file system
     * has no parent.
     *
     * @return
     *      A {@link FileName} object representing the parent name.  Returns
     *      null for the root of a file system.
     */
    FileName getParent();

    /**
     * Resolves a name, relative to the file.  Equivalent to calling
     * <code>resolveName( path, NameScope.FILE_SYSTEM )</code>.
     *
     * @param path
     *      The path to resolve.
     *
     * @return
     *      A {@link FileName} object representing the resolved name.
     *
     * @throws FileSystemException
     *      If the name is invalid.
     */
    FileName resolveName( String path ) throws FileSystemException;

    /**
     * Resolves a name, relative to the file.  Refer to {@link NameScope#CHILD}
     * and {@link NameScope#FILE_SYSTEM} for a description of how names are
     * resolved.
     *
     * @param name
     *      The path to resolve.
     *
     * @param scope
     *      The scope to use when resolving the name.
     *
     * @return
     *      A {@link FileName} object representing the resolved name.
     *
     * @throws FileSystemException
     *      If the name is invalid.
     */
    FileName resolveName( String name, NameScope scope ) throws FileSystemException;
}
