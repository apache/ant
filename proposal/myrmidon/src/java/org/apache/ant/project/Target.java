/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.util.Iterator;

public interface Target
{
    Iterator getDependencies();
    Iterator getTasks();
    String getCondition();
    boolean isIfCondition();
}


