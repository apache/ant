/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.type;

import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;

/**
 * Create a component based on role and hint.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public interface ComponentFactory
{
    /**
     * Create a Component with appropriate name.
     *
     * @param name the name
     * @return the created component
     * @exception ComponentException if an error occurs
     */
    Component create( String name )
        throws ComponentException;
}
