/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.runtime;

import org.apache.myrmidon.framework.conditions.Condition;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.role.RoleInfo;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A condition that evaluates to true if a particular type is available.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="condition" name="type-available"
 */
public class TypeAvailableCondition
    implements Condition
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( TypeAvailableCondition.class );

    private String m_roleShorthand;
    private String m_name;

    /**
     * Sets the role to search for.
     */
    public void setType( final String type )
    {
        m_roleShorthand = type;
    }

    /**
     * Sets the type to search for.
     */
    public void setName( final String name )
    {
        m_name = name;
    }

    /**
     * Evaluates this condition.
     *
     * @param context
     *      The context to evaluate the condition in.
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        if( m_name == null )
        {
            final String message = REZ.getString( "typeavailable.no-type-name.error" );
            throw new TaskException( message );
        }

        try
        {
            // Map the shorthand name to a role
            final String roleName;
            if( m_roleShorthand != null )
            {
                final RoleManager roleManager = (RoleManager)context.getService( RoleManager.class );
                final RoleInfo roleInfo = roleManager.getRoleByShorthandName( m_roleShorthand );
                if( roleInfo == null )
                {
                    final String message = REZ.getString( "typeavailable.unknown-role.error", m_roleShorthand );
                    throw new TaskException( message );
                }
                roleName = roleInfo.getName();
            }
            else
            {
                roleName = DataType.ROLE;
            }

            // Lookup the type
            final TypeManager typeManager = (TypeManager)context.getService( TypeManager.class );
            final TypeFactory typeFactory = typeManager.getFactory( roleName );

            // Check if the type is available
            return typeFactory.canCreate( m_name );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "typeavailable.evaluate.error", m_name );
            throw new TaskException( message, e );
        }
    }
}
