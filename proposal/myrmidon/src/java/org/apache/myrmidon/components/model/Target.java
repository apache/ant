/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.model;

import org.apache.ant.util.Condition;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.configuration.Configuration;

/**
 * Interface to represent targets in build file.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface Target
    extends Component
{
    String ROLE = "org.apache.myrmidon.components.model.Target";

    /**
     * Get dependencies of target
     *
     * @return the dependency list
     */
    String[] getDependencies();

    /**
     * Get tasks in target
     *
     * @return the target list
     */
    Configuration[] getTasks();

    /**
     * Get condition under which target is executed.
     *
     * @return the condition for target or null
     */
    Condition getCondition();
}


