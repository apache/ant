/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.util.Properties;

/**
 * Class as Argument to FileNameMapper.setType.
 */
public class MapperType
    extends EnumeratedAttribute
{
    private final Properties m_implementations;

    public MapperType()
    {
        m_implementations = new Properties();
        m_implementations.put( "identity",
                               "org.apache.tools.ant.util.IdentityMapper" );
        m_implementations.put( "flatten",
                               "org.apache.tools.ant.util.FlatFileNameMapper" );
        m_implementations.put( "glob",
                               "org.apache.tools.ant.util.GlobPatternMapper" );
        m_implementations.put( "merge",
                               "org.apache.tools.ant.util.MergingMapper" );
        m_implementations.put( "regexp",
                               "org.apache.tools.ant.util.RegexpPatternMapper" );
    }

    public String getImplementation()
    {
        return m_implementations.getProperty( getValue() );
    }

    public String[] getValues()
    {
        return new String[]{"identity", "flatten", "glob", "merge", "regexp"};
    }
}
