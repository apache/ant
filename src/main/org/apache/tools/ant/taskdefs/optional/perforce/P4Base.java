/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.perforce;

import java.io.*;
import org.apache.tools.ant.*;
//import org.apache.tools.ant.util.regexp.*;
import org.apache.oro.text.perl.*;

/** Base class for Perforce (P4) ANT tasks. See individual task for example usage.
 *
 * @see P4Sync
 * @see P4Have
 * @see P4Change
 * @see P4Edit
 * @see P4Submit
 * @see P4Label
 * @see org.apache.tools.ant.taskdefs.Exec
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public abstract class P4Base extends org.apache.tools.ant.Task {

    /**Perl5 regexp in Java - cool eh? */
    protected Perl5Util util = null;
    /** The OS shell to use (cmd.exe or /bin/sh) */
    protected String shell;

    //P4 runtime directives
    /** Perforce Server Port (eg KM01:1666) */
    protected String P4Port     = "";
    /** Perforce Client (eg myclientspec) */
    protected String P4Client   = "";
    /** Perforce User (eg fbloggs) */
    protected String P4User     = "";
    /** Perforce view for commands (eg //projects/foobar/main/source/... ) */
    protected String P4View     = "";

    //P4 g-opts and cmd opts (rtfm)
    /** Perforce 'global' opts.
     * Forms half of low level API */
    protected String P4Opts     = "";
    /** Perforce command opts.
     * Forms half of low level API */
    protected String P4CmdOpts  = "";

    //Setters called by Ant
    public void setPort(String P4Port)        { this.P4Port       =   "-p"+P4Port;    }
    public void setClient(String P4Client)    { this.P4Client     =   "-c"+P4Client;  }
    public void setUser(String P4User)        { this.P4User       =   "-u"+P4User;    }
    public void setView(String P4View)        { this.P4View       =   P4View;         }
    public void setCmdopts(String P4CmdOpts)  { this.P4CmdOpts    =   P4CmdOpts;      }

    public void init() {

        util = new Perl5Util();

        // Not as comprehensive as Exec but Exec 
        // doesn't allow stdin and stdout/stderr processing
        
        String myOS = System.getProperty("os.name");
        if(myOS == null) throw new BuildException("Unable to determine OS");
        myOS = myOS.toLowerCase();
        
        if( myOS.indexOf("os/2") >= 0 ) {
            shell = "cmd /c ";
        } else if( myOS.startsWith("windows") 
                   && (myOS.indexOf("2000") >= 0 || myOS.indexOf("nt") >= 0 ) ) {
            shell = "cmd /c ";
        } else {
            // What about Mac OS? No perforce support there?
            shell = "/bin/sh "; //This needs testing on Unix!!!!
        }
        //Get default P4 settings from environment - Mark would have done something cool with
        //introspection here.....:-)
        String tmpprop;
        if((tmpprop = project.getProperty("p4.port")) != null) setPort(tmpprop);
        if((tmpprop = project.getProperty("p4.client")) != null) setClient(tmpprop);
        if((tmpprop = project.getProperty("p4.user")) != null) setUser(tmpprop);        
    }

    protected void execP4Command(String command) throws BuildException {
        execP4Command(command, null, null);
    }
    
    protected void execP4Command(String command, P4OutputHandler handler) throws BuildException {
        execP4Command(command, null, handler);
    }
    /** Execute P4 command assembled by subclasses.
        @param command The command to run
        @param p4input Input to be fed to command on stdin
        @param handler A P4OutputHandler to process any output
    */
    protected void execP4Command(String command, String p4input, P4OutputHandler handler) throws BuildException {
        try{

            P4Opts = P4Port+" "+P4User+" "+P4Client;
            log("Execing "+shell+"p4 "+P4Opts+" "+command, Project.MSG_VERBOSE);
            Process proc = Runtime.getRuntime().exec(shell+"p4 "+P4Opts+" "+command);

            if(p4input != null && p4input.length() >0) {
                OutputStream out = proc.getOutputStream();
                out.write(p4input.getBytes());
                out.flush();
                out.close();
            }
            
            //Q: Do we need to read p4 output if we're not interested?
            
            BufferedReader input = new BufferedReader(
                                                      new InputStreamReader(
                                                                            new SequenceInputStream(proc.getInputStream(),proc.getErrorStream())));


            //we check for a match on the input to save time on the substitution.
            String line;
            while((line = input.readLine()) != null) {
                if(handler != null) handler.process(line);
            }
                
            proc.waitFor();
            input.close();
        }catch(Exception e) {
            throw new BuildException("Problem exec'ing P4 command: "+e.getMessage());
        }
    }
}
