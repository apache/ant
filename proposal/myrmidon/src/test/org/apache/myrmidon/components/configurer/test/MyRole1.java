/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.test;

import org.apache.myrmidon.framework.DataType;

/**
 * A basic interface to test configurer.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface MyRole1
    extends DataType
{
    String ROLE = MyRole1.class.getName();
}
