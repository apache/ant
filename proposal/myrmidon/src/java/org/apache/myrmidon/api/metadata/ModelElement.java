/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.api.metadata;

/**
 * A ModelElement represents the data necessary to configure
 * the task or sub-object. It usually represents an XML element in a
 * build file and has similar features to XML elements.
 *
 * <p>It has a set of un-ordered attributes with each attribute mapping
 * a key to a value. The ModelElement can also have either a set of ordered
 * sub-elements or text content (one or the other - not both).</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ModelElement
{
    /**
     * Return an array containing all the child <code>ModelElement</code>s
     * that are contained within this <code>ModelElement</code>. If this method
     * returns an array containing 1 or more elements then it must return null
     * for getContent() method.
     *
     * @todo determine whether we return null or an empty array when no
     *       child elements.
     * @return all the child <code>ModelElement</code>s
     * @see #getContent()
     */
    public ModelElement[] getChildren()
    {
        return null;
    }

    /**
     * Return an array containing the names of all the attributes stored
     * in this <code>ModelElement</code>. The user can then pass these
     * parameters into the getAttribute() method of this class to get the
     * value of the attribute.
     *
     * @return an array of the attribute names
     * @see #getAttribute(String)
     */
    public String[] getAttributeNames()
    {
        return null;
    }

    /**
     * Get the value of the attribute passed in.
     * If no such attribute exists return null.
     *
     * @param name the name of the attribute to retrieve value for
     * @return the value of the attribute with specified name or null
     *         if no such element.
     */
    public String getAttribute( final String name )
    {
        return null;
    }

    /**
     * Retrieve the content of this element if any. Will return
     * null if no content available. Note it is invalid for this
     * method to return a non-null value and the getChildren()
     * method to return an array of 1 or more child elements.
     *
     * @return the content value if any, else null
     * @see #getChildren()
     */
    public String getContent()
    {
        return null;
    }
}
