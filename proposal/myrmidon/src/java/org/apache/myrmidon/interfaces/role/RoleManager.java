/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.role;

/**
 * Interface to manage roles.
 *
 * @author <a href="mailto:bloritsch@apache.org">Berin Loritsch</a>
 * @author <a href="mailto:ricardo@apache,org">Ricardo Rocha</a>
 * @author <a href="mailto:giacomo@apache,org">Giacomo Pati</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version CVS $Revision$ $Date$
 */
public interface RoleManager
{
    String ROLE = RoleManager.class.getName();

    /**
     * Find role based on shorthand name.
     *
     * @param name the shorthand name
     * @return the role, or null if the role cannot be found.
     */
    RoleInfo getRoleByShorthandName( String name );

    /**
     * Find role based on role type.
     *
     * @param type the role type.
     * @return the role, or null if the role cannot be found.
     */
    RoleInfo getRoleByType( Class type );

    /**
     * Find role based on name.
     *
     * @param name the role name
     * @return the role, or null if the role cannot be found.
     */
    RoleInfo getRole( String name );

    /**
     * Adds a role definition.
     */
    void addRole( RoleInfo role ) throws RoleException;
}
