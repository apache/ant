/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.avalon.framework.configuration.DefaultConfiguration;

/**
 * The Ant1Compat version of the &lt;antcall&gt; task, which delegates to the
 * Myrmidon version.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class CallTarget extends AbstractAnt1AntTask
{
    /**
     * Properties are referred to as Parameters in &lt;antcall&gt;
     */
    public Property createParam()
    {
        return doCreateProperty();
    }

    /**
     * The only configuration not done by base class is the task name.
     */
    protected DefaultConfiguration buildTaskModel()
    {
        return new DefaultConfiguration( "ant-call", "" );
    }
}
