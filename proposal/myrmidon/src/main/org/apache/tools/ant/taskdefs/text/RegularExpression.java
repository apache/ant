/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.text;

import org.apache.myrmidon.api.TaskException;
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
public class RegularExpression
{
    // The regular expression factory
    private final static RegexpFactory factory = new RegexpFactory();

    private Regexp m_regexp;

    public RegularExpression()
        throws TaskException
    {
        m_regexp = factory.newRegexp();
    }

    public void setPattern( final String pattern )
        throws TaskException
    {
        m_regexp.setPattern( pattern );
    }

    /**
     * Gets the pattern string for this RegularExpression in the given project.
     */
    public String getPattern()
        throws TaskException
    {
        return m_regexp.getPattern();
    }

    public Regexp getRegexp()
    {
        return m_regexp;
    }
}
