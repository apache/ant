/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.types.EnumeratedAttribute;

public class FileDir
    extends EnumeratedAttribute
{
    private final static String[] values = {"file", "dir"};

    public String[] getValues()
    {
        return values;
    }

    public boolean isDir()
    {
        return "dir".equalsIgnoreCase( getValue() );
    }

    public boolean isFile()
    {
        return "file".equalsIgnoreCase( getValue() );
    }

    public String toString()
    {
        return getValue();
    }
}
