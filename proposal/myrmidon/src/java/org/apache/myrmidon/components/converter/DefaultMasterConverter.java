/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.converter;

import org.apache.aut.converter.Converter;
import org.apache.aut.converter.AbstractMasterConverter;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Converter engine to handle converting between types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultMasterConverter
    extends AbstractMasterConverter
    implements ConverterRegistry, Serviceable
{
    private TypeManager m_typeManager;

    /**
     * Retrieve relevent services needed to deploy.
     *
     * @param serviceManager the ServiceManager
     * @exception ServiceException if an error occurs
     */
    public void service( final ServiceManager serviceManager )
        throws ServiceException
    {
        m_typeManager = (TypeManager)serviceManager.lookup( TypeManager.ROLE );
    }

    /**
     * Register a converter
     *
     * @param className the className of converter
     * @param source the source classname
     * @param destination the destination classname
     */
    public void registerConverter( final String className,
                                   final String source,
                                   final String destination )
    {
        super.registerConverter( className, source, destination );
    }

    /**
     * Create an instance of converter with specified name.
     *
     * @param name the name of converter
     * @return the created converter instance
     * @throws Exception if converter can not be created.
     */
    protected Converter createConverter( final String name )
        throws Exception
    {
        final TypeFactory factory = m_typeManager.getFactory( Converter.ROLE );
        return (Converter)factory.create( name );
    }
}
