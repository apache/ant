/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.sitraka;

import org.apache.tools.ant.types.EnumeratedAttribute;

public class Recordfromstart
    extends EnumeratedAttribute
{
    public String[] getValues()
    {
        return new String[]{"coverage", "none", "all"};
    }
}
