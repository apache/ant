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
import org.apache.myrmidon.components.service.test.LifecycleValidator;

/**
 * A test service factory, which asserts that the factory has been properly
 * set-up before it is used.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class TestServiceFactory2
    extends LifecycleValidator
    implements ServiceFactory
{
    /**
     * Create a service that corresponds to this factory.
     */
    public Object createService()
        throws AntServiceException
    {
        assertSetup();
        return new TestServiceImpl2();
    }
}
