/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.runtime;

import org.apache.myrmidon.framework.AbstractTypeDef;
import org.apache.myrmidon.interfaces.deployer.ConverterDefinition;
import org.apache.myrmidon.interfaces.deployer.TypeDefinition;

/**
 * Task to define a converter.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:task name="converter-def"
 */
public class ConverterDef
    extends AbstractTypeDef
{
    protected TypeDefinition createTypeDefinition()
    {
        return new ConverterDefinition();
    }
}
