/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.javadoc;

import org.apache.tools.todo.types.EnumeratedAttribute;

public class AccessType
    extends EnumeratedAttribute
{
    public String[] getValues()
    {
        // Protected first so if any GUI tool offers a default
        // based on enum #0, it will be right.
        return new String[]{"protected", "public", "package", "private"};
    }
}
