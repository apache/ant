/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.property.test;

import org.apache.myrmidon.interfaces.property.PropertyResolver;
import org.apache.myrmidon.components.property.test.AbstractPropertyResolverTestCase;
import org.apache.myrmidon.components.property.DefaultPropertyResolver;
import org.apache.myrmidon.components.store.DefaultPropertyStore;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * Functional tests for {@link org.apache.myrmidon.components.property.DefaultPropertyResolver}.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class DefaultPropertyResolverTestCase
    extends AbstractPropertyResolverTestCase
{
    public DefaultPropertyResolverTestCase( final String name )
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
        final Resources rez = getResourcesForTested( DefaultPropertyStore.class );
        final String undefinedProp = "undefinedProperty";
        doTestFailure( "${" + undefinedProp + "}",
                       rez.getString( "unknown-prop.error", undefinedProp ),
                       m_store );

        //TODO - "" should be disallowed as a property name
        doTestFailure( "${}",
                       rez.getString( "unknown-prop.error", "" ),
                       m_store );
    }
}
