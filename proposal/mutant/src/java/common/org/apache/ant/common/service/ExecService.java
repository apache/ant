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
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.model.NamespaceValueCollection;
import org.apache.ant.common.event.BuildListener;

/**
 * The ExecService provides executiuon services to tasks
 *
 * @author Conor MacNeill
 * @created 8 February 2002
 */
public interface ExecService {
    /**
     * Parse an XML file into a build model.
     *
     * @param xmlBuildFile The file containing the XML build description.
     * @return A Project model for the build.
     * @exception AntException if the build cannot be parsed
     */
    Project parseXMLBuildFile(File xmlBuildFile) throws AntException;


    /**
     * Create a project reference.
     *
     * @param referenceName the name under which the project will be
     *      referenced.
     * @param model the project model.
     * @param initialData the project's initial data load.
     * @exception AntException if the project cannot be referenced.
     */
    void createProjectReference(String referenceName, Project model,
                                Map initialData) throws AntException;


    /**
     * Setup a sub-build.
     *
     * @param model the project model to be used for the build
     * @param properties the initiali properties to be used in the build
     * @param addListeners true if the current frame's listeners should be
     *        added to the created Frame
     * @return a key to the build allowing it to be executed and managed
     * @exception AntException if the subbuild cannot be setup
     */
    Object setupBuild(Project model, Map properties, boolean addListeners)
         throws AntException;


    /**
     * Setup a sub-build using the current frame's project model
     *
     * @param properties the initiali properties to be used in the build
     * @param addListeners true if the current frame's listeners should be
     *        added to the created Frame
     * @return a key to the build allowing it to be executed and managed
     * @exception AntException if the subbuild cannot be setup
     */
    Object setupBuild(Map properties, boolean addListeners)
         throws AntException;


    /**
     * Force initialisation of a particular ant library in the context of the
     * given subbuild.
     *
     * @param key the build key.
     * @param libraryId the id of the library to be initialized.
     * @exception AntException if the build cannot be run
     */
    void initializeBuildLibrary(Object key, String libraryId)
        throws AntException;

    /**
     * Add a listener to a subbuild
     *
     * @param key the key identifying the build previously setup
     * @param listener the listener to add to the build.
     *
     * @exception AntException if the build cannot be found.
     */
    void addBuildListener(Object key, BuildListener listener)
        throws AntException;


    /**
     * Run a build which have been previously setup
     *
     * @param key the key returned previously when the build was
     *      setup
     * @param targets A list of targets to be run
     * @exception AntException if the build cannot be run
     */
    void runBuild(Object key, List targets)
        throws AntException;

    /**
     * execute a task. The task should have already been initialised by the
     * core
     *
     * @param task the task to be executed.
     * @exception AntException if there is a problem in execution.
     */
    void executeTask(Task task) throws AntException;


    /**
     * Execute a task with a set of aspect values. Normally aspect values come
     * from a build model but not all tasks will be created from a build model.
     * Some may be created dynamically and configured programatically. This
     * method allows aspect values to provided for execution of such tasks since
     * by their nature, aspect values are not part of the task configuration.
     *
     * @param task the task to be executed
     * @param namespaceValues the namespace attribute values.
     * @exception AntException if there is an execution problem
     */
    void executeTask(Task task, NamespaceValueCollection namespaceValues)
         throws AntException;


    /**
     * get the name of the project associated with this execution.
     *
     * @return the project's name
     */
    String getProjectName();


    /**
     * Get the basedir for the current execution
     *
     * @return the base directory for this execution of Ant
     */
    File getBaseDir();

    /**
     * Set the basedir for the current execution
     *
     * @param baseDir the new base directory for this execution of Ant
     * @exception AntException if the base directory cannot be set to the given
     *            value.
     */
    void setBaseDir(File baseDir) throws AntException ;


    /**
     * Handle subbuild output.
     *
     * @param subbuildKey the core's key for managing the subbuild.
     * @param line the content produce by the current thread.
     * @param isErr true if this content is from the thread's error stream.
     * @exception AntException if the subbuild cannot be found.
     */
    void handleBuildOutput(Object subbuildKey, String line, boolean isErr)
         throws AntException;
}

