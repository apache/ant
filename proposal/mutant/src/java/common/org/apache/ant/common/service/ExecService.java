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
package org.apache.ant.common.service;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.ant.common.antlib.Task;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.util.ExecutionException;

/**
 * The ExecService provides executiuon services to tasks
 *
 * @author Conor MacNeill
 * @created 8 February 2002
 */
public interface ExecService {
    /**
     * Run a sub-build.
     *
     * @param antFile the file containing the XML description of the model
     * @param targets A list of targets to be run
     * @param properties the initiali properties to be used in the build
     * @exception ExecutionException if the subbuild cannot be run
     */
    void runBuild(File antFile, Map properties, List targets)
         throws ExecutionException;

    /**
     * Run a sub-build.
     *
     * @param model the project model to be used for the build
     * @param targets A list of targets to be run
     * @param properties the initiali properties to be used in the build
     * @exception ExecutionException if the subbuild cannot be run
     */
    void runBuild(Project model, Map properties, List targets)
         throws ExecutionException;

    /**
     * Run a sub-build using the current frame's project model
     *
     * @param targets A list of targets to be run
     * @param properties the initiali properties to be used in the build
     * @exception ExecutionException if the subbuild cannot be run
     */
    void callTarget(Map properties, List targets)
         throws ExecutionException;

    /**
     * execute a task. The task should have already been initialised by
     * the core
     *
     * @param task the task to be executed. 
     * @exception ExecutionException if there is a problem in execution.
     */
    void executeTask(Task task) throws ExecutionException;
}

