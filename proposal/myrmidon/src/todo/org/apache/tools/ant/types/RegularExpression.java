/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;

import java.util.Stack;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.regexp.Regexp;
import org.apache.tools.ant.util.regexp.RegexpFactory;

/**
 * A regular expression datatype. Keeps an instance of the compiled expression
 * for speed purposes. This compiled expression is lazily evaluated (it is
 * compiled the first time it is needed). The syntax is the dependent on which
 * regular expression type you are using. The system property
 * "ant.regexp.regexpimpl" will be the classname of the implementation that will
 * be used. <pre>
 * For jdk  &lt;= 1.3, there are two available implementations:
 *   org.apache.tools.ant.util.regexp.JakartaOroRegexp (the default)
 *        Based on the jakarta-oro package
 *
 *   org.apache.tools.ant.util.regexp.JakartaRegexpRegexp
 *        Based on the jakarta-regexp package
 *
 * For jdk &gt;= 1.4 an additional implementation is available:
 *   org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp
 *        Based on the jdk 1.4 built in regular expression package.
 * </pre> <pre>
 *   &lt;regularexpression [ [id="id"] pattern="expression" | refid="id" ]
 *   /&gt;
 * </pre>
 *
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">
 *      mattinger@mindless.com</a>
 * @see org.apache.oro.regex.Perl5Compiler
 * @see org.apache.regexp.RE
 * @see java.util.regex.Pattern
 * @see org.apache.tools.ant.util.regexp.Regexp
 */
public class RegularExpression extends DataType
{
    public final static String DATA_TYPE_NAME = "regularexpression";

    // The regular expression factory
    private final static RegexpFactory factory = new RegexpFactory();

    private Regexp regexp;

    public RegularExpression()
        throws TaskException
    {
        this.regexp = factory.newRegexp();
    }

    public void setPattern( String pattern )
        throws TaskException
    {
        this.regexp.setPattern( pattern );
    }

    /**
     * Gets the pattern string for this RegularExpression in the given project.
     *
     * @param p Description of Parameter
     * @return The Pattern value
     */
    public String getPattern( Project p )
        throws TaskException
    {
        if( isReference() )
            return getRef( p ).getPattern( p );

        return regexp.getPattern();
    }

    /**
     * Get the RegularExpression this reference refers to in the given project.
     * Check for circular references too
     *
     * @param p Description of Parameter
     * @return The Ref value
     */
    public RegularExpression getRef( Project p )
        throws TaskException
    {
        if( !checked )
        {
            Stack stk = new Stack();
            stk.push( this );
            dieOnCircularReference( stk, p );
        }

        Object o = ref.getReferencedObject( p );
        if( !( o instanceof RegularExpression ) )
        {
            String msg = ref.getRefId() + " doesn\'t denote a regularexpression";
            throw new TaskException( msg );
        }
        else
        {
            return (RegularExpression)o;
        }
    }

    public Regexp getRegexp( Project p )
        throws TaskException
    {
        if( isReference() )
        {
            return getRef( p ).getRegexp( p );
        }
        return this.regexp;
    }

}
