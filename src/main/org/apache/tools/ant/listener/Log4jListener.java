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

package org.apache.tools.ant.listener;

import org.apache.tools.ant.*;

import org.apache.log4j.Category;
import org.apache.log4j.helpers.NullEnumeration;

  
/**
 *  Listener which sends events to Log4j logging system
 *
 * @author <a href="mailto:conor@apache.org>Conor MacNeill </a>
 */
public class Log4jListener implements BuildListener {
    static final String LOG4J_CONFIG_PROPERTY = "log4j.configuration";
    
    private boolean initialized = false;
    
    public Log4jListener() {
        initialized = false;
        Category cat = Category.getInstance("org.apache.tools.ant");
        Category rootCat = Category.getRoot();
        if (!(rootCat.getAllAppenders() instanceof NullEnumeration)) {
            initialized = true;
        }
        else {
            cat.error("No log4j.properties in build area");
        }
    }
    
    public void buildStarted(BuildEvent event) {
        if (initialized) {
            Category cat = Category.getInstance(Project.class.getName());
            cat.info("Build started.");
        }
    }
    
    public void buildFinished(BuildEvent event) {
        if (initialized) {
            Category cat = Category.getInstance(Project.class.getName());
            if (event.getException() == null) {
                cat.info("Build finished.");
            }
            else {
                cat.error("Build finished with error.", event.getException());
            }
        }   
    }
    
    public void targetStarted(BuildEvent event) {
        if (initialized) {
            Category cat = Category.getInstance(Target.class.getName());
            cat.info("Target \"" + event.getTarget().getName() + "\" started.");
        }
    }
    
    public void targetFinished(BuildEvent event) {
        if (initialized) {
            String targetName = event.getTarget().getName();
            Category cat = Category.getInstance(Target.class.getName());
            if (event.getException() == null) {
                cat.info("Target \"" + event.getTarget().getName() + "\" finished.");
            }
            else {
                cat.error("Target \"" + event.getTarget().getName() + "\" finished with error.", event.getException());
            }
        } 
    }
    
    public void taskStarted(BuildEvent event) {
        if (initialized) {
            Task task = event.getTask();
            Category cat = Category.getInstance(task.getClass().getName());
            cat.info("Task \"" + task.getTaskName() + "\" started.");
        }
    }
    
    public void taskFinished(BuildEvent event) {
        if (initialized) {
            Task task = event.getTask();
            Category cat = Category.getInstance(task.getClass().getName());
            if (event.getException() == null) {
                cat.info("Task \"" + task.getTaskName() + "\" finished.");
            }
            else {
                cat.error("Task \"" + task.getTaskName() + "\" finished with error.", event.getException());
            }
        }
    }
    
    public void messageLogged(BuildEvent event) {
        if (initialized) {
            Object categoryObject = event.getTask();
            if (categoryObject == null) {
                categoryObject = event.getTarget();
                if (categoryObject == null) {
                    categoryObject = event.getProject();
                }
            }
            
            Category cat = Category.getInstance(categoryObject.getClass().getName());
            switch (event.getPriority()) {
                case Project.MSG_ERR:
                    cat.error(event.getMessage());
                    break;
                case Project.MSG_WARN:
                    cat.warn(event.getMessage());
                    break;
                case Project.MSG_INFO:
                    cat.info(event.getMessage());
                    break;
                case Project.MSG_VERBOSE:
                    cat.debug(event.getMessage());
                    break;
                case Project.MSG_DEBUG:
                    cat.debug(event.getMessage());
                    break;
                default:                        
                    cat.error(event.getMessage());
                    break;
            }
        }
    }
}
