/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Enumerated attribute with the values "file", "dir" and "both" for the
 * type attribute.
 */
public class FileDirBoth
    extends EnumeratedAttribute
{
    public String[] getValues()
    {
        return new String[]{"file", "dir", "both"};
    }
}
