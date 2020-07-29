/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Executes a given command if the os platform is appropriate.
 *
 * <p><strong>As of Ant 1.2, this class is no longer the
 * implementation of Ant's &lt;exec&gt; task - it is considered to be
 * dead code by the Ant developers and is unmaintained.  Don't use
 * it.</strong></p>
 *
 * @deprecated since 1.2.
 *             delegate to {@link org.apache.tools.ant.taskdefs.Execute Execute}
 *             instead.
 */
@Deprecated
public class Exec extends Task {
    private String os;
    private String out;
    private File dir;
    private String command;
    // CheckStyle:VisibilityModifier OFF - bc
    protected PrintWriter fos = null;
    // CheckStyle:VisibilityModifier ON
    private boolean failOnError = false;

    /**
     * Constructor for Exec.
     * Prints a warning message to std error.
     */
    public Exec() {
        System.err.println("As of Ant 1.2 released in October 2000, "
            + "the Exec class");
        System.err.println("is considered to be dead code by the Ant "
            + "developers and is unmaintained.");
        System.err.println("Don't use it!");
    }

    /**
     * Execute the task.
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        run(command);
    }

    /**
     * Execute the command.
     * @param command the command to exec
     * @return the exit value of the command
     * @throws BuildException on error
     */
    protected int run(String command) throws BuildException {

        int err = -1; // assume the worst

        // test if os match
        String myos = System.getProperty("os.name");
        log("Myos = " + myos, Project.MSG_VERBOSE);
        if (os != null && !os.contains(myos)) {
            // this command will be executed only on the specified OS
            log("Not found in " + os, Project.MSG_VERBOSE);
            return 0;
        }

        // default directory to the project's base directory
        if (dir == null) {
          dir = getProject().getBaseDir();
        }

        if (myos.toLowerCase(Locale.ENGLISH).contains("windows")) {
            if (!dir.equals(getProject().resolveFile("."))) {
                if (myos.toLowerCase(Locale.ENGLISH).contains("nt")) {
                    command = "cmd /c cd " + dir + " && " + command;
                } else {
                    String ant = getProject().getProperty(MagicNames.ANT_HOME);
                    if (ant == null) {
                        throw new BuildException("Property '" + MagicNames.ANT_HOME + "' not "
                            + "found", getLocation());
                    }

                    String antRun = getProject().resolveFile(ant + "/bin/antRun.bat").toString();
                    command = antRun + " " + dir + " " + command;
                }
            }
        } else {
            String ant = getProject().getProperty(MagicNames.ANT_HOME);
            if (ant == null) {
              throw new BuildException("Property '" + MagicNames.ANT_HOME + "' not found",
                                       getLocation());
            }
            String antRun = getProject().resolveFile(ant + "/bin/antRun").toString();

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
                new StreamPumper(proc.getInputStream(), Project.MSG_INFO);
            StreamPumper errorPumper =
                new StreamPumper(proc.getErrorStream(), Project.MSG_WARN);

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
                    throw new BuildException("Exec returned: " + err, getLocation());
                }
                log("Result: " + err, Project.MSG_ERR);
            }
        } catch (IOException ioe) {
            throw new BuildException("Error exec: " + command, ioe, getLocation());
        } catch (InterruptedException ex) {
            //ignore
        }

        return err;
    }

    /**
     * Set the directory.
     * @param d a <code>String</code> value
     */
    public void setDir(String d) {
        this.dir = getProject().resolveFile(d);
    }

    /**
     * Set the Operating System that this exec is to run in.
     * @param os a <code>String</code> value
     */
    public void setOs(String os) {
        this.os = os;
    }

    /**
     * Set the command to exec.
     * @param command a <code>String</code> value
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * Set the output filename.
     * @param out a <code>String</code> value
     */
    public void setOutput(String out) {
        this.out = out;
    }

    /**
     * Set the failOnError attribute.
     * Default is false.
     * @param fail a <code>boolean</code> value
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    /**
     * Log an output message.
     * @param line the line to putput
     * @param messageLevel the level of logging - ignored
     *                     if output is going to a file
     */
    protected void outputLog(String line, int messageLevel) {
        if (fos == null) {
            log(line, messageLevel);
        } else {
            fos.println(line);
        }
    }

    /**
     * Close output.
     */
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
        private static final int SLEEP_TIME = 5;

        public StreamPumper(InputStream is, int messageLevel) {
            this.din = new BufferedReader(new InputStreamReader(is));
            this.messageLevel = messageLevel;
        }

        public void pumpStream() throws IOException {
            if (!endOfStream) {
                String line = din.readLine();

                if (line != null) {
                    outputLog(line, messageLevel);
                } else {
                    endOfStream = true;
                }
            }
        }

        @Override
        public void run() {
            try {
                try {
                    while (!endOfStream) {
                        pumpStream();
                        sleep(SLEEP_TIME);
                    }
                } catch (InterruptedException ie) {
                    //ignore
                }
                din.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
    }
}
