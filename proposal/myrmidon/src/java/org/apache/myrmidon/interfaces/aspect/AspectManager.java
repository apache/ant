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
    String ROLE = AspectManager.class.getName();

    String[] getNames();

    void dispatchAspectSettings( String name, Parameters parameters, Configuration[] elements )
        throws TaskException;

    void addAspectHandler( String name, AspectHandler handler )
        throws TaskException;

    void removeAspectHandler( String name, AspectHandler handler )
        throws TaskException;
}
