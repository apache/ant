/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * A list of possible values for the <code>setAction()</code> method.
 * Possible values include: start and stop.
 */
public class ActionChoices
    extends EnumeratedAttribute
{
    private final static String[] values = {"start", "stop"};

    public String[] getValues()
    {
        return values;
    }
}
