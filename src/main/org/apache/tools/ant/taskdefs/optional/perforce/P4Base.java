/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.tools.ant.taskdefs.optional.perforce;

import java.io.IOException;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;


/** Base class for Perforce (P4) ANT tasks. See individual task for example usage.
 *
 * @see P4Sync
 * @see P4Have
 * @see P4Change
 * @see P4Edit
 * @see P4Submit
 * @see P4Label
 * @see org.apache.tools.ant.taskdefs.Execute
 */
public abstract class P4Base extends org.apache.tools.ant.Task {

    /**Perl5 regexp in Java - cool eh? */
    protected Perl5Util util = null;
    /** The OS shell to use (cmd.exe or /bin/sh) */
    protected String shell;

    //P4 runtime directives
    /** Perforce Server Port (eg KM01:1666) */
    protected String P4Port = "";
    /** Perforce Client (eg myclientspec) */
    protected String P4Client = "";
    /** Perforce User (eg fbloggs) */
    protected String P4User = "";
    /** Perforce view for commands. (eg //projects/foobar/main/source/... )*/
    protected String P4View = "";

    // Perforce task directives
    /** Keep going or fail on error - defaults to fail. */
    protected boolean failOnError = true;

    //P4 g-opts and cmd opts (rtfm)
    /** Perforce 'global' opts.
     * Forms half of low level API */
    protected String P4Opts = "";
    /** Perforce command opts.
     * Forms half of low level API */
    protected String P4CmdOpts = "";

    /** Set by the task or a handler to indicate that the task has failed.  BuildExceptions
     * can also be thrown to indicate failure. */
    private boolean inError = false;

    /** If inError is set, then errorMessage needs to contain the reason why. */
    private String errorMessage = "";
    /**
     * gets whether or not the task has encountered an error
     * @return error flag
     * @since ant 1.6
     */
    public boolean getInError() {
        return inError;
    }

    /**
     * sets the error flag on the task
     * @param inError if true an error has been encountered by the handler
     * @since ant 1.6
     */
    public void setInError(boolean inError) {
        this.inError = inError;
    }

    /**
     * gets the error message recorded by the Perforce handler
     * @return error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * sets the error message
     * @param errorMessage line of error output
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    //Setters called by Ant

    /**
     * The p4d server and port to connect to;
     * optional, default "perforce:1666"
     *
     * @param P4Port the port one wants to set such as localhost:1666
     */
    public void setPort(String P4Port) {
        this.P4Port = "-p" + P4Port;
    }

    /**
     * The p4 client spec to use;
     * optional, defaults to the current user
     *
     * @param P4Client the name of the Perforce client spec
     */
    public void setClient(String P4Client) {
        this.P4Client = "-c" + P4Client;
    }

    /**
     * The p4 username;
     * optional, defaults to the current user
     *
     * @param P4User the user name
     */
    public void setUser(String P4User) {
        this.P4User = "-u" + P4User;
    }
    /**
     * Set global P4 options; Used on all
     * of the Perforce tasks.
     *
     * @param P4Opts global options, to use a specific P4Config file for instance
     */
    public void setGlobalopts(String P4Opts) {
        this.P4Opts = P4Opts;
    }
    /**
     * The client, branch or label view to operate upon;
     * optional default "//...".
     *
     * the view is required for the following tasks :
     * <ul>
     * <li>p4delete</li>
     * <li>p4edit</li>
     * <li>p4reopen</li>
     * <li>p4resolve</li>
     * </ul>
     *
     * @param P4View the view one wants to use
     */
    public void setView(String P4View) {
        this.P4View = P4View;
    }

    /**
     * Set extra command options; only used on some
     * of the Perforce tasks.
     *
     * @param P4CmdOpts  command line options going after the particular
     * Perforce command
     */
    public void setCmdopts(String P4CmdOpts) {
        this.P4CmdOpts = P4CmdOpts;
    }

    /**
     * whether to stop the build (true, default)
     * or keep going if an error is returned from the p4 command
     * @param fail indicates whether one wants to fail the build if an error comes from the
     * Perforce command
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }
    /**
     *  sets attributes Port, Client, User from properties
     *  if these properties are defined.
     *  Called automatically by UnknownElement
     *  @see org.apache.tools.ant.UnknownElement
     *  <table>
     *  <tr><th>Property</th><th>Attribute</th></tr>
     *  <tr><td>p4.port</td><td>Port</td></tr>
     *  <tr><td>p4.client</td><td>Client</td></tr>
     *  <tr><td>p4.user</td><td>User</td></tr>
     *  </table>
     */
    public void init() {

        util = new Perl5Util();

        //Get default P4 settings from environment - Mark would have done something cool with
        //introspection here.....:-)
        String tmpprop;
        if ((tmpprop = getProject().getProperty("p4.port")) != null) {
            setPort(tmpprop);
        }
        if ((tmpprop = getProject().getProperty("p4.client")) != null) {
            setClient(tmpprop);
        }
        if ((tmpprop = getProject().getProperty("p4.user")) != null) {
            setUser(tmpprop);
        }
    }
    /**
    *  no usages found for this method
    *  runs a Perforce command without a handler
    * @param command the command that one wants to execute
    * @throws BuildException if failonerror is set and the command fails
    */
    protected void execP4Command(String command) throws BuildException {
        execP4Command(command, null);
    }

    /**
     * Execute P4 command assembled by subclasses.
     *
     * @param command The command to run
     * @param handler A P4Handler to process any input and output
     *
     * @throws BuildException if failonerror has been set to true
     */
    protected void execP4Command(String command, P4Handler handler) throws BuildException {
        try {
            // reset error flags before executing the command
            inError = false;
            errorMessage = "";
            Commandline commandline = new Commandline();
            commandline.setExecutable("p4");

            //Check API for these - it's how CVS does it...
            if (P4Port != null && P4Port.length() != 0) {
                commandline.createArgument().setValue(P4Port);
            }
            if (P4User != null && P4User.length() != 0) {
                commandline.createArgument().setValue(P4User);
            }
            if (P4Client != null && P4Client.length() != 0) {
                commandline.createArgument().setValue(P4Client);
            }
            if (P4Opts != null && P4Opts.length() != 0) {
                commandline.createArgument().setLine(P4Opts);
            }
            commandline.createArgument().setLine(command);

            log(commandline.describeCommand(), Project.MSG_VERBOSE);

            if (handler == null) {
                handler = new SimpleP4OutputHandler(this);
            }

            Execute exe = new Execute(handler, null);

            exe.setAntRun(getProject());

            exe.setCommandline(commandline.getCommandline());

            try {
                exe.execute();

                if (inError && failOnError) {
                    throw new BuildException(errorMessage);
                }
            } catch (IOException e) {
                throw new BuildException(e);
            } finally {
                try {
                    handler.stop();
                } catch (Exception e) {
                    log(e.toString(), Project.MSG_ERR);
                }
            }


        } catch (Exception e) {
            String failMsg = "Problem exec'ing P4 command: " + e.getMessage();
            if (failOnError) {
                throw new BuildException(failMsg);
            } else {
                log(failMsg, Project.MSG_ERR);
            }

        }
    }
}
