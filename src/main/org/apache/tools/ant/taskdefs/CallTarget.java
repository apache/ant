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
import org.apache.tools.ant.Task;
import java.io.IOException;

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
 *
 * @since Ant 1.2
 *
 * @ant.task name="antcall" category="control"
 */
public class CallTarget extends Task {

    private Ant callee;
    private String subTarget;
    // must match the default value of Ant#inheritAll
    private boolean inheritAll = true;
    // must match the default value of Ant#inheritRefs
    private boolean inheritRefs = false;

    /**
     * If true, pass all properties to the new Ant project.
     * Defaults to true.
     */
    public void setInheritAll(boolean inherit) {
       inheritAll = inherit;
    }

    /**
     * If true, pass all references to the new Ant project.
     * Defaults to false
     * @param inheritRefs new value
     */
    public void setInheritRefs(boolean inheritRefs) {
        this.inheritRefs = inheritRefs;
    }

    /**
     * init this task by creating new instance of the ant task and
     * configuring it's by calling its own init method.
     */
    public void init() {
        callee = (Ant) getProject().createTask("ant");
        callee.setOwningTarget(getOwningTarget());
        callee.setTaskName(getTaskName());
        callee.setLocation(getLocation());
        callee.init();
    }

    /**
     * hand off the work to the ant task of ours, after setting it up
     * @throws BuildException on validation failure or if the target didn't
     * execute
     */
    public void execute() throws BuildException {
        if (callee == null) {
            init();
        }

        if (subTarget == null) {
            throw new BuildException("Attribute target is required.",
                                     getLocation());
        }

        callee.setAntfile(getProject().getProperty("ant.file"));
        callee.setTarget(subTarget);
        callee.setInheritAll(inheritAll);
        callee.setInheritRefs(inheritRefs);
        callee.execute();
    }

    /**
     * Property to pass to the invoked target.
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
     *
     * @since Ant 1.6
     */
    public void addPropertyset(org.apache.tools.ant.types.PropertySet ps) {
        if (callee == null) {
            init();
        }
        callee.addPropertyset(ps);
    }

    /**
     * Target to execute, required.
     */
    public void setTarget(String target) {
        subTarget = target;
    }

    /**
     * Pass output sent to System.out to the new project.
     *
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
     * @see Task#handleInput(byte[], int, int)
     *
     * @since Ant 1.6
     */
    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        if (callee != null) {
            return callee.handleInput(buffer, offset, length);
        } else {
            return super.handleInput(buffer, offset, length);
        }
    }

    /**
     * Pass output sent to System.out to the new project.
     *
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
     * Pass output sent to System.err to the new project.
     *
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
     * Pass output sent to System.err to the new project and flush stream.
     *
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
