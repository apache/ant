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
import org.apache.myrmidon.interfaces.deployer.TypeDefinition;

/**
 * A typelib type descriptor, which defines a set of types.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class TypeDescriptor
    extends TypelibDescriptor
{
    private final List m_definitions = new ArrayList();

    public TypeDescriptor( final String url )
    {
        super( url );
    }

    public TypeDefinition[] getDefinitions()
    {
        return (TypeDefinition[])m_definitions.toArray
            ( new TypeDefinition[ m_definitions.size() ] );
    }

    public void addDefinition( final TypeDefinition def )
    {
        m_definitions.add( def );
    }
}
