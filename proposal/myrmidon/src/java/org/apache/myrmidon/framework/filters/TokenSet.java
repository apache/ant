/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.filters;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A set of tokens.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:role shorthand="token-set"
 */
public interface TokenSet
{
    /**
     * Evaluates the value for a token.
     *
     * @param token the token to evaluate.
     * @param context the context to use to evaluate the token.
     * @return the value for the token, or null if the token is unknown.
     */
    String getValue( String token, TaskContext context )
        throws TaskException;
}
