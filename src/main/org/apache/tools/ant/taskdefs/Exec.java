/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Executes a given command if the os platform is appropriate.
 *
 * <p><strong>As of Ant 1.2, this class is no longer the
 * implementation of Ant's &lt;exec&gt; task - it is considered to be
 * dead code by the Ant developers and is unmaintained.  Don't use
 * it.</strong></p>

 * @author duncan@x180.com
 * @author rubys@us.ibm.com
 *
 * @deprecated delegate to {@link org.apache.tools.ant.taskdefs.Execute Execute} 
 *             instead.
 */
public class Exec extends Task {
    private String os;
    private String out;
    private File dir;
    private String command;
    protected PrintWriter fos = null;
    private boolean failOnError = false;

    private static final int BUFFER_SIZE = 512;

    public Exec() {
        System.err.println("As of Ant 1.2 released in October 2000, " 
            + "the Exec class");
        System.err.println("is considered to be dead code by the Ant " 
            + "developers and is unmaintained.");
        System.err.println("Don\'t use it!");
    }

    public void execute() throws BuildException {
        run(command);
    }

    protected int run(String command) throws BuildException {

        int err = -1; // assume the worst

        // test if os match
        String myos = System.getProperty("os.name");
        log("Myos = " + myos, Project.MSG_VERBOSE);
        if ((os != null) && (os.indexOf(myos) < 0)){
            // this command will be executed only on the specified OS
            log("Not found in " + os, Project.MSG_VERBOSE);
            return 0;
        }

        // default directory to the project's base directory
        if (dir == null) {
          dir = project.getBaseDir();
        }

        if (myos.toLowerCase().indexOf("windows") >= 0) {
            if (!dir.equals(project.resolveFile("."))) {
                if (myos.toLowerCase().indexOf("nt") >= 0) {
                    command = "cmd /c cd " + dir + " && " + command;
                } else {
                    String ant = project.getProperty("ant.home");
                    if (ant == null) {
                        throw new BuildException("Property 'ant.home' not " 
                            + "found", location);
                    }
                
                    String antRun = project.resolveFile(ant + "/bin/antRun.bat").toString();
                    command = antRun + " " + dir + " " + command;
                }
            }
        } else {
            String ant = project.getProperty("ant.home");
            if (ant == null) {
              throw new BuildException("Property 'ant.home' not found", 
                location);
            }
            String antRun = project.resolveFile(ant + "/bin/antRun").toString();

            command = antRun + " " + dir + " " + command;
        }

        try {
            // show the command
            log(command, Project.MSG_VERBOSE);

            // exec command on system runtime
            Process proc = Runtime.getRuntime().exec(command);

            if (out != null)  {
                fos = new PrintWriter(new FileWriter(out));
                log("Output redirected to " + out, Project.MSG_VERBOSE);
            }

            // copy input and error to the output stream
            StreamPumper inputPumper =
                new StreamPumper(proc.getInputStream(), Project.MSG_INFO, this);
            StreamPumper errorPumper =
                new StreamPumper(proc.getErrorStream(), Project.MSG_WARN, this);

            // starts pumping away the generated output/error
            inputPumper.start();
            errorPumper.start();

            // Wait for everything to finish
            proc.waitFor();
            inputPumper.join();
            errorPumper.join();
            proc.destroy();

            // close the output file if required
            logFlush();

            // check its exit value
            err = proc.exitValue();
            if (err != 0) {
                if (failOnError) {
                    throw new BuildException("Exec returned: " + err, location);
                } else {
                    log("Result: " + err, Project.MSG_ERR);
                }
            }
        } catch (IOException ioe) {
            throw new BuildException("Error exec: " + command, ioe, location);
        } catch (InterruptedException ex) {}

        return err;
    }

    public void setDir(String d) {
        this.dir = project.resolveFile(d);
    }

    public void setOs(String os) {
        this.os = os;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setOutput(String out) {
        this.out = out;
    }

    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    protected void outputLog(String line, int messageLevel) {
        if (fos == null) {
            log(line, messageLevel); 
        } else {
            fos.println(line);
        }
    }

    protected void logFlush() {
        if (fos != null) {
          fos.close();
        }
    }

    // Inner class for continually pumping the input stream during
    // Process's runtime.
    class StreamPumper extends Thread {
        private BufferedReader din;
        private int messageLevel;
        private boolean endOfStream = false;
        private int SLEEP_TIME = 5;
        private Exec parent;

        public StreamPumper(InputStream is, int messageLevel, Exec parent) {
            this.din = new BufferedReader(new InputStreamReader(is));
            this.messageLevel = messageLevel;
            this.parent = parent;
        }

        public void pumpStream() throws IOException {
            byte[] buf = new byte[BUFFER_SIZE];
            if (!endOfStream) {
                String line = din.readLine();

                if (line != null) {
                    outputLog(line, messageLevel);
                } else {
                    endOfStream = true;
                }
            }
        }

        public void run() {
            try {
                try {
                    while (!endOfStream) {
                        pumpStream();
                        sleep(SLEEP_TIME);
                    }
                } catch (InterruptedException ie) {}
                din.close();
            } catch (IOException ioe) {}
        }
    }
}
