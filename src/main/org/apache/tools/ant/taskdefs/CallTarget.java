/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import org.apache.tools.ant.*;

/**
 * Call another target in the same project.
 *
 *  <pre>
 *    <target name="foo">
 *      <antcall target="bar">
 *        <param name="property1" value="aaaaa" />
 *        <param name="foo" value="baz" />
 *       </antcall>
 *    </target>
 *
 *    <target name="bar" depends="init">
 *      <echo message="prop is ${property1} ${foo}" />
 *    </target>
 * </pre>
 *
 * <p>This only works as expected if neither property1 nor foo are
 * defined in the project itself.
 *
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class CallTarget extends Task {

    private Ant callee;
    private String subTarget;
    private boolean initialized = false;
    private boolean inheritAll = true;

    /**
     * If true, inherit all properties from parent Project
     * If false, inherit only userProperties and those defined
     * inside the antcall call itself
     **/
    public void setInheritAll(boolean inherit) {
       inheritAll = inherit;
    } //-- setInheritAll

    public void init() {
        callee = (Ant) project.createTask("ant");
        callee.setOwningTarget(target);
        callee.setTaskName(getTaskName());
        callee.setLocation(location);
        callee.init();
        initialized = true;
    }

    public void execute() {
        if (!initialized) {
            init();
        }
        
        if (subTarget == null) {
            throw new BuildException("Attribute target is required.", 
                                     location);
        }
        
        callee.setDir(project.getBaseDir());
        callee.setAntfile(project.getProperty("ant.file"));
        callee.setTarget(subTarget);
        callee.setInheritAll(inheritAll);
        callee.execute();
    }

    public Property createParam() {
        return callee.createProperty();
    }

    public void setTarget(String target) {
        subTarget = target;
    }

    protected void handleOutput(String line) {
        if (callee != null) {
            callee.handleOutput(line);
        }
        else {
            super.handleOutput(line);
        }
    }
    
    protected void handleErrorOutput(String line) {
        if (callee != null) {
            callee.handleErrorOutput(line);
        }
        else {
            super.handleErrorOutput(line);
        }
    }
    
}
