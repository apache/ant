/* ====================================================================
 * 
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */
package org.apache.tools.ant.taskdefs.optional;

import java.io.*;
import java.text.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.types.Commandline;

/**
 * A task that fetches source files from a PVCS archive
 *
 * @author Thomas Christensen <tchristensen@nordija.com>
 */
public class Pvcs extends org.apache.tools.ant.Task {
    private String pvcsbin;
    private String repository;
    private String pvcsProject;
    private String workspace;
    private String force;
    private String promotiongroup;
    private String label;
    private boolean ignorerc;

    /**
     * Constant for the thing to execute
     */
    private static final String PCLI_EXE = "pcli";

    /**
     * Constant for the PCLI listversionedfiles recursive i a format "get" understands
     */
    private static final String PCLI_LVF_ARGS = "lvf -z -aw";

    /**
     * Constant for the thing to execute
     */
    private static final String GET_EXE = "get";


    protected int runCmd(Commandline cmd, ExecuteStreamHandler out) {
        try {
            Project aProj = getProject();
            Execute exe = new Execute(out);
            exe.setAntRun(aProj);
            exe.setWorkingDirectory(aProj.getBaseDir());
            exe.setCommandline(cmd.getCommandline());
            return exe.execute();
        }
        catch (java.io.IOException e) {
            String msg = "Failed executing: " + cmd.toString();
            throw new BuildException(msg, location);
        }
    }

    private String getExecutable(String exe) {
        StringBuffer correctedExe = new StringBuffer();
        if(getPvcsbin()!=null)
            if(pvcsbin.endsWith(File.separator))
                correctedExe.append(pvcsbin);
            else
                correctedExe.append(pvcsbin).append(File.separator);
        return correctedExe.append(exe).toString();
    }

    /**
     * @exception org.apache.tools.ant.BuildException Something is stopping the build...
     */
    public void execute() throws org.apache.tools.ant.BuildException {
        Project aProj = getProject();
        int result = 0;

        if(repository == null || repository.trim().equals("")) 
            throw new BuildException("Required argument repository not specified");

        // Check workspace exists
        // Launch PCLI listversionedfiles -z -aw
        // Capture output
        // build the command line from what we got the format is
        Commandline commandLine = new Commandline();
        commandLine.setExecutable(getExecutable(PCLI_EXE));

        commandLine.createArgument().setValue("lvf");
        commandLine.createArgument().setValue("-z");
        commandLine.createArgument().setValue("-aw");
        if(getWorkspace()!=null)
            commandLine.createArgument().setValue("-sp"+getWorkspace());
        commandLine.createArgument().setValue("-pr"+getRepository());
        if(getPvcsproject()!=null)
            commandLine.createArgument().setValue(getPvcsproject());

        File tmp;
        try {
            tmp = File.createTempFile("pvcs_ant_",".log");
            result = runCmd(commandLine, new PumpStreamHandler(new FileOutputStream(tmp), new LogOutputStream(this,Project.MSG_WARN)));
            if ( result != 0 && !ignorerc) {
                String msg = "Failed executing: " + commandLine.toString();
                throw new BuildException(msg, location);
            }

            if(!tmp.exists())
                throw new BuildException("Communication between ant and pvcs failed");
                                
                                // Create foldes in workspace
            createFolders(tmp);

                                // Launch get on output captured from PCLI lvf
            commandLine.clearArgs();
            commandLine.setExecutable(getExecutable(GET_EXE));

            if(getForce()!=null && getForce().equals("yes"))
                commandLine.createArgument().setValue("-Y");
            else
                commandLine.createArgument().setValue("-N");

            if(getPromotiongroup()!=null)
                commandLine.createArgument().setValue("-G"+getPromotiongroup());
            else {
                if(getLabel()!=null)
                    commandLine.createArgument().setValue("-r"+getLabel());
            }
            commandLine.createArgument().setValue("@"+tmp.getAbsolutePath());
            result = runCmd(commandLine, new LogStreamHandler(this,Project.MSG_INFO, Project.MSG_WARN));
            if ( result != 0 && !ignorerc) {
                String msg = "Failed executing: " + commandLine.toString();
                throw new BuildException(msg, location);
            }

        } catch(FileNotFoundException e) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(e.getMessage(),location);
        } catch(IOException e) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(e.getMessage(),location);
        } catch(ParseException e) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(e.getMessage(),location);
        }
    }

    /**
     * Parses the file and creates the folders specified in the output section
     */
    private void createFolders(File file) throws IOException, ParseException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        MessageFormat mf = new MessageFormat("{0}({1})");
        String line = in.readLine();
        while(line != null) {
            if(line.startsWith("\"")) {
                Object[] objs = mf.parse(line);
                String f = (String)objs[1];
                // Extract the name of the directory from the filename
                File dir = new File(f.substring(0,f.lastIndexOf(File.separator)));
                if(!dir.exists())
                    dir.mkdirs();
            }
            line = in.readLine();
        }
    }
         
    /**
     * Get network name of the PVCS repository
     * @return String
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Specifies the network name of the PVCS repository
     * @param repo String
     */
    public void setRepository(String repo) {
        repository = repo;
    }

    /**
     * Get name of the project in the PVCS repository
     * @return String
     */
    public String getPvcsproject() {
        return pvcsProject;
    }

    /**
     * Specifies the name of the project in the PVCS repository
     * @param prj String
     */
    public void setPvcsproject(String prj) {
        pvcsProject = prj;
    }

    /**
     * Get name of the workspace to store the retrieved files
     * @return String
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Specifies the name of the workspace to store retrieved files
     * @param ws String
     */
    public void setWorkspace(String ws) {
        workspace = ws;
    }

    /**
     * Get name of the PVCS bin directory 
     * @return String
     */
    public String getPvcsbin() {
        return pvcsbin;
    }

    /**
     * Specifies the location of the PVCS bin directory
     * @param ws String
     */
    public void setPvcsbin(String bin) {
        pvcsbin = bin;
    }

    /**
     * Get value of force
     * @return String
     */
    public String getForce() {
        return force;
    }

    /**
     * Specifies the value of the force argument
     * @param repo String (yes/no)
     */
    public void setForce(String f) {
        if(f!=null && f.equalsIgnoreCase("yes"))
            force="yes";
        else
            force = "no";
    }

    /**
     * Get value of promotiongroup
     * @return String
     */
    public String getPromotiongroup() {
        return promotiongroup;
    }

    /**
     * Specifies the name of the promotiongroup argument
     * @param repo String 
     */
    public void setPromotiongroup(String w) {
        promotiongroup=w;
    }

    /**
     * Get value of label
     * @return String
     */
    public String getLabel() {
        return label;
    }

    /**
     * Specifies the name of the label argument
     * @param repo String 
     */
    public void setLabel(String l) {
        label=l;
    }

    /**
     * Get value of ignorereturncode
     * @return String
     */
    public String getIgnoreReturnCode() {
        if(ignorerc)
            return "true";
        else
            return "false";
    }

    /**
     * If set to true the return value from executing the pvcs 
     * commands are ignored.
     * @param l String "true/false"
     */
    public void setIgnoreReturnCode(String l) {
        if(l.trim().equalsIgnoreCase("true"))
            ignorerc=true;
        else
            ignorerc=false;
    }

    /**
     * Creates a Pvcs object
     * Default PVCS project is "/"
     */
    public Pvcs() {
        super();
        pvcsProject = "/";
        workspace = null;
        repository = null;
        pvcsbin = null;
        force=null;
        promotiongroup=null;
        label=null;
        ignorerc=false;
    }
}
