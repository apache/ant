/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.text;

import java.util.Stack;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;

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
    extends DataType
{
    public final static String DATA_TYPE_NAME = "substitition";

    private String expression;

    public Substitution()
    {
        this.expression = null;
    }

    public void setExpression( String expression )
    {
        this.expression = expression;
    }

    /**
     * Gets the pattern string for this RegularExpression in the given project.
     *
     * @param p Description of Parameter
     * @return The Expression value
     */
    public String getExpression( Project p )
        throws TaskException
    {
        if( isReference() )
        {
            return getRef( p ).getExpression( p );
        }

        return expression;
    }

    /**
     * Get the RegularExpression this reference refers to in the given project.
     * Check for circular references too
     *
     * @param p Description of Parameter
     * @return The Ref value
     */
    public Substitution getRef( Project p )
        throws TaskException
    {
        if( !checked )
        {
            Stack stk = new Stack();
            stk.push( this );
            dieOnCircularReference( stk, p );
        }

        Object o = ref.getReferencedObject( p );
        if( !( o instanceof Substitution ) )
        {
            String msg = ref.getRefId() + " doesn\'t denote a substitution";
            throw new TaskException( msg );
        }
        else
        {
            return (Substitution)o;
        }
    }
}
