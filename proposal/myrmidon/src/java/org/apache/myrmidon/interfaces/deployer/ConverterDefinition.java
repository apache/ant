/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.deployer;

/**
 * A specialised TypeDefinition which defines a converter.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class ConverterDefinition
    extends TypeDefinition
{
    private final String m_sourceType;
    private final String m_destinationType;

    /**
     * Creates a converter definition.
     * @param className the name of the implementing class
     * @param sourceType the name of the types converted from
     * @param destinationType the name of the type converted to
     */
    public ConverterDefinition( final String className,
                                final String sourceType,
                                final String destinationType )
    {
        super( className, "converter", className );
        m_sourceType = sourceType;
        m_destinationType = destinationType;
    }

    /**
     * Provides the name of the type which this converter can convert from.
     * @return the converter's source type.
     */
    public String getSourceType()
    {
        return m_sourceType;
    }

    /**
     * Provides the name of the type which this converter can convert to.
     * @return the converter's destination type.
     */
    public String getDestinationType()
    {
        return m_destinationType;
    }
}
