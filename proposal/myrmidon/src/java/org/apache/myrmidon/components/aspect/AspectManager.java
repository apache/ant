/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.aspect;

import org.apache.myrmidon.aspects.AspectHandler;
import org.apache.avalon.framework.component.Component;

/**
 * Manage and propogate Aspects.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface AspectManager
    extends Component, AspectHandler
{
    String ROLE = "org.apache.myrmidon.components.aspect.AspectManager";

    void addAspectHandler( AspectHandler handler );
    void removeAspectHandler( AspectHandler handler );
}
