/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;

/**
 * Adaption of VAJLocalUtil to Task context.
 */
class VAJLocalToolUtil
    extends VAJLocalUtil
{
    private VAJTask m_task;

    public VAJLocalToolUtil( final VAJTask task )
    {
        m_task = task;
    }

    public void log( final String msg, final int level )
    {
        m_task.log( msg, level );
    }
}
