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
package org.apache.ant.antcore.execution;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.ant.antcore.config.AntConfig;
import org.apache.ant.common.event.BuildListener;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.util.ExecutionException;
import org.apache.ant.common.util.DemuxOutputReceiver;
import org.apache.ant.init.InitConfig;

/**
 * The ExecutionManager is used to manage the execution of a build. The
 * Execution manager is responsible for loading the Ant task libraries,
 * creating Frames for each project that is part of the build and then
 * executing the tasks within those Execution Frames.
 *
 * @author Conor MacNeill
 * @created 12 January 2002
 */
public class ExecutionManager implements DemuxOutputReceiver {
    /** BuildEvent support used to fire events and manage listeners */
    private BuildEventSupport eventSupport = new BuildEventSupport();

    /** The Execution Frame for the top level project being executed */
    private Frame mainFrame;

    /**
     * The configuration to be used in this execution of Ant. It is formed
     * from the system, user and any runtime configs.
     */
    private AntConfig config;

    /**
     * Ant's initialization configuration with information on the location
     * of Ant and its libraries.
     */
    private InitConfig initConfig;

    /**
     * Create an ExecutionManager. When an ExecutionManager is created, it
     * loads the ant libraries which are installed in the Ant lib/task
     * directory.
     *
     * @param initConfig Ant's configuration - classloaders etc
     * @param config The user config to use - may be null
     * @exception ExecutionException if there is a problem with one of Ant's
     *      tasks
     */
    public ExecutionManager(InitConfig initConfig, AntConfig config)
         throws ExecutionException {
        this.config = config;
        this.initConfig = initConfig;
        init();
    }

    /**
     * Initialise the execution manager
     *
     * @exception ExecutionException if the standard ant libraries cannot be
     *      loaded
     */
    public void init() throws ExecutionException {
    }

    /**
     * Run a build, executing each of the targets on the given project
     *
     * @param project the project model to be used for the build
     * @param targets a list of target names to be executed.
     * @param commandProperties the properties defined by the front end to
     *      control the build
     * @exception AntException if there is a problem in the build
     */
    public void runBuild(Project project, List targets, Map commandProperties)
         throws AntException {
        Throwable buildFailureCause = null;
        try {
            
            // start by validating the project we have been given.
            project.validate();

            mainFrame = new Frame(initConfig, config);
            for (Iterator j = eventSupport.getListeners(); j.hasNext();) {
                BuildListener listener = (BuildListener) j.next();
                mainFrame.addBuildListener(listener);
            }

            mainFrame.setProject(project);
            mainFrame.setInitialProperties(commandProperties);

            eventSupport.fireBuildStarted(project);
            mainFrame.runBuild(targets);
        } catch (RuntimeException e) {
            buildFailureCause = e;
            throw e;
        } catch (ExecutionException e) {
            ExecutionException ee = e instanceof ExecutionException 
                ? e : new ExecutionException(e);
            buildFailureCause = e;
            throw ee;
        } finally {
            eventSupport.fireBuildFinished(project, buildFailureCause);
        }
    }

    /**
     * Add a build listener to the build
     *
     * @param listener the listener to be added to the build
     */
    public void addBuildListener(BuildListener listener) {
        eventSupport.addBuildListener(listener);
        if (mainFrame != null) {
            mainFrame.addBuildListener(listener);
        }
    }

    /**
     * Remove a build listener from the execution
     *
     * @param listener the listener to be removed
     */
    public void removeBuildListener(BuildListener listener) {
        eventSupport.removeBuildListener(listener);
        if (mainFrame != null) {
            mainFrame.removeBuildListener(listener);
        }
    }

    /**
     * Handle the content from a single thread. This method will be called
     * by the thread producing the content. The content is broken up into
     * separate lines
     *
     * @param line the content produce by the current thread.
     * @param isErr true if this content is from the thread's error stream.
     */
    public void threadOutput(String line, boolean isErr) {
        if (mainFrame == null) {
            eventSupport.threadOutput(line, isErr);
        } else {
            mainFrame.threadOutput(line, isErr);
        }
    }
}

