/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import java.util.ArrayList;
import java.util.List;

/**
 * A typelib role descriptor, which defines a set of roles.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class RoleDescriptor
    extends TypelibDescriptor
{
    private final List m_definitions = new ArrayList();

    public RoleDescriptor( final String url )
    {
        super( url );
    }

    /**
     * Returns the role definitions in the descriptor.
     */
    public RoleDefinition[] getDefinitions()
    {
        return (RoleDefinition[])m_definitions.toArray
            ( new RoleDefinition[ m_definitions.size() ] );
    }

    /**
     * Adds a role definition to the descriptor.
     */
    public void addDefinition( final RoleDefinition def )
    {
        m_definitions.add( def );
    }
}
