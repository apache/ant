/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.types;


import java.util.Stack;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.regexp.Regexp;
import org.apache.tools.ant.util.regexp.RegexpFactory;

/***
 * A regular expression datatype.  Keeps an instance of the
 * compiled expression for speed purposes.  This compiled
 * expression is lazily evaluated (it is compiled the first
 * time it is needed).  The syntax is the dependent on which
 * regular expression type you are using.  The system property
 * "ant.regexp.regexpimpl" will be the classname of the implementation
 * that will be used.
 *
 * <pre>
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
 * </pre>
 *
 * <pre>
 *   &lt;regexp [ [id="id"] pattern="expression" | refid="id" ] 
 *   /&gt;
 * </pre>
 *
 * @see org.apache.oro.text.regex.Perl5Compiler
 * @see org.apache.regexp.RE
 * @see java.util.regex.Pattern
 *
 * @see org.apache.tools.ant.util.regexp.Regexp
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">mattinger@mindless.com</a>
 *
 * @ant.datatype name="regexp"
 */
public class RegularExpression extends DataType {
    /** Name of this data type */
    public static final String DATA_TYPE_NAME = "regexp";

    // The regular expression factory
    private static final RegexpFactory factory = new RegexpFactory();

    private Regexp regexp;

    public RegularExpression() {
        this.regexp = factory.newRegexp();
    }

    public void setPattern(String pattern) {
        this.regexp.setPattern(pattern);
    }

    /***
     * Gets the pattern string for this RegularExpression in the
     * given project.
     */
    public String getPattern(Project p) {
        if (isReference()) {
            return getRef(p).getPattern(p);
        }

        return regexp.getPattern();
    }

    public Regexp getRegexp(Project p) {
        if (isReference()) {
            return getRef(p).getRegexp(p);
        }
        return this.regexp;
    }

    /***
     * Get the RegularExpression this reference refers to in
     * the given project.  Check for circular references too
     */
    public RegularExpression getRef(Project p) {
        if (!checked) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }

        
        Object o = ref.getReferencedObject(p);
        if (!(o instanceof RegularExpression)) {
            String msg = ref.getRefId() + " doesn\'t denote a " 
                + DATA_TYPE_NAME;
            throw new BuildException(msg);
        } else {
            return (RegularExpression) o;
        }
    }
}
