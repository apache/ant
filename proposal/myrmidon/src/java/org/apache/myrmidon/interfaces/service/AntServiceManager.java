/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.service;

/**
 * Manages a set of services.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface AntServiceManager
{
    String ROLE = AntServiceManager.class.getName();

    /**
     * Determines if this service manager contains a particular service.
     *
     * @param serviceType The service interface.
     */
    boolean hasService( Class serviceType );

    /**
     * Locates a service instance.
     *
     * @param serviceType The service interface.
     * @return The service instance.  The returned object is guaranteed to
     *         implement the service interface.
     * @throws AntServiceException If the service does not exist.
     */
    Object getService( Class serviceType )
        throws AntServiceException;
}
