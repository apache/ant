/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.ant.core.execution;


import org.apache.ant.core.model.*;
import org.apache.ant.core.support.*;
import java.util.*;

/**
 * The ExecutionManager manages the execution of Ant. It will create
 * ExecutionFrames to handle the various imported projects, the 
 * data values associated with those projects. Before the ExecutionManager
 * can be used, it must be initialised with a set of Ant libraries. These 
 * will contain task definitions, aspect definitions, etc.
 * 
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public class ExecutionManager implements BuildListener {
    private static final String VISITING = "VISITING";
    private static final String VISITED = "VISITED";

    private HashMap taskDefs = new HashMap();
    private HashMap converterDefs = new HashMap();
    private Project project;
    private ExecutionFrame mainFrame;
    private Map namespaceMap = null;
    private List frameInitOrder = null;
    
    private BuildEventSupport eventSupport = new BuildEventSupport();
    
    public void addBuildListener(BuildListener listener) {
        eventSupport.addBuildListener(listener);
    }
    
    public void removeBuildListener(BuildListener listener) {
        eventSupport.removeBuildListener(listener);
    }

    /** 
     * Forward any events to our listeners.
     */
    public void processBuildEvent(BuildEvent event) {
        eventSupport.forwardEvent(event);
    }

    public void addLibraries(AntLibrary[] libraries) throws ConfigException {
        for (int i = 0; i < libraries.length; ++i) {
            addLibrary(libraries[i]);
        }
    }
    
    public void addLibrary(AntLibrary library) throws ConfigException {
        for (Iterator i = library.getTaskDefinitions(); i.hasNext(); ) {
            TaskDefinition taskDefinition = (TaskDefinition)i.next();
            addTaskDefinition(taskDefinition);
        }
        for (Iterator i = library.getConverterDefinitions(); i.hasNext(); ) {
            ConverterDefinition converterDef = (ConverterDefinition)i.next();
            addConverterDefinition(converterDef);
        }
    }

    public void addTaskDefinition(TaskDefinition taskDefinition) throws ConfigException {
        String taskName = taskDefinition.getName();
        if (taskDefs.containsKey(taskName)) {
            String message = "Task " + taskName + " is defined twice" +
                             ", in " + ((TaskDefinition)taskDefs.get(taskName)).getLibraryURL() +
                             " and " + taskDefinition.getLibraryURL();
            throw new ConfigException(message, new Location(taskDefinition.getLibraryURL().toString()));
        }
        taskDefs.put(taskName, taskDefinition);
    }
    
    public void addConverterDefinition(ConverterDefinition converterDef) throws ConfigException {
        String targetClassname = converterDef.getTargetClassName();
        if (converterDefs.containsKey(targetClassname)) {
            String message = "Converter for " + targetClassname + " is defined twice" +
                             ", in " + ((ConverterDefinition)converterDefs.get(targetClassname)).getLibraryURL() +
                             " and " + converterDef.getLibraryURL();
            throw new ConfigException(message, new Location(converterDef.getLibraryURL().toString()));
        }
        converterDefs.put(targetClassname, converterDef);
    }
    
    public void setProject(Project project) throws ConfigException {
        this.project = project;
        mainFrame = new ExecutionFrame(project, (Map)taskDefs.clone(), 
                                       (Map)converterDefs.clone(), null);
        namespaceMap = new HashMap();
        frameInitOrder = new ArrayList();
        setupFrame(mainFrame);
        
        // We iterate through all nodes of all projects and make sure every node is OK
        Map state = new HashMap();
        Stack visiting = new Stack();
        List dependencyOrder = new ArrayList();

        checkFrameTargets(mainFrame, dependencyOrder, state, visiting);
    }

    /**
     * Check whether the targets in the given execution frame and its subframes are OK
     */
    private void checkFrameTargets(ExecutionFrame frame, List dependencyOrder, 
                                   Map state, Stack visiting) 
            throws ConfigException {
        // get the targets and just iterate through them.
        for (Iterator i = frame.getProject().getTargets(); i.hasNext();) {
            Target target = (Target)i.next();
            fillinDependencyOrder(frame, target.getName(),
                                  dependencyOrder, state, visiting);
        }
        
        // Now do the subframes.
        for (Iterator i = frame.getImportedFrames(); i.hasNext();) {
            ExecutionFrame importedFrame = (ExecutionFrame)i.next();
            checkFrameTargets(importedFrame, dependencyOrder, state, visiting);
        }
    }
    
    private void setupFrame(ExecutionFrame frame) {
        frame.addBuildListener(this);
        
        String namespace = frame.getNamespace();
        if (namespace != null) {
            namespaceMap.put(namespace, frame);
        }
        for (Iterator i = frame.getImportedFrameNames(); i.hasNext();) {
            String importName = (String)i.next();
            setupFrame(frame.getImportedFrame(importName));
        }
        frameInitOrder.add(frame);
    }
    
    public void runBuild(List targetNames) throws AntException {
        Throwable buildFailureCause = null;
        try {
            eventSupport.fireBuildStarted(this, project);
            // we initialise each execution frame
            for (Iterator i = frameInitOrder.iterator(); i.hasNext();) {
                ExecutionFrame frame = (ExecutionFrame)i.next();
                frame.initialise();
            }
            if (targetNames.isEmpty()) {
                // we just execute the default target if any
                String defaultTarget = project.getDefaultTarget();
                if (defaultTarget != null) {
                    executeTarget(defaultTarget);
                }
            }
            else {
                for (Iterator i = targetNames.iterator(); i.hasNext();) {
                    executeTarget((String)i.next());
                }
            }
            eventSupport.fireBuildFinished(this, project, null);
        }
        catch (RuntimeException e) {
            buildFailureCause = e;
            throw e;
        }
        catch (AntException e) {
            buildFailureCause = e;
            throw e;
        }
        finally {
            eventSupport.fireBuildFinished(this, project, buildFailureCause);
        }
    }

    private ExecutionFrame getFrame(String name) {
        int namespaceIndex = name.lastIndexOf(":");
        if (namespaceIndex == -1) {
            return mainFrame;
        }
        return (ExecutionFrame)namespaceMap.get(name.substring(0, namespaceIndex));
    }
    
    public void executeTarget(String targetName) throws ExecutionException, ConfigException {
        // to execute a target we must determine its dependencies and 
        // execute them in order.
        Map state = new HashMap();
        Stack visiting = new Stack();
        List dependencyOrder = new ArrayList();
        ExecutionFrame startingFrame = getFrame(targetName);
        fillinDependencyOrder(startingFrame, startingFrame.getRelativeName(targetName),
                              dependencyOrder, state, visiting);

        // Now tell each frame to execute the target
        for (Iterator i = dependencyOrder.iterator(); i.hasNext();) {
            String fullTargetName = (String)i.next();
            ExecutionFrame frame = getFrame(fullTargetName);
            frame.executeTargetTasks(frame.getRelativeName(fullTargetName));
        }
    }
    
    private void fillinDependencyOrder(ExecutionFrame frame, String targetName, 
                                       List dependencyOrder, Map state,
                                       Stack visiting) throws ConfigException {
        String fullTargetName = frame.getQualifiedName(targetName); 
        if (state.get(fullTargetName) == VISITED) {
            return;
        }
        Target target = frame.getProject().getTarget(targetName);                                       
        if (target == null) {
            StringBuffer sb = new StringBuffer("Target `");
            sb.append(targetName);
            sb.append("' does not exist in this project. ");
            if (!visiting.empty()) {
                String parent = (String)visiting.peek();
                sb.append("It is used from target `");
                sb.append(parent);
                sb.append("'.");
            }

            throw new ConfigException(new String(sb), frame.getProject().getLocation());
        }
        
        state.put(fullTargetName, VISITING);
        visiting.push(fullTargetName);
        for (Iterator i = target.getDependencies(); i.hasNext(); ) {
            String dependency = (String)i.next();
            String fullyQualifiedName = frame.getQualifiedName(dependency);
            ExecutionFrame dependencyFrame = getFrame(fullyQualifiedName);
            if (dependencyFrame == null) {
                StringBuffer sb = new StringBuffer("Target `");
                sb.append(dependency);
                sb.append("' does not exist in this project. ");
                throw new ConfigException(new String(sb), target.getLocation());
            }
            
            String dependencyState = (String)state.get(fullyQualifiedName);
            if (dependencyState == null) {
                fillinDependencyOrder(dependencyFrame, dependencyFrame.getRelativeName(fullyQualifiedName),
                                      dependencyOrder, state, visiting);
            }
            else if (dependencyState == VISITING) {
                String circleDescription
                    = getCircularDesc(dependency, visiting);
                throw new ConfigException(circleDescription, target.getLocation());
            }
        }
        
        state.put(fullTargetName, VISITED);
        String poppedNode = (String)visiting.pop();
        if (poppedNode != fullTargetName) {
            throw new ConfigException("Problem determining dependencies " + 
                                        " - expecting '" + fullTargetName + 
                                        "' but got '" + poppedNode + "'");
        }
        dependencyOrder.add(fullTargetName);                                        
    }
                                
    private String getCircularDesc(String end, Stack visitingNodes) {
        StringBuffer sb = new StringBuffer("Circular dependency: ");
        sb.append(end);
        String c;
        do {
            c = (String)visitingNodes.pop();
            sb.append(" <- ");
            sb.append(c);
        } while(!c.equals(end));
        return new String(sb);
    }
}
