/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.util.Iterator;
import org.apache.ant.util.Condition;
import org.apache.avalon.Component;

/**
 * Interface to represent targets in build file.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface Target
    extends Component
{
    /**
     * Get dependencies of target
     *
     * @return the dependency list
     */
    Iterator getDependencies();

    /**
     * Get tasks in target
     *
     * @return the target list
     */
    Iterator getTasks();

    /**
     * Get condition under which target is executed.
     *
     * @return the condition for target or null
     */
    Condition getCondition();
}


