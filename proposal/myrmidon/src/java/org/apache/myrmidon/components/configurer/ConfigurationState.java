/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

/**
 * A marker interface that represents the state of an object while it is being
 * configured.
 *
 * @author Adam Murdoch
 * @version $Revision$ $Date$
 */
interface ConfigurationState
{
    /**
     * Returns the configurer being used to configure the object.
     */
    ObjectConfigurer getConfigurer();
}
