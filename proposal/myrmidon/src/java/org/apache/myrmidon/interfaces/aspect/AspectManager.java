/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.aspect;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.aspects.AspectHandler;

/**
 * Manage and propogate Aspects..
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface AspectManager
    extends AspectHandler
{
    /** Role name for this interface. */
    String ROLE = AspectManager.class.getName();

    /**
     * @return The names of all AspectHandlers managed.
     */
    String[] getNames();

    /**
     * Dispatches aspect settings to the named AspectHandler.
     * @param name The name of the AspectHandler to recieve the settings.
     * @param parameters The parameter settings.
     * @param elements The nested Configuration settings.
     * @throws TaskException if the named AspectHandler doesn't exist,
     *                  or it cannot handle the settings.
     */
    void dispatchAspectSettings( String name, Parameters parameters, Configuration[] elements )
        throws TaskException;

    /**
     * Adds a named aspect handler to the manager.
     * @param name The name used to lookup the aspect handler.
     * @param handler The aspect handler to add.
     * @throws TaskException If an error occurs.
     */
    void addAspectHandler( String name, AspectHandler handler )
        throws TaskException;

    /**
     * Removes a named aspect handler from the manager.
     * @param name The name of the handler to remove.
     * @throws TaskException If the named handler doesn't exist.
     */
    void removeAspectHandler( String name )
        throws TaskException;
}
