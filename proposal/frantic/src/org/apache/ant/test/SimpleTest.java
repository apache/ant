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
package org.apache.ant.test;




import org.apache.ant.AntException;
import org.apache.ant.engine.DefaultEngineListener;
import org.apache.ant.engine.TaskEngine;
import org.apache.ant.engine.TaskEngineImpl;
import org.apache.ant.tasks.Task;
import org.apache.ant.tasks.build.Project;
import org.apache.ant.tasks.build.Target;
import org.apache.ant.tasks.util.Property;
import org.apache.ant.tasks.util.PropertyDump;

public class SimpleTest {
    
    Task target;
    
    public SimpleTest() {
        try {
            Task rootTask = buildProject();
            TaskEngine engine = TaskEngineImpl.getTaskEngine();
            engine.addAntEngineListener(new DefaultEngineListener());
            engine.execute(rootTask, target);
        } catch (AntException ax) {
            ax.printStackTrace(System.err);
        }
    }
    
    protected Task buildProject() {
        Project project = new Project();
        project.setTaskName("project");
        project.setBasedir("somedir");
        project.setDefault("build");
        
        Property property1 = new Property();
        property1.setTaskName("prop1");
        property1.setName("basedir");
        property1.setValue("/org/apache");
        project.addChild(property1);
        
        Property property2 = new Property();
        property2.setTaskName("prop2");
        property2.setName("dir1");
        property2.setValue("${basedir}/ant");
        project.addChild(property2);
        
        Target target1 = new Target();
        target1.setTaskName("clean");
        project.addChild(target1);
        
        PropertyDump pd = new PropertyDump();
        pd.setTaskName("dump");
        target1.addChild(pd);
        
        Target target2 = new Target();
        target2.setTaskName("prepare");
        target2.setDepends("../clean");
        project.addChild(target2);
        
        Property property3 = new Property();
        property3.setTaskName("prop3");
        property3.setName("dir2");
        property3.setValue("${dir1}/tasks");
        target2.addChild(property3);
        
        Target target3 = new Target();
        target3.setTaskName("build");
        target3.setDepends("../prepare");
        project.addChild(target3);
        
        Property property4 = new Property();
        property4.setTaskName("prop4");
        property4.setName("dir3");
        property4.setValue("r2}");
        target3.addChild(property4);
        
        Property property5 = new Property();
        property5.setTaskName("prop5");
        property5.setName("dir4");
        property5.setValue("${di${dir3}");
        target3.addChild(property5);
        
        target = target2;
        
        return project;
    }
    
    public static void main(String[] args) {
        SimpleTest simpleTest1 = new SimpleTest();
    }
}
