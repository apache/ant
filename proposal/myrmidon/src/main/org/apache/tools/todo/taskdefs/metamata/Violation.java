/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.metamata;

/**
 * the class used to report violation information
 */
final class Violation
{
    private final String m_error;
    private final int m_line;

    public Violation( final String error, final int line )
    {
        m_error = error;
        m_line = line;
    }

    protected String getError()
    {
        return m_error;
    }

    protected int getLine()
    {
        return m_line;
    }
}
