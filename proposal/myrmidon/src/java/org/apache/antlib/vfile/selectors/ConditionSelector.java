/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile.selectors;

import org.apache.myrmidon.framework.conditions.Condition;
import org.apache.myrmidon.framework.conditions.AndCondition;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.aut.vfs.FileObject;

/**
 * A file selector that evaluates a set of nested {@link Condition} elements.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:type type="v-file-selector" name="condition"
 */
public class ConditionSelector
    implements FileSelector
{
    private AndCondition m_condition = new AndCondition();

    /**
     * Adds a condition.
     */
    public void add( final Condition condition )
    {
        m_condition.add( condition );
    }

    /**
     * Accepts a file.
     */
    public boolean accept( final FileObject file,
                           final String path,
                           final TaskContext context )
        throws TaskException
    {
        return m_condition.evaluate( context );
    }
}
