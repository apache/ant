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

import java.util.Map;
import org.apache.ant.common.antlib.AbstractAspect;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.Task;
import org.apache.ant.common.service.DataService;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.util.ExecutionException;
import org.apache.ant.common.model.BuildElement;
import org.apache.ant.common.model.AspectValueCollection;

/**
 * The Ant aspect - handles all ant aspects
 *
 * @author Conor MacNeill
 */
public class AntAspect extends AbstractAspect {
    /** The Ant aspect used to identify Ant metadata */
    public static final String ANT_ASPECT = "ant";
    
    /** The core's data service implementation */
    private DataService dataService = null;

    /** The core's component service */
    private ComponentService componentService = null;

    /**
     * Initialise the aspect with a context. 
     *
     * @param context the aspect's context
     * @exception ExecutionException if the aspect cannot be initialised
     */
    public void init(AntContext context) throws ExecutionException {
        super.init(context);
        dataService = (DataService) context.getCoreService(DataService.class);
        componentService 
            = (ComponentService) context.getCoreService(ComponentService.class);
    }
    
    /**
     * This join point is activated after a component has been created and
     * configured. If the aspect wishes, an object can be returned in place
     * of the one created by Ant. 
     *
     * @param component the component that has been created.
     * @param model the Build model used to create the component.
     *
     * @return a replacement for the component if desired. If null is returned
     *         the current component is used.
     * @exception ExecutionException if the component cannot be processed.
     */         
    public Object postCreateComponent(Object component, BuildElement model) 
         throws ExecutionException {
        String typeId = model.getAspectAttributeValue(ANT_ASPECT, "id");
    
        if (typeId != null) {
            dataService.setMutableDataValue(typeId, component);
        }
        
        return null;
    }

    /**
     * This join point is activated just prior to task execution.
     *
     * @param task the task being executed.
     * @param aspectValues a collection of aspect attribute values for use 
     *        during the task execution.
     *
     * @return an objectwhich indicates that this aspect wishes to 
     * be notified after execution has been completed, in which case the obkect
     * is returned to provide the aspect its context. If this returns null
     * the aspect's postExecuteTask method will not be invoked.
     * @exception ExecutionException if the aspect cannot process the task.
     */
    public Object preExecuteTask(Task task, AspectValueCollection aspectValues) 
         throws ExecutionException {
        AntAspectContext aspectContext = new AntAspectContext();
        Map antAspectValues = aspectValues.getAttributes(ANT_ASPECT);
        if (antAspectValues == null) {
            return null;
        }
        
        componentService.configureAttributes(aspectContext, antAspectValues);
        if (aspectContext.isRequired()) {
            return aspectContext;
        }
        return null;             
    }
    
    /**
     * This join point is activated after a task has executed. The aspect
     * may override the task's failure cause by returning a new failure.
     *
     * @param context the context the aspect provided in preExecuteTask.
     * @param failureCause the current failure reason for the task.
     *
     * @return a new failure reason or null if the task is not to fail.
     */
    public Throwable postExecuteTask(Object context, Throwable failureCause) {
        AntAspectContext aspectContext = (AntAspectContext) context;
        if (!aspectContext.getFailOnError()) {
            return null;
        }
        return super.postExecuteTask(context, failureCause);
    }

}

