/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.service.test;

import org.apache.myrmidon.components.service.test.LifecycleValidator;
import org.apache.myrmidon.components.service.test.TestService;

/**
 * A test service that asserts it has been set-up correctly.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class TestServiceImpl2
    extends LifecycleValidator
    implements TestService
{
    public void doWork()
    {
        assertSetup();
    }
}
