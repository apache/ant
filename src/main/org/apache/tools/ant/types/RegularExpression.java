/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
        if (!isChecked()) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }


        Object o = getRefid().getReferencedObject(p);
        if (!(o instanceof RegularExpression)) {
            String msg = getRefid().getRefId() + " doesn\'t denote a "
                + DATA_TYPE_NAME;
            throw new BuildException(msg);
        } else {
            return (RegularExpression) o;
        }
    }
}
