/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.text;

/**
 * A regular expression substitution datatype. It is an expression that is meant
 * to replace a regular expression. <pre>
 *   &lt;substitition [ [id="id"] expression="expression" | refid="id" ]
 *   /&gt;
 * </pre>
 *
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">
 *      mattinger@mindless.com</a>
 * @see org.apache.oro.text.regex.Perl5Substitition
 */
public class Substitution
{
    private String m_expression;

    public void setExpression( final String expression )
    {
        m_expression = expression;
    }

    /**
     * Gets the pattern string for this RegularExpression in the given project.
     */
    public String getExpression()
    {
        return m_expression;
    }
}
