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
package org.apache.ant.antlib.monitor;

import org.apache.ant.common.antlib.AbstractAspect;
import org.apache.ant.common.antlib.Task;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.model.BuildElement;
import org.apache.ant.common.model.NamespaceValueCollection;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.event.MessageLevel;
import java.util.Date;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A monitoring aspect to help understand memory and performance
 * characteristics.
 *
 * @author Conor MacNeill
 */
public class MonitorAspect extends AbstractAspect {
    /** The log into which monitoring info is written */
    private static PrintStream monitorLog;

    /**
     * Create a monitoring aspect instance. There will be one instance per
     * active frame but they all share the same log output.
     */
    public MonitorAspect() {
        synchronized (MonitorAspect.class) {
            if (monitorLog == null) {
                try {
                    monitorLog
                        = new PrintStream(new FileOutputStream("monitor.log"));
                    monitorLog.println("Logging started at " + new Date());
                } catch (IOException e) {
                    log("Unable to open monitor log", MessageLevel.MSG_WARN);
                }
            }
        }
    }

    /**
     * This join point is activated just prior to task execution.
     *
     * @param task the task being executed.
     * @param namespaceValues a collection of namesapce attribute values for use
     *        during the task execution - may be null if no namespace values are
     *        provided.
     * @return an object which indicates that this aspect wishes to
     * be notified after execution has been completed, in which case the obkect
     * is returned to provide the aspect its context. If this returns null
     * the aspect's postExecuteTask method will not be invoked.
     * @exception AntException if the aspect cannot process the task.
     */
    public Object preExecuteTask(Task task,
                                 NamespaceValueCollection namespaceValues)
         throws AntException {
        System.gc();
        AntContext taskContext = task.getAntContext();
        BuildElement model = taskContext.getModel();
        String name = (model == null) ? task.getClass().getName()
            : model.getType();
        return new MonitorRecord(name, taskContext.getLocation());
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
        MonitorRecord record = (MonitorRecord) context;
        System.gc();
        record.print(monitorLog);
        return failureCause;
    }
}
