/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util.mappers;

import java.util.Properties;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Class as Argument to FileNameMapper.setType.
 */
public class MapperType
    extends EnumeratedAttribute
{
    private final Properties c_implementations;

    public MapperType()
    {
        c_implementations = new Properties();
        c_implementations.put( "identity",
                               "org.apache.tools.ant.util.IdentityMapper" );
        c_implementations.put( "flatten",
                               "org.apache.tools.ant.util.FlatFileNameMapper" );
        c_implementations.put( "glob",
                               "org.apache.tools.ant.util.GlobPatternMapper" );
        c_implementations.put( "merge",
                               "org.apache.tools.ant.util.MergingMapper" );
        c_implementations.put( "regexp",
                               "org.apache.tools.ant.util.RegexpPatternMapper" );
    }

    public String getImplementation()
    {
        return c_implementations.getProperty( getValue() );
    }

    public String[] getValues()
    {
        return new String[]{"identity", "flatten", "glob", "merge", "regexp"};
    }
}
