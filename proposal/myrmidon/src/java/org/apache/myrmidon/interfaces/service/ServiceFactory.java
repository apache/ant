/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.service;

/**
 * A ServiceFactory is used to create a service for use in the
 * Myrmidon runtime. The factory is responsible for creating and
 * preparing the service for use.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 *
 * @ant:role shorthand="service-factory"
 */
public interface ServiceFactory
{
    /** Role name for this interface. */
    String ROLE = ServiceFactory.class.getName();

    /**
     * Create a service that corresponds to this factory.
     * This method is usually called after the factory has been
     * prepared and configured as appropriate.
     * @return The created service.
     * @throws AntServiceException If the service could not be created.
     */
    Object createService()
        throws AntServiceException;
}
