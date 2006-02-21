/*
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002, 2006 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
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
 *
 */

package org.apache.tools.ant.taskdefs.optional;
import org.apache.tools.ant.*;

import java.util.*;

import org.apache.commons.discovery.*;
import org.apache.commons.discovery.jdk.JDKHooks;
import org.apache.commons.discovery.resource.*;


/**
 * Default implementation for discovery and (lazy) creation of tasks.
 *
 * Several mechanisms will be used:
 * - properties files found in the classpath ( META-INF/ant.tasks ).
 * - resources named after the task name: META-INF/ant/[TASK_NAME].task
 *
 */
public class TaskDiscovery extends Task implements ProjectComponentFactory
{
    String RESOURCE_NAME="META-INF/ant.tasks";

    // Also discovery the 'legacy' names - in ant1.6 the initial preloaded tasks
    // should be deprecated.
    
    Resource[] discoveredTasks = null;

    Hashtable taskDefs=new Hashtable();

    public Object createProjectComponent( Project project,
                                          String ns,
                                          String taskName )
        throws BuildException
    {
        //        System.out.println("Try create " + taskName);
        // 
        return null;
    }

    public String toString() {
        StringBuffer sb=new StringBuffer();
        sb.append( "DiscoveredTasks[" );
        if( discoveredTasks != null ) {
            for( int i=0; i<discoveredTasks.length; i++ ) {
                if( i>0) sb.append( ", ");
                sb.append( discoveredTasks[i] );
            }
            sb.append( "]");
        }
        return sb.toString();
    }


    Properties taskClassNames=new Properties();

    /** @TODO: Register itself as ProjectComponentHelper.
     */
    public void execute() throws BuildException
    {
        ProjectComponentHelper pcHelper=ProjectComponentHelper.getProjectComponentHelper();
        pcHelper.addComponentFactory( this );
        
        // We'll read all 'ant.tasks' at startup, and every time an unknown task
        // is found ( the classloader may be different from last time ). Not the best
        // solution, just a start.
        DiscoverResources disc = new DiscoverResources();
        disc.addClassLoader( JDKHooks.getJDKHooks().getThreadContextClassLoader() );
        disc.addClassLoader( this.getClass().getClassLoader() );
        
        ResourceIterator ri = disc.findResources(RESOURCE_NAME);
        
        Vector vector = new Vector();
        while (ri.hasNext()) {
            Resource resourceInfo = ri.nextResource();
            vector.add(resourceInfo);
            System.out.println("Found " + resourceInfo);

            
        }
        
        discoveredTasks = new Resource[vector.size()];
        vector.copyInto(discoveredTasks);
    }
        
}
