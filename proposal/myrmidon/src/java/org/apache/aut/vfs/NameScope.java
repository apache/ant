/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * An enumerated type for file name scope, used when resolving a name relative
 * to a file.
 *
 * @author Adam Murdoch
 */
public final class NameScope
{
    /**
     * Resolve against the children of the base file.
     *
     * <p>The supplied name must be a valid element name.  That is, it may
     * not be empty, or <code>.</code>, or <code>..</code>, or contain any
     * separator characters.
     */
    public static final NameScope CHILD = new NameScope( "child" );

    /**
     * Resolve against files in the same file system as the base file.
     *
     * <p>If the supplied name is an absolute path, then it is resolved
     * relative to the root of the file system that the base file belongs to.
     * If a relative name is supplied, then it is resolved relative to the base
     * file.
     *
     * <p>The path may use any mix of <code>/</code>, <code>\</code>, or file
     * system specific separators to separate elements in the path.  It may
     * also contain <code>.</code> and <code>..</code> elements.
     *
     * <p>A path is considered absolute if it starts with a separator character,
     * and relative if it does not.
     */
    public static final NameScope FILE_SYSTEM = new NameScope( "filesystem" );

    private String m_name;

    private NameScope( final String name )
    {
        m_name = name;
    }

    /** Returns the name of the scope. */
    public String toString()
    {
        return m_name;
    }

    /** Returns the name of the scope. */
    public String getName()
    {
        return m_name;
    }
}
