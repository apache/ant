/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.*;

import java.io.PrintStream;

/**
 * Jakarta Commons Logging listener.
 * Note: do not use the SimpleLog as your logger implementation as it
 * causes an infinite loop since it writes to System.err, which Ant traps
 * and reroutes to the logger/listener layer.
 *
 * The following names are used for the log:
 *  org.apache.tools.ant.Project.PROJECT_NAME  - for project events
 *  org.apache.tools.ant.Target.TARGET_NAME - for target events
 *  TASK_CLASS_NAME.TARGET_NAME - for events in individual targets.
 *
 * In all target and project names we replace "." and " " with "-".
 *
 * TODO: we should use the advanced context logging features ( and expose them
 * in c-l first :-)
 * TODO: this is _very_ inefficient. Switching the out and tracking the logs
 * can be optimized a lot - but may require few more changes to the core.
 *
 * @author Erik Hatcher
 * @since Ant 1.5
 */
public class CommonsLoggingListener implements BuildListener, BuildLogger {

    /** Indicates if the listener was initialized. */
    private boolean initialized = false;

    private LogFactory logFactory;

    /**
     * Construct the listener and make sure that a LogFactory
     * can be obtained.
     */
    public CommonsLoggingListener() {
    }

    private Log getLog( String cat, String suffix ) {
        if( suffix != null ) {
            suffix=suffix.replace('.', '-');
            suffix=suffix.replace(' ', '-');
            cat=cat + "." + suffix;
        }
        PrintStream tmpOut=System.out;
        PrintStream tmpErr=System.err;
        System.setOut( out );
        System.setErr( err );

        if( ! initialized ) {
            try {
                logFactory = LogFactory.getFactory();
            } catch (LogConfigurationException e) {
                e.printStackTrace(System.err);
                return null;
            }
        }

        initialized = true;
        Log log=logFactory.getInstance(cat);
        System.setOut( tmpOut );
        System.setErr( tmpErr );
        return log;
    }

    /**
     * @see BuildListener#buildStarted
     */
    public void buildStarted(BuildEvent event) {
        String categoryString= "org.apache.tools.ant.Project";
        Log log=getLog(categoryString, null);

        if (initialized) {
            realLog( log, "Build started.", Project.MSG_INFO, null);
        }
    }

    /**
     * @see BuildListener#buildFinished
     */
    public void buildFinished(BuildEvent event) {
        if (initialized) {
            String categoryString= "org.apache.tools.ant.Project";
            Log log=getLog(categoryString, event.getProject().getName());

            if (event.getException() == null) {
                realLog( log, "Build finished.", Project.MSG_INFO, null);
            } else {
                realLog( log, "Build finished with error.", Project.MSG_ERR,
                        event.getException());
            }
        }
    }

    /**
     * @see BuildListener#targetStarted
     */
    public void targetStarted(BuildEvent event) {
        if (initialized) {
            Log log = getLog("org.apache.tools.ant.Target",
                    event.getTarget().getName() );
            // Since task log category includes target, we don't really
            // need this message
            realLog( log, "Start: " + event.getTarget().getName(),
                    Project.MSG_DEBUG, null);
        }
    }

    /**
     * @see BuildListener#targetFinished
     */
    public void targetFinished(BuildEvent event) {
        if (initialized) {
            String targetName = event.getTarget().getName();
            Log log = getLog("org.apache.tools.ant.Target",
                    event.getTarget().getName() );
            if (event.getException() == null) {
                realLog(log, "Target end: " + targetName, Project.MSG_DEBUG, null);
            } else {
                realLog(log, "Target \"" + targetName
                        + "\" finished with error.", Project.MSG_ERR,
                        event.getException());
            }
        }
    }

    /**
     * @see BuildListener#taskStarted
     */
    public void taskStarted(BuildEvent event) {
        if (initialized) {
            Task task = event.getTask();
            Object real=task;
            if( task instanceof UnknownElement ) {
                Object realObj=((UnknownElement)task).getObject();
                if( realObj!=null ) {
                    real=realObj;
                }
            }
            Log log = getLog(real.getClass().getName(), null);
            if( log.isTraceEnabled()) {
                realLog( log, "Task \"" + task.getTaskName() + "\" started ",
                        Project.MSG_VERBOSE, null);
            }
        }
    }

    /**
     * @see BuildListener#taskFinished
     */
    public void taskFinished(BuildEvent event) {
        if (initialized) {
            Task task = event.getTask();
            Object real=task;
            if( task instanceof UnknownElement ) {
                Object realObj=((UnknownElement)task).getObject();
                if( realObj!=null ) {
                    real=realObj;
                }
            }
            Log log = getLog(real.getClass().getName(), null);
            if (event.getException() == null) {
                if( log.isTraceEnabled() ) {
                    realLog( log, "Task \"" + task.getTaskName() + "\" finished.",
                            Project.MSG_VERBOSE, null);
                }
            } else {
                realLog( log, "Task \"" + task.getTaskName()
                        + "\" finished with error.", Project.MSG_ERR,
                        event.getException());
            }
        }
    }


    /**
     * @see BuildListener#messageLogged
     */
    public void messageLogged(BuildEvent event) {
        if (initialized) {
            Object categoryObject = event.getTask();
            String categoryString=null;
            String categoryDetail=null;

            if (categoryObject == null) {
                categoryObject = event.getTarget();
                if (categoryObject == null) {
                    categoryObject = event.getProject();
                    categoryString="org.apache.tools.ant.Project";
                    categoryDetail=event.getProject().getName();
                } else {
                    categoryString= "org.apache.tools.ant.Target";
                    categoryDetail=event.getTarget().getName();
                }
            } else {
                // It's a task - append the target
                if( event.getTarget() != null ) {
                    categoryString=categoryObject.getClass().getName();
                    categoryDetail=event.getTarget().getName();
                } else {
                    categoryString=categoryObject.getClass().getName();
                }

            }

            Log log = getLog(categoryString, categoryDetail);
            int priority=event.getPriority();
            String message=event.getMessage();
            realLog( log, message, priority , null);
        }
    }

    private void realLog( Log log, String message, int priority, Throwable t )
    {
        PrintStream tmpOut=System.out;
        PrintStream tmpErr=System.err;
        System.setOut( out );
        System.setErr( err );
        switch (priority) {
            case Project.MSG_ERR:
                if( t==null ) {
                    log.error(message);
                } else {
                    log.error( message,t );
                }
                break;
            case Project.MSG_WARN:
                if( t==null ) {
                    log.warn(message);
                } else {
                    log.warn( message,t );
                }
                break;
            case Project.MSG_INFO:
                if( t==null ) {
                    log.info(message);
                } else {
                    log.info( message,t );
                }
                break;
            case Project.MSG_VERBOSE:
                log.debug(message);
                break;
            case Project.MSG_DEBUG:
                log.debug(message);
                break;
            default:
                log.error(message);
                break;
        }
        System.setOut( tmpOut );
        System.setErr( tmpErr );
    }

    PrintStream out;
    PrintStream err;

    public void setMessageOutputLevel(int level) {
        // Use the logger config
    }

    public void setOutputPrintStream(PrintStream output) {
        this.out = output;
    }

    public void setEmacsMode(boolean emacsMode) {
        // Doesn't make sense for c-l. Use the logger config
    }

    public void setErrorPrintStream(PrintStream err) {
        this.err=err;
    }

}
