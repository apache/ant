/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.ant.tasks.build;


import org.apache.ant.AntException;
import org.apache.ant.engine.TaskEngine;
import org.apache.ant.tasks.BaseTask;
import org.apache.ant.tasks.Task;

public class Project extends BaseTask {
    
    private String def;
    private String basedir;
    
    public Project() {
        super();
    }
    
    public String getDefault() {
        return def;
    }
    
    public void setDefault(String newDefault) {
        def = newDefault;
    }
    
    public void setBasedir(String newBasedir) {
        basedir = newBasedir;
    }
    
    public String getBasedir() {
        return basedir;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //                        BaseTask Implementation                         //
    ////////////////////////////////////////////////////////////////////////////
    
    public void init(TaskEngine engine) throws AntException {
    }
    
    /**
     * Here is where we check and see if there are any Targets specified. We do
     * this by peeking into the known taskStack and checking to see if a Target
     * is next to be executed. If not, we add our default Target to the list.
     */
    public void execute(TaskEngine engine) throws AntException {
        // see if it is necessary to invoke the default task
        Task task = engine.getNextExecuteTask();
        if (task == null && getDefault() != null) {
            Task defaultTask = getTask(getDefault());
            if (defaultTask != null) {
                engine.execute(defaultTask);
            }
        }
    }
    
    public boolean isPropertyContainer() {
        return true;
    }
}
