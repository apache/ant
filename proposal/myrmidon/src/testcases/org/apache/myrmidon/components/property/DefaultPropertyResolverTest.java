/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.property;

import org.apache.myrmidon.interfaces.property.PropertyResolver;

/**
 * Functional tests for {@link DefaultPropertyResolver}.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class DefaultPropertyResolverTest
    extends AbstractPropertyResolverTest
{
    public DefaultPropertyResolverTest( String name )
    {
        super( name );
    }

    protected PropertyResolver createResolver()
    {
        return new DefaultPropertyResolver();
    }

    /**
     * Tests handing undefined property.
     */
    public void testUndefinedProp() throws Exception
    {
        final String undefinedProp = "undefinedProperty";
        doTestFailure( "${" + undefinedProp + "}",
                       REZ.getString( "prop.missing-value.error", undefinedProp ),
                       m_context );

        //TODO - "" should be disallowed as a property name
        doTestFailure( "${}",
                       REZ.getString( "prop.missing-value.error", "" ),
                       m_context );
    }
}
