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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;

/**
 * Exits the active build, giving an additional message
 * if available.
 *
 * The <code>if</code> and <code>unless</code> attributes make the
 * failure conditional -both probe for the named property being defined.
 * The <code>if</code> tests for the property being defined, the
 * <code>unless</code> for a property being undefined.
 *
 * If both attributes are set, then the test fails only if both tests
 * are true. i.e.
 * <pre>fail := defined(ifProperty) &amp;&amp; !defined(unlessProperty)</pre>
 *
 * A single nested<code>&lt;condition&gt;</code> element can be specified
 * instead of using <code>if</code>/<code>unless</code> (a combined
 * effect can be achieved using <code>isset</code> conditions).
 *
 * @since Ant 1.2
 *
 * @ant.task name="fail" category="control"
 */
public class Exit extends Task {

    private static class NestedCondition extends ConditionBase implements Condition {
        @Override
        public boolean eval() {
            if (countConditions() != 1) {
                throw new BuildException(
                    "A single nested condition is required.");
            }
            return getConditions().nextElement().eval();
        }
    }

    private String message;
    private Object ifCondition, unlessCondition;
    private NestedCondition nestedCondition;
    private Integer status;

    /**
     * A message giving further information on why the build exited.
     *
     * @param value message to output
     */
    public void setMessage(String value) {
        this.message = value;
    }

    /**
     * Only fail if the given expression evaluates to true or the name
     * of an existing property.
     * @param c property name or evaluated expression
     * @since Ant 1.8.0
     */
    public void setIf(Object c) {
        ifCondition = c;
    }

    /**
     * Only fail if the given expression evaluates to true or the name
     * of an existing property.
     * @param c property name or evaluated expression
     */
    public void setIf(String c) {
        setIf((Object) c);
    }

    /**
     * Only fail if the given expression evaluates to false or tno
     * property of the given name exists.
     * @param c property name or evaluated expression
     * @since Ant 1.8.0
     */
    public void setUnless(Object c) {
        unlessCondition = c;
    }

    /**
     * Only fail if the given expression evaluates to false or tno
     * property of the given name exists.
     * @param c property name or evaluated expression
     */
    public void setUnless(String c) {
        setUnless((Object) c);
    }

    /**
     * Set the status code to associate with the thrown Exception.
     * @param i   the <code>int</code> status
     */
    public void setStatus(int i) {
        status = i;
    }

    /**
     * Throw a <code>BuildException</code> to exit (fail) the build.
     * If specified, evaluate conditions:
     * A single nested condition is accepted, but requires that the
     * <code>if</code>/<code>unless</code> attributes be omitted.
     * If the nested condition evaluates to true, or the
     * ifCondition is true or unlessCondition is false, the build will exit.
     * The error message is constructed from the text fields, from
     * the nested condition (if specified), or finally from
     * the if and unless parameters (if present).
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        boolean fail = (nestedConditionPresent()) ? testNestedCondition()
                     : (testIfCondition() && testUnlessCondition());
        if (fail) {
            String text = null;
            if (message != null && !message.trim().isEmpty()) {
                text = message.trim();
            } else {
                if (!isNullOrEmpty(ifCondition) && testIfCondition()) {
                    text = "if=" + ifCondition;
                }
                if (!isNullOrEmpty(unlessCondition) && testUnlessCondition()) {
                    if (text == null) {
                        text = "";
                    } else {
                        text += " and ";
                    }
                    text += "unless=" + unlessCondition;
                }
                if (nestedConditionPresent()) {
                    text = "condition satisfied";
                } else if (text == null) {
                    text = "No message";
                }
            }
            log("failing due to " + text, Project.MSG_DEBUG);
            throw status == null ? new BuildException(text)
                : new ExitStatusException(text, status);
        }
    }

    private boolean isNullOrEmpty(Object value) {
        return value == null || "".equals(value);
    }

    /**
     * Set a multiline message.
     * @param msg the message to display
     */
    public void addText(String msg) {
        if (message == null) {
            message = "";
        }
        message += getProject().replaceProperties(msg);
    }

    /**
     * Add a condition element.
     * @return <code>ConditionBase</code>.
     * @since Ant 1.6.2
     */
    public ConditionBase createCondition() {
        if (nestedCondition != null) {
            throw new BuildException("Only one nested condition is allowed.");
        }
        nestedCondition = new NestedCondition();
        return nestedCondition;
    }

    /**
     * test the if condition
     * @return true if there is no if condition, or the named property exists
     */
    private boolean testIfCondition() {
        return PropertyHelper.getPropertyHelper(getProject())
            .testIfCondition(ifCondition);
    }

    /**
     * test the unless condition
     * @return true if there is no unless condition,
     *  or there is a named property but it doesn't exist
     */
    private boolean testUnlessCondition() {
        return PropertyHelper.getPropertyHelper(getProject())
            .testUnlessCondition(unlessCondition);
    }

    /**
     * test the nested condition
     * @return true if there is none, or it evaluates to true
     */
    private boolean testNestedCondition() {
        boolean result = nestedConditionPresent();

        if (result && ifCondition != null || unlessCondition != null) {
            throw new BuildException(
                "Nested conditions not permitted in conjunction with if/unless attributes");
        }

        return result && nestedCondition.eval();
    }

    /**
     * test whether there is a nested condition.
     * @return <code>boolean</code>.
     */
    private boolean nestedConditionPresent() {
        return (nestedCondition != null);
    }

}
