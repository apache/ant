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
package org.apache.ant.antlib.system;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ant.common.util.AntException;

/**
 * Common Base class for the Ant and AntCall tasks
 *
 * @author Conor MacNeill
 * @created 4 February 2002
 */
public abstract class AntBase extends SubBuild {

    /**
     * flag which indicates if all current properties should be passed to the
     * subbuild
     */
    private boolean inheritAll = true;

    /**
     * flag which indicates if all current references should be passed to the
     * subbuild
     */
    private boolean inheritRefs = false;

    /**
     * The key to the subbuild with which the Ant task can manage the subbuild
     */
    private Object subbuildKey;

    /** The name of the target to be evaluated in the sub-build */
    private String targetName;

    /**
     * Get the properties to be used with the sub-build
     *
     * @return the properties the sub-build will start with
     */
    protected Map getProperties() {
        if (!inheritAll) {
            return super.getProperties();
        }

        // need to combine existing properties with new ones
        Map subBuildProperties = getDataService().getAllProperties();

        subBuildProperties.putAll(super.getProperties());
        return subBuildProperties;
    }


    /**
     * Get the list of targets to be executed
     *
     * @return A List of string target names.
     */
    protected List getTargets() {
        List targets = new ArrayList();

        if (targetName != null) {
            targets.add(targetName);
        }
        return targets;
    }


    /**
     * Handle error information produced by the task. When a task prints to
     * System.err the container may catch this and redirect the content back
     * to the task by invoking this method. This method must NOT call
     * System.err, directly or indirectly.
     *
     * @param line The line of error info produce by the task
     * @exception AntException if the output cannot be handled.
     */
    public void handleSystemErr(String line) throws AntException {
        if (subbuildKey == null) {
            super.handleSystemErr(line);
        } else {
            getExecService().handleBuildOutput(subbuildKey, line, true);
        }
    }


    /**
     * Handle Output produced by the task. When a task prints to System.out
     * the container may catch this and redirect the content back to the task
     * by invoking this method. This method must NOT call System.out, directly
     * or indirectly.
     *
     * @param line The line of content produce by the task
     * @exception AntException if the output cannot be handled.
     */
    public void handleSystemOut(String line) throws AntException {
        if (subbuildKey == null) {
            super.handleSystemOut(line);
        } else {
            getExecService().handleBuildOutput(subbuildKey, line, false);
        }
    }


    /**
     * Indicate if all properties should be passed
     *
     * @param inheritAll true if all properties should be passed
     */
    public void setInheritAll(boolean inheritAll) {
        this.inheritAll = inheritAll;
    }


    /**
     * Indicate if all references are to be passed to the subbuild
     *
     * @param inheritRefs true if the sub-build should be given all the
     *      current references
     */
    public void setInheritRefs(boolean inheritRefs) {
        this.inheritRefs = inheritRefs;
    }


    /**
     * Set the key of the subbuild
     *
     * @param key the key returned by the Ant core for managing the subbuild
     */
    protected void setSubBuildKey(Object key) {
        this.subbuildKey = key;
    }


    /**
     * Sets the target to be executed in the subbuild
     *
     * @param targetName the name of the target to build
     */
    public void setTarget(String targetName) {
        this.targetName = targetName;
    }
}

