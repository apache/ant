/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.converter.lib;

import org.apache.aut.converter.AbstractMasterConverter;
import org.apache.aut.converter.Converter;

/**
 * A very simple master converter that is capable of using
 * any of the converters in this package.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class SimpleMasterConverter
    extends AbstractMasterConverter
{
    public SimpleMasterConverter()
    {
        registerConverter( ObjectToStringConverter.class.getName(),
                           "java.lang.Object",
                           "java.lang.String" );
        registerConverter( StringToBooleanConverter.class.getName(),
                           "java.lang.String",
                           "java.lang.Boolean" );
        registerConverter( StringToByteConverter.class.getName(),
                           "java.lang.String",
                           "java.lang.Byte" );
        registerConverter( StringToClassConverter.class.getName(),
                           "java.lang.String",
                           "java.lang.Class" );
        registerConverter( StringToDoubleConverter.class.getName(),
                           "java.lang.String",
                           "java.lang.Double" );
        registerConverter( StringToFloatConverter.class.getName(),
                           "java.lang.String",
                           "java.lang.Float" );
        registerConverter( StringToIntegerConverter.class.getName(),
                           "java.lang.String",
                           "java.lang.Integer" );
        registerConverter( StringToLongConverter.class.getName(),
                           "java.lang.String",
                           "java.lang.Long" );
        registerConverter( StringToShortConverter.class.getName(),
                           "java.lang.String",
                           "java.lang.Short" );
        registerConverter( StringToURLConverter.class.getName(),
                           "java.lang.String",
                           "java.net.URL" );
        registerConverter( StringToDateConverter.class.getName(),
                           "java.lang.String",
                           "java.util.Date" );
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
        final ClassLoader classLoader = getClass().getClassLoader();
        final Class clazz = classLoader.loadClass( name );
        return (Converter)clazz.newInstance();
    }
}
