/*
 * Copyright  2000-2004 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
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
 * <pre>fail := defined(ifProperty) && !defined(unlessProperty)</pre>
 *
 * A single nested<CODE>&lt;condition&gt;</CODE> element can be specified
 * instead of using <CODE>if</CODE>/<CODE>unless</CODE> (a combined
 * effect can be achieved using <CODE>isset</CODE> conditions).
 *
 * @since Ant 1.2
 *
 * @ant.task name="fail" category="control"
 */
public class Exit extends ConditionBase {
    private String message;
    private String ifCondition, unlessCondition;

    /**
     * A message giving further information on why the build exited.
     *
     * @param value message to output
     */
    public void setMessage(String value) {
        this.message = value;
    }

    /**
     * Only fail if a property of the given name exists in the current project.
     * @param c property name
     */
    public void setIf(String c) {
        ifCondition = c;
    }

    /**
     * Only fail if a property of the given name does not
     * exist in the current project.
     * @param c property name
     */
    public void setUnless(String c) {
        unlessCondition = c;
    }

    /**
     * Throw a <CODE>BuildException</CODE> to exit (fail) the build.
     * If specified, evaluate conditions:
     * A single nested condition is accepted, but requires that the
     * <CODE>if</CODE>/<code>unless</code> attributes be omitted.
     * If the nested condition evaluates to true, or the
     * ifCondition is true or unlessCondition is false, the build will exit.
     * The error message is constructed from the text fields, from
     * the nested condition (if specified), or finally from
     * the if and unless parameters (if present).
     * @throws BuildException
     */
    public void execute() throws BuildException {
        boolean fail = (nestedConditionPresent()) ? testNestedCondition()
                     : (testIfCondition() && testUnlessCondition());
        if (fail) {
            String text = null;
            if (message != null && message.trim().length() > 0) {
                text = message.trim();
            } else {
                if (ifCondition != null && ifCondition.length() > 0
                    && getProject().getProperty(ifCondition) != null) {
                    text = "if=" + ifCondition;
                }
                if (unlessCondition != null && unlessCondition.length() > 0
                    && getProject().getProperty(unlessCondition) == null) {
                    if (text == null) {
                        text = "";
                    } else {
                        text += " and ";
                    }
                    text += "unless=" + unlessCondition;
                }
                if (nestedConditionPresent()) {
                    text = "condition satisfied";
                } else {
                    if (text == null) {
                        text = "No message";
                    }
                }
            }
            throw new BuildException(text);
        }
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
     * test the if condition
     * @return true if there is no if condition, or the named property exists
     */
    private boolean testIfCondition() {
        if (ifCondition == null || "".equals(ifCondition)) {
            return true;
        }
        return getProject().getProperty(ifCondition) != null;
    }

    /**
     * test the unless condition
     * @return true if there is no unless condition,
     *  or there is a named property but it doesn't exist
     */
    private boolean testUnlessCondition() {
        if (unlessCondition == null || "".equals(unlessCondition)) {
            return true;
        }
        return getProject().getProperty(unlessCondition) == null;
    }

    /**
     * test the nested condition
     * @return true if there is none, or it evaluates to true
     */
    private boolean testNestedCondition() {
        if (ifCondition != null || unlessCondition != null) {
            throw new BuildException("Nested conditions "
              + "not permitted in conjunction with if/unless attributes");
        }

        int count = countConditions();
        if (count > 1) {
            throw new BuildException("Too many conditions:  " + count);
        }

        return (count == 0) ? true
            : (((Condition)(getConditions().nextElement())).eval());
    }

    private boolean nestedConditionPresent() {
        return (countConditions() > 0);
    }
}
