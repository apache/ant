/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

/**
 * A role definition.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
class RoleDefinition
{
    private final String m_roleName;
    private final String m_shortHand;

    public RoleDefinition( final String roleName,
                           final String shortHand )
    {
        m_roleName = roleName;
        m_shortHand = shortHand;
    }

    public String getRoleName()
    {
        return m_roleName;
    }

    public String getShortHand()
    {
        return m_shortHand;
    }
}
