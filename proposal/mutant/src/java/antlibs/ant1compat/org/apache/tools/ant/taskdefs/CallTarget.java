/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.ant.antlib.system.AntBase;
import org.apache.ant.antlib.system.AntCall;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.AntLibFactory;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.util.AntException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * CallTarget facade over AntCall
 *
 * @author Conor MacNeill
 * @created 31 January 2002
 */
public class CallTarget extends Task {
    /** The core AntCall implementation to actually use */
    private AntCall antCall = null;

    /** The properties created by this task */
    private List properties = new ArrayList();

    /**
     * If true, inherit all properties from parent Project If false, inherit
     * only userProperties and those defined inside the antcall call itself
     *
     * @param inherit the new inheritAll value
     */
    public void setInheritAll(boolean inherit) {
        antCall.setInheritAll(inherit);
    }

    /**
     * Sets the target of the CallTarget
     *
     * @param target the new target value
     */
    public void setTarget(String target) {
        antCall.setTarget(target);
    }

    /** Initialize the task */
    public void init() {
        AntContext context = getAntContext();
        try {
            ComponentService componentService = getComponentService();
            AntLibFactory factory = getProject().getFactory();
            antCall = (AntCall) componentService.createComponent("ant.system",
                "antcall");
        } catch (AntException e) {
            throw new BuildException(e);
        }
    }

    /** execute the call */
    public void execute() {
        for (Iterator i = properties.iterator(); i.hasNext();) {
            Property property = (Property) i.next();
            AntBase.Property newProperty = new AntBase.Property();
            newProperty.setName(property.getName());
            newProperty.setValue(property.getValue());
            antCall.addProperty(newProperty);
        }
        try {
            antCall.execute();
        } catch (AntException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Create a nested param element.
     *
     * @return the Property object to be configured.
     */
    public Property createParam() {
        Property property = new Property();
        properties.add(property);
        return property;
    }

    /**
     * create a reference element that identifies a data type that should be
     * carried over to the new project.
     *
     * @param r the reference to be added to the call
     */
    public void addReference(AntBase.Reference r) {
        try {
            antCall.addReference(r);
        } catch (AntException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Gets the componentService
     *
     * @return the componentService instance provided by the core
     * @exception AntException if the service is not available.
     */
    private ComponentService getComponentService() throws AntException {
        AntContext context = getAntContext();
        return
            (ComponentService) context.getCoreService(ComponentService.class);
    }
}

