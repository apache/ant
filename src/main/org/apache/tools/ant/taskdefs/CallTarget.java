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

import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.PropertySet;

/**
 * Call another target in the same project.
 *
 *  <pre>
 *    &lt;target name="foo"&gt;
 *      &lt;antcall target="bar"&gt;
 *        &lt;param name="property1" value="aaaaa" /&gt;
 *        &lt;param name="foo" value="baz" /&gt;
 *       &lt;/antcall&gt;
 *    &lt;/target&gt;
 *
 *    &lt;target name="bar" depends="init"&gt;
 *      &lt;echo message="prop is ${property1} ${foo}" /&gt;
 *    &lt;/target&gt;
 * </pre>
 *
 * <p>This only works as expected if neither property1 nor foo are
 * defined in the project itself.
 *
 *
 * @since Ant 1.2
 *
 * @ant.task name="antcall" category="control"
 */
public class CallTarget extends Task {

    private Ant callee;
    // must match the default value of Ant#inheritAll
    private boolean inheritAll = true;
    // must match the default value of Ant#inheritRefs
    private boolean inheritRefs = false;

    private boolean targetSet = false;

    /**
     * If true, pass all properties to the new Ant project.
     * Defaults to true.
     * @param inherit <code>boolean</code> flag.
     */
    public void setInheritAll(boolean inherit) {
       inheritAll = inherit;
    }

    /**
     * If true, pass all references to the new Ant project.
     * Defaults to false.
     * @param inheritRefs <code>boolean</code> flag.
     */
    public void setInheritRefs(boolean inheritRefs) {
        this.inheritRefs = inheritRefs;
    }

    /**
     * Initialize this task by creating new instance of the ant task and
     * configuring it by calling its own init method.
     */
    public void init() {
        callee = new Ant(this);
        callee.init();
    }

    /**
     * Delegate the work to the ant task instance, after setting it up.
     * @throws BuildException on validation failure or if the target didn't
     * execute.
     */
    public void execute() throws BuildException {
        if (callee == null) {
            init();
        }
        if (!targetSet) {
            throw new BuildException(
                "Attribute target or at least one nested target is required.",
                 getLocation());
        }
        callee.setAntfile(getProject().getProperty(MagicNames.ANT_FILE));
        callee.setInheritAll(inheritAll);
        callee.setInheritRefs(inheritRefs);
        callee.execute();
    }

    /**
     * Create a new Property to pass to the invoked target(s).
     * @return a <code>Property</code> object.
     */
    public Property createParam() {
        if (callee == null) {
            init();
        }
        return callee.createProperty();
    }

    /**
     * Reference element identifying a data type to carry
     * over to the invoked target.
     * @param r the specified <code>Ant.Reference</code>.
     * @since Ant 1.5
     */
    public void addReference(Ant.Reference r) {
        if (callee == null) {
            init();
        }
        callee.addReference(r);
    }

    /**
     * Set of properties to pass to the new project.
     * @param ps the <code>PropertySet</code> to pass.
     * @since Ant 1.6
     */
    public void addPropertyset(PropertySet ps) {
        if (callee == null) {
            init();
        }
        callee.addPropertyset(ps);
    }

    /**
     * Set target to execute.
     * @param target the name of the target to execute.
     */
    public void setTarget(String target) {
        if (callee == null) {
            init();
        }
        callee.setTarget(target);
        targetSet = true;
    }

    /**
     * Add a target to the list of targets to invoke.
     * @param t <code>Ant.TargetElement</code> representing the target.
     * @since Ant 1.6.3
     */
    public void addConfiguredTarget(Ant.TargetElement t) {
        if (callee == null) {
            init();
        }
        callee.addConfiguredTarget(t);
        targetSet = true;
    }

    /**
     * Handles output.
     * Send it the the new project if is present, otherwise
     * call the super class.
     * @param output The string output to output.
     * @see Task#handleOutput(String)
     * @since Ant 1.5
     */
    public void handleOutput(String output) {
        if (callee != null) {
            callee.handleOutput(output);
        } else {
            super.handleOutput(output);
        }
    }

    /**
     * Handles input.
     * Delegate to the created project, if present, otherwise
     * call the super class.
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read.
     *
     * @return the number of bytes read.
     *
     * @exception IOException if the data cannot be read.
     * @see Task#handleInput(byte[], int, int)
     * @since Ant 1.6
     */
    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        if (callee != null) {
            return callee.handleInput(buffer, offset, length);
        }
        return super.handleInput(buffer, offset, length);
    }

    /**
     * Handles output.
     * Send it the the new project if is present, otherwise
     * call the super class.
     * @param output The string to output.
     * @see Task#handleFlush(String)
     * @since Ant 1.5.2
     */
    public void handleFlush(String output) {
        if (callee != null) {
            callee.handleFlush(output);
        } else {
            super.handleFlush(output);
        }
    }

    /**
     * Handle error output.
     * Send it the the new project if is present, otherwise
     * call the super class.
     * @param output The string to output.
     *
     * @see Task#handleErrorOutput(String)
     * @since Ant 1.5
     */
    public void handleErrorOutput(String output) {
        if (callee != null) {
            callee.handleErrorOutput(output);
        } else {
            super.handleErrorOutput(output);
        }
    }

    /**
     * Handle error output.
     * Send it the the new project if is present, otherwise
     * call the super class.
     * @param output The string to output.
     * @see Task#handleErrorFlush(String)
     * @since Ant 1.5.2
     */
    public void handleErrorFlush(String output) {
        if (callee != null) {
            callee.handleErrorFlush(output);
        } else {
            super.handleErrorFlush(output);
        }
    }
}
