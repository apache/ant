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
package org.apache.ant.antcore.config;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.ant.common.model.BuildElement;

/**
 * An AntConfig is the java class representation of the antconfig.xml files
 * used to configure Ant.
 *
 * @author Conor MacNeill
 * @created 20 January 2002
 */
public class AntConfig {
    /** Indicates if remote libraries may be used */
    private boolean remoteLibs = false;

    /** Indicates if remote projects may be used */
    private boolean remoteProjects = false;

    /** Indicates if unset properties are ignored */
    private boolean unsetProperties = true;

    /**
     * Configuration tasks. 
     */
    private List tasks = new ArrayList();
    
    /**
     * Indicate if unset properties are OK.
     *
     * @return true if unset properties will not cause an exception
     */
    public boolean isUnsetPropertiesAllowed() {
        return unsetProperties;
    }

    /**
     * Indicate if the use of remote library's is allowe dby this config.
     *
     * @return true if this config allows the use of remote libraries,
     */
    public boolean isRemoteLibAllowed() {
        return remoteLibs;
    }

    /**
     * Indicate if this config allows the execution of a remote project
     *
     * @return true if remote projects are allowed
     */
    public boolean isRemoteProjectAllowed() {
        return remoteProjects;
    }

    /**
     * Get the configuration tasks
     *
     * @return an iterator over the set of config tasks.
     */
    public Iterator getTasks() {
        return tasks.iterator();
    }

    /**
     * Add a config task.
     *
     * @param task a task to be executed as part of the configuration process.
     */
    public void addTask(BuildElement task) {
        tasks.add(task);
    }

    /**
     * Allow remote libraries to be used
     *
     * @param allowRemoteLibs true if remote libraries may be used.
     */
    public void allowRemoteLibs(boolean allowRemoteLibs) {
        this.remoteLibs = allowRemoteLibs;
    }

    /**
     * Allow remote projects to be used
     *
     * @param allowRemoteProjects true if remote projects may be executed.
     */
    public void allowRemoteProjects(boolean allowRemoteProjects) {
        this.remoteProjects = allowRemoteProjects;
    }

    /**
     * Allow properties to be used even when they have not been set
     *
     * @param allowUnsetProperties true if un set properties should not
     *      cause an exception
     */
    public void allowUnsetProperties(boolean allowUnsetProperties) {
        this.unsetProperties = allowUnsetProperties;
    }
    
    /**
     * Merge in another configuration. The configuration being merged in
     * takes precedence
     *
     * @param otherConfig the other AntConfig to be merged.
     */
    public void merge(AntConfig otherConfig) {
        remoteLibs = otherConfig.remoteLibs;
        remoteProjects = otherConfig.remoteProjects;
        unsetProperties = otherConfig.unsetProperties;
        tasks.addAll(otherConfig.tasks);
    }
}

