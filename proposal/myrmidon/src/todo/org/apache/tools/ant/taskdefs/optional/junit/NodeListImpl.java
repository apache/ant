/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.ArrayList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * custom implementation of a nodelist
 */
public class NodeListImpl
    extends ArrayList
    implements NodeList
{
    public int getLength()
    {
        return size();
    }

    public Node item( final int i )
    {
        try
        {
            return (Node)get( i );
        }
        catch( final ArrayIndexOutOfBoundsException aioobe )
        {
            return null;// conforming to NodeList interface
        }
    }
}
