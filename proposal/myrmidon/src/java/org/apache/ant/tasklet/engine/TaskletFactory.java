/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import org.apache.avalon.camelot.Factory;
import org.apache.avalon.camelot.FactoryException;
import org.apache.ant.tasklet.Tasklet;

/**
 * Facility used to load Tasklets.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TaskletFactory
    extends Factory
{
    Tasklet createTasklet( TaskletInfo info ) 
        throws FactoryException;
}
