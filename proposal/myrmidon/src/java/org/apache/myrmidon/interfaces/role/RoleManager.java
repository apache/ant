/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.role;

import org.apache.avalon.framework.component.Component;

/**
 * Interface to manage roles and mapping to shorthand names.
 *
 * @author <a href="mailto:bloritsch@apache.org">Berin Loritsch</a>
 * @author <a href="mailto:ricardo@apache,org">Ricardo Rocha</a>
 * @author <a href="mailto:giacomo@apache,org">Giacomo Pati</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public interface RoleManager
    extends Component
{
    String ROLE = RoleManager.class.getName();

    /**
     * Find Role name based on shorthand name.
     *
     * @param shorthandName the shorthand name
     * @return the role
     */
    String getRoleForName( String name );

    /**
     * Find name based on role.
     *
     * @param role the role
     * @return the name
     */
    String getNameForRole( String role );

    /**
     * Adds a role mapping.
     *
     * @param name the shorthand name.
     * @param role the role name.
     */
    void addNameRoleMapping( String name, String role )
        throws IllegalArgumentException;
}
