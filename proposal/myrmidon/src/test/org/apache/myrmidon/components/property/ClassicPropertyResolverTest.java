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
 * A test for {@link ClassicPropertyResolver}.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class ClassicPropertyResolverTest
    extends AbstractPropertyResolverTest
{
    public ClassicPropertyResolverTest( String name )
    {
        super( name );
    }

    protected PropertyResolver createResolver()
    {
        return new ClassicPropertyResolver();
    }

    /**
     * Tests handing undefined property.
     */
    public void testUndefinedProp() throws Exception
    {
        final String undefinedProp = "undefinedProperty";
        final String propRef = "${" + undefinedProp + "}";
        doTestResolution( propRef, propRef, m_context );
    }
}
