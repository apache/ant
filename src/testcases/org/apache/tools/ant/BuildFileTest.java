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

package org.apache.tools.ant;

import junit.framework.*;
import org.apache.tools.ant.*;
import java.io.*;

/**
 * A BuildFileTest is a TestCase which executes targets from an Ant buildfile 
 * for testing.
 * 
 * This class provides a number of utility methods for particular build file 
 * tests which extend this class. 
 * 
 * @author Nico Seessle <nico@seessle.de>
 * @author Conor MacNeill <conor@apache.org> 
 */
public abstract class BuildFileTest extends TestCase { 
    
    protected Project project;
    
    private StringBuffer logBuffer;
    private StringBuffer fullLogBuffer;
    private StringBuffer outBuffer;
    private StringBuffer errBuffer;
    private BuildException buildException;
    
    public BuildFileTest(String name) {
        super(name);
    }
    
    protected void expectBuildException(String target, String cause) { 
        expectSpecificBuildException(target, cause, null);
    }

    /**
     * Assert that the given message has been logged with a priority
     * &gt;= INFO when running the given target.
     */
    protected void expectLog(String target, String log) { 
        executeTarget(target);
        String realLog = getLog();
        assertEquals(log, realLog);
    }

    protected String getLog() { 
        return logBuffer.toString();
    }

    /**
     * Assert that the given message has been logged with a priority
     * &gt;= DEBUG when running the given target.
     */
    protected void expectDebuglog(String target, String log) { 
        executeTarget(target);
        String realLog = getFullLog();
        assertEquals(log, realLog);
    }

    protected String getFullLog() { 
        return fullLogBuffer.toString();
    }

    protected void expectOutput(String target, String output) { 
        executeTarget(target);
        String realOutput = getOutput();
        assertEquals(output, realOutput);
    }

    protected void expectOutputAndError(String target, String output, String error) { 
        executeTarget(target);
        String realOutput = getOutput();
        assertEquals(output, realOutput);
        String realError = getError();
        assertEquals(error, realError);
    }

    protected String getOutput() {
        return cleanBuffer(outBuffer);
    }
     
    protected String getError() {
        return cleanBuffer(errBuffer);
    }
        
    private String cleanBuffer(StringBuffer buffer) {
        StringBuffer cleanedBuffer = new StringBuffer();
        boolean cr = false;
        for (int i = 0; i < buffer.length(); i++) { 
            char ch = buffer.charAt(i);
            if (ch == '\r') {
                cr = true;
                continue;
            }

            if (!cr) { 
                cleanedBuffer.append(ch);
            } else { 
                if (ch == '\n') {
                    cleanedBuffer.append(ch);
                } else {
                    cleanedBuffer.append('\r').append(ch);
                }
            }
        }
        return cleanedBuffer.toString();
    }
    
    protected void configureProject(String filename) { 
        logBuffer = new StringBuffer();
        fullLogBuffer = new StringBuffer();
        project = new Project();
        project.init();
        project.setUserProperty( "ant.file" , new File(filename).getAbsolutePath() );
        project.addBuildListener(new AntTestListener());
        ProjectHelper.configureProject(project, new File(filename));
    }
    
    protected void executeTarget(String targetName) { 
        PrintStream sysOut = System.out;
        PrintStream sysErr = System.err;
        try { 
            sysOut.flush();
            sysErr.flush();
            outBuffer = new StringBuffer();
            PrintStream out = new PrintStream(new AntOutputStream());
            System.setOut(out);
            errBuffer = new StringBuffer();
            PrintStream err = new PrintStream(new AntOutputStream());
            System.setErr(err);
            logBuffer = new StringBuffer();
            fullLogBuffer = new StringBuffer();
            buildException = null;
            project.executeTarget(targetName);
        } finally { 
            System.setOut(sysOut);
            System.setErr(sysErr);
        }
        
    }
    
    protected File getProjectDir() {
        return project.getBaseDir();
    }

    protected void expectSpecificBuildException(String target, String cause, String msg) { 
        try {
            executeTarget(target);
        } catch (org.apache.tools.ant.BuildException ex) {
            if ((null != msg) && (!ex.getMessage().equals(msg))) {
                fail("Should throw BuildException because '" + cause + "' with message '" + msg + "' (actual message '" + ex.getMessage() + "' instead)");
            }
            return;
        }
        fail("Should throw BuildException because: " + cause);
    }
    
    protected void expectBuildExceptionContaining(String target, String cause, String contains) { 
        try {
            executeTarget(target);
        } catch (org.apache.tools.ant.BuildException ex) {
            if ((null != contains) && (ex.getMessage().indexOf(contains) == -1)) {
                fail("Should throw BuildException because '" + cause + "' with message containing '" + contains + "' (actual message '" + ex.getMessage() + "' instead)");
            }
            return;
        }
        fail("Should throw BuildException because: " + cause);
    }

    private class AntOutputStream extends java.io.OutputStream { 
        public void write(int b) { 
            outBuffer.append((char)b);
        }
    }
    
    private class AntTestListener implements BuildListener { 
        /**
         *  Fired before any targets are started.
         */
        public void buildStarted(BuildEvent event) {
        }

        /**
         *  Fired after the last target has finished. This event
         *  will still be thrown if an error occured during the build.
         *
         *  @see BuildEvent#getException()
         */
        public void buildFinished(BuildEvent event) {
        }

        /**
         *  Fired when a target is started.
         *
         *  @see BuildEvent#getTarget()
         */
        public void targetStarted(BuildEvent event) {
            //System.out.println("targetStarted " + event.getTarget().getName());
        }

        /**
         *  Fired when a target has finished. This event will
         *  still be thrown if an error occured during the build.
         *
         *  @see BuildEvent#getException()
         */
        public void targetFinished(BuildEvent event) {
            //System.out.println("targetFinished " + event.getTarget().getName());
        }

        /**
         *  Fired when a task is started.
         *
         *  @see BuildEvent#getTask()
         */
        public void taskStarted(BuildEvent event) {
            //System.out.println("taskStarted " + event.getTask().getTaskName());
        }

        /**
         *  Fired when a task has finished. This event will still
         *  be throw if an error occured during the build.
         *
         *  @see BuildEvent#getException()
         */
        public void taskFinished(BuildEvent event) {
            //System.out.println("taskFinished " + event.getTask().getTaskName());
        }

        /**
         *  Fired whenever a message is logged.
         *
         *  @see BuildEvent#getMessage()
         *  @see BuildEvent#getPriority()
         */
        public void messageLogged(BuildEvent event) {
            if (event.getPriority() == Project.MSG_INFO ||
                event.getPriority() == Project.MSG_WARN ||
                event.getPriority() == Project.MSG_ERR)
            {
                logBuffer.append(event.getMessage());
            }
            fullLogBuffer.append(event.getMessage());
            
        }
    }


}
