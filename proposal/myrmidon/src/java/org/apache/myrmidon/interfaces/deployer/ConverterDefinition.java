/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.deployer;

/**
 * A converter definition.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class ConverterDefinition
    extends TypeDefinition
{
    private final String m_sourceType;
    private final String m_destinationType;

    public ConverterDefinition( final String className,
                                final String sourceType,
                                final String destinationType )
    {
        super( className, "converter", className );
        m_sourceType = sourceType;
        m_destinationType = destinationType;
    }

    /**
     * Returns the converter's source type.
     */
    public String getSourceType()
    {
        return m_sourceType;
    }

    /**
     * Returns the converter's destination type.
     */
    public String getDestinationType()
    {
        return m_destinationType;
    }
}
