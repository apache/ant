/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.service.test;

import org.apache.myrmidon.interfaces.service.AntServiceException;
import org.apache.myrmidon.interfaces.service.ServiceFactory;

/**
 * A test service factory.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class TestServiceFactory1
    implements ServiceFactory
{
    /**
     * Create a service that coresponds to this factory.
     */
    public Object createService()
        throws AntServiceException
    {
        return new TestServiceImpl1();
    }
}
