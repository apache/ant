/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.filters.TokenSet;

/**
 * A token set that uses the project's properties as tokens.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="token-set" name="properties"
 */
public class PropertyTokenSet
    implements TokenSet
{
    /**
     * Evaluates the value for a token.
     */
    public String getValue( String token, TaskContext context )
        throws TaskException
    {
        final Object propValue = context.getProperty( token );
        if( propValue == null )
        {
            return null;
        }
        return propValue.toString();
    }
}
