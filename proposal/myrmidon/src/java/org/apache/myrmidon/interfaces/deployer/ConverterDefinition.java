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
    private String m_sourceType;
    private String m_destinationType;

    /**
     * Returns the type's role.
     */
    public String getRoleShorthand()
    {
        return "converter";
    }

    /**
     * Returns the type's name.
     */
    public String getName()
    {
        return getClassname();
    }

    /**
     * Returns the converter's source type.
     */
    public String getSourceType()
    {
        return m_sourceType;
    }

    /**
     * Sets the converter's source type.
     */
    public void setSourceType( final String sourceType )
    {
        m_sourceType = sourceType;
    }

    /**
     * Returns the converter's destination type.
     */
    public String getDestinationType()
    {
        return m_destinationType;
    }

    /**
     * Sets the converter's destination type.
     */
    public void setDestinationType( final String destinationType )
    {
        m_destinationType = destinationType;
    }
}
