/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.types;

import org.apache.tools.ant.Project;

/***
 * A regular expression substitution datatype.  It is an expression
 * that is meant to replace a regular expression.
 *
 * <pre>
 *   &lt;substitution [ [id="id"] expression="expression" | refid="id" ]
 *   /&gt;
 * </pre>
 *
 * @see org.apache.oro.text.regex.Perl5Substitution
 */
public class Substitution extends DataType {
    /** The name of this data type */
    public static final String DATA_TYPE_NAME = "substitution";

    private String expression;

    /** Constructor for Substitution. */
    public Substitution() {
        this.expression = null;
    }

    /**
     * Set the pattern string for this regular expression substitution.
     * @param expression the regular expression to use
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /***
     * Gets the pattern string for this RegularExpression in the
     * given project.
     * @param p the project to look for the regular expression if this object is
     *          a reference
     * @return the pattern string
     */
    public String getExpression(Project p) {
        if (isReference()) {
            return getRef(p).getExpression(p);
        }

        return expression;
    }

    /***
     * Get the RegularExpression this reference refers to in
     * the given project.  Check for circular references too.
     * @param p the project to look for the regular expression reference
     * @return the resolved reference
     */
    public Substitution getRef(Project p) {
        return getCheckedRef(Substitution.class, getDataTypeName(), p);
    }
}
