/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.types.Argument;

/**
 * Adds an "implementation" attribute to Commandline$Attribute used to
 * filter command line attributes based on the current implementation.
 */
public class ImplementationSpecificArgument
    extends Argument
{
    private String m_impl;
    private Javac m_javac;

    public ImplementationSpecificArgument( Javac javac )
    {
        m_javac = javac;
    }

    public void setImplementation( String impl )
    {
        this.m_impl = impl;
    }

    public String[] getParts()
    {
        if( m_impl == null || m_impl.equals( m_javac.determineCompiler() ) )
        {
            return super.getParts();
        }
        else
        {
            return new String[ 0 ];
        }
    }
}
