/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


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
 * @author <a href="mailto:nico@seessle.de">Nico Seessle</a>
 * @author steve loughran
 * @since Ant 1.2
 *
 * @ant.task name="fail" category="control"
 */
public class Exit extends Task {
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
     * evaluate both if and unless conditions, and if
     * ifCondition is true or unlessCondition is false, throw a
     * build exception to exit the build.
     * The error message is constructed from the text fields, or from
     * the if and unless parameters (if present).
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (testIfCondition() && testUnlessCondition()) {
            String text = null;
            if (message != null && message.length() > 0) {
                text = message;
            } else {

                if (getProject().getProperty(ifCondition) != null) {
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

}