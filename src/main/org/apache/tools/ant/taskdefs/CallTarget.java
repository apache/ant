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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

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
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
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
                                     location);
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
    public void handleOutput(String line) {
        if (callee != null) {
            callee.handleOutput(line);
        } else {
            super.handleOutput(line);
        }
    }
    
    /**
     * Pass output sent to System.out to the new project.
     *
     * @since Ant 1.5.2
     */
    public void handleFlush(String line) {
        if (callee != null) {
            callee.handleFlush(line);
        } else {
            super.handleFlush(line);
        }
    }
    
    /**
     * Pass output sent to System.err to the new project.
     *
     * @since Ant 1.5
     */
    public void handleErrorOutput(String line) {
        if (callee != null) {
            callee.handleErrorOutput(line);
        } else {
            super.handleErrorOutput(line);
        }
    }
    
    /**
     * Pass output sent to System.err to the new project.
     *
     * @since Ant 1.5.2
     */
    public void handleErrorFlush(String line) {
        if (callee != null) {
            callee.handleErrorFlush(line);
        } else {
            super.handleErrorFlush(line);
        }
    }
}
