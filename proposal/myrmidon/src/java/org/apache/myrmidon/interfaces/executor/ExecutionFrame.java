/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.executor;

import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.myrmidon.interfaces.property.PropertyStore;

/**
 * An Execution Frame represents the scope in which tasks are executed.
 * The scope may include an entire workspace, a project, target, or
 * individual task.
 *
 * <p>An Execution Frame bundles together all of the context required to
 * execute tasks - that is, a set of properties, a set of services, and
 * a logger.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface ExecutionFrame
{
    /** Role name for this interface. */
    String ROLE = ExecutionFrame.class.getName();

    /**
     * Returns the set of services to use to create, configure, and execute
     * tasks.
     */
    ServiceManager getServiceManager();

    /**
     * Returns the logger which is to be supplied to tasks.
     */
    Logger getLogger();

    /**
     * Returns the set of properties to be supplied to tasks.
     */
    PropertyStore getProperties();
}
