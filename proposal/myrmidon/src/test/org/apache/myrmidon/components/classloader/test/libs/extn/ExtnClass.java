/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.classloader.test.libs.extn;

import org.apache.myrmidon.components.classloader.test.libs.shared.SharedClass;

/**
 * A test class loaded from an extension.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class ExtnClass
{
    public SharedClass m_test = new SharedClass();
}
