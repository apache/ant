/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.conditions;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * Class representing a condition.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 *
 * @ant:role shorthand="condition"
 */
public interface Condition
{
    /**
     * Evaluates this condition.
     *
     * @param context
     *      The context to evaluate the condition in.
     */
    boolean evaluate( final TaskContext context )
        throws TaskException;
}
