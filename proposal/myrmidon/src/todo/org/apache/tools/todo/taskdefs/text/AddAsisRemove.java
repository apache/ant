/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.text;

import org.apache.tools.todo.types.EnumeratedAttribute;

/**
 * Enumerated attribute with the values "asis", "add" and "remove".
 */
public class AddAsisRemove
    extends EnumeratedAttribute
{
    public String[] getValues()
    {
        return new String[]{"add", "asis", "remove"};
    }
}
