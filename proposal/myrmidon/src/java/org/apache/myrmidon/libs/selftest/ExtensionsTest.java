/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.libs.selftest;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.libs.selftest.extension1.ExtensionsLoadedClass;

/**
 * This is to test whether extension is loaded.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class ExtensionsTest
    extends AbstractTask
{
    public void execute()
        throws TaskException
    {
        ExtensionsLoadedClass.doSomething();
    }
}
