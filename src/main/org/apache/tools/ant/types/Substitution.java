/*
 * Copyright  2001-2002,2004 Apache Software Foundation
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

/***
 * A regular expression substitution datatype.  It is an expression
 * that is meant to replace a regular expression.
 *
 * <pre>
 *   &lt;substitition [ [id="id"] expression="expression" | refid="id" ]
 *   /&gt;
 * </pre>
 *
 * @see org.apache.oro.text.regex.Perl5Substitution
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">mattinger@mindless.com</a>
 */
public class Substitution extends DataType {
    /** The name of this data type */
    public static final String DATA_TYPE_NAME = "substitition";

    private String expression;

    public Substitution() {
        this.expression = null;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    /***
     * Gets the pattern string for this RegularExpression in the
     * given project.
     */
    public String getExpression(Project p) {
        if (isReference()) {
            return getRef(p).getExpression(p);
        }

        return expression;
    }

    /***
     * Get the RegularExpression this reference refers to in
     * the given project.  Check for circular references too
     */
    public Substitution getRef(Project p) {
        if (!isChecked()) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }


        Object o = getRefid().getReferencedObject(p);
        if (!(o instanceof Substitution)) {
            String msg = getRefid().getRefId() + " doesn\'t denote a substitution";
            throw new BuildException(msg);
        } else {
            return (Substitution) o;
        }
    }
}
