/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

/**
 * A Constant Pool entry which represents a constant value.
 *
 * @author Conor MacNeill
 */
public abstract class ConstantCPInfo extends ConstantPoolEntry
{

    /**
     * The entry's untyped value. Each subclass interprets the constant value
     * based on the subclass's type. The value here must be compatible.
     */
    private Object value;

    /**
     * Initialise the constant entry.
     *
     * @param tagValue the constant pool entry type to be used.
     * @param entries the number of constant pool entry slots occupied by this
     *      entry.
     */
    protected ConstantCPInfo( int tagValue, int entries )
    {
        super( tagValue, entries );
    }

    /**
     * Set the constant value.
     *
     * @param newValue the new untyped value of this constant.
     */
    public void setValue( Object newValue )
    {
        value = newValue;
    }

    /**
     * Get the value of the constant.
     *
     * @return the value of the constant (untyped).
     */
    public Object getValue()
    {
        return value;
    }

}

