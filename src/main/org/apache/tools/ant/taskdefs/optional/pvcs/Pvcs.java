/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.pvcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.types.Commandline;

/**
 *
 * Extracts the latest edition of the source code from a PVCS repository.
 * PVCS is a version control system
 * developed by <a href="http://www.merant.com/products/pvcs">Merant</a>.
 * <br>
 * Before using this tag, the user running ant must have access to the commands 
 * of PVCS (get and pcli) and must have access to the repository. Note that the way to specify
 * the repository is platform dependent so use property to specify location of repository.
 * <br>
 * This version has been tested agains PVCS version 6.5 and 6.6 under Windows and Solaris.
 
 *
 * <b>19-04-2001</b> <p>The task now has a more robust
 * parser. It allows for platform independant file paths
 * and supports file names with <i>()</i>. Thanks to Erik Husby for
 * bringing the bug to my attention.
 *
 * <b>27-04-2001</b> <p>UNC paths are now handled properly.
 * Fix provided by Don Jeffery. He also added an <i>UpdateOnly</i> flag
 * that, when true, conditions the PVCS get using the -U option to only
 * update those files that have a modification time (in PVCS) that is newer
 * than the existing workfile.
 *
 * @author <a href="mailto:tchristensen@nordija.com">Thomas Christensen</a>
 * @author <a href="mailto:donj@apogeenet.com">Don Jeffery</a>
 * @author <a href="mailto:snewton@standard.com">Steven E. Newton</a>
 */
public class Pvcs extends org.apache.tools.ant.Task {
    private String pvcsbin;
    private String repository;
    private String pvcsProject;
    private Vector pvcsProjects;
    private String workspace;
    private String force;
    private String promotiongroup;
    private String label;
    private boolean ignorerc;
    private boolean updateOnly;
    private String filenameFormat;
    private String lineStart;
    private String userId;
    /**
     * Constant for the thing to execute
     */
    private static final String PCLI_EXE = "pcli";

    /*
     * Constant for the PCLI listversionedfiles recursive i a format "get" understands
     */
    // private static final String PCLI_LVF_ARGS = "lvf -z -aw";

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
        } catch (java.io.IOException e) {
            String msg = "Failed executing: " + cmd.toString()
                + ". Exception: " + e.getMessage();
            throw new BuildException(msg, getLocation());
        }
    }

    private String getExecutable(String exe) {
        StringBuffer correctedExe = new StringBuffer();
        if (getPvcsbin() != null) {
            if (pvcsbin.endsWith(File.separator)) {
                correctedExe.append(pvcsbin);
            } else {
                correctedExe.append(pvcsbin).append(File.separator);
            }
        }
        return correctedExe.append(exe).toString();
    }

    /**
     * @exception org.apache.tools.ant.BuildException Something is stopping the build...
     */
    public void execute() throws org.apache.tools.ant.BuildException {
        int result = 0;

        if (repository == null || repository.trim().equals("")) {
            throw new BuildException("Required argument repository not specified");
        }

        // Check workspace exists
        // Launch PCLI listversionedfiles -z -aw
        // Capture output
        // build the command line from what we got the format is
        Commandline commandLine = new Commandline();
        commandLine.setExecutable(getExecutable(PCLI_EXE));

        commandLine.createArgument().setValue("lvf");
        commandLine.createArgument().setValue("-z");
        commandLine.createArgument().setValue("-aw");
        if (getWorkspace() != null) {
            commandLine.createArgument().setValue("-sp" + getWorkspace());
        }
        commandLine.createArgument().setValue("-pr" + getRepository());

        String uid = getUserId();

        if (uid != null) {
            commandLine.createArgument().setValue("-id" + uid);
        }

        // default pvcs project is "/"
        if (getPvcsproject() == null && getPvcsprojects().isEmpty()) {
			pvcsProject = "/";
        }

        if (getPvcsproject() != null) {
            commandLine.createArgument().setValue(getPvcsproject());
        }
        if (!getPvcsprojects().isEmpty()) {
            Enumeration e = getPvcsprojects().elements();
            while (e.hasMoreElements()) {
                String projectName = ((PvcsProject) e.nextElement()).getName();
                if (projectName == null || (projectName.trim()).equals("")) {
                    throw new BuildException("name is a required attribute "
                        + "of pvcsproject");
                }
                commandLine.createArgument().setValue(projectName);
            }
        }

        File tmp = null;
        File tmp2 = null;
        try {
            Random rand = new Random(System.currentTimeMillis());
            tmp = new File("pvcs_ant_" + rand.nextLong() + ".log");
            FileOutputStream fos = new FileOutputStream(tmp);
            tmp2 = new File("pvcs_ant_" + rand.nextLong() + ".log");
            log(commandLine.describeCommand(), Project.MSG_VERBOSE);
            try {
                result = runCmd(commandLine, 
                                new PumpStreamHandler(fos, 
                                    new LogOutputStream(this,
                                                        Project.MSG_WARN)));
            } finally {
                fos.close();
            }
            
            if (result != 0 && !ignorerc) {
                String msg = "Failed executing: " + commandLine.toString();
                throw new BuildException(msg, getLocation());
            }

            if (!tmp.exists()) {
                throw new BuildException("Communication between ant and pvcs "
                    + "failed. No output generated from executing PVCS "
                    + "commandline interface \"pcli\" and \"get\"");
            }

            // Create folders in workspace
            log("Creating folders", Project.MSG_INFO);
            createFolders(tmp);

            // Massage PCLI lvf output transforming '\' to '/' so get command works appropriately
            massagePCLI(tmp, tmp2);

            // Launch get on output captured from PCLI lvf
            commandLine.clearArgs();
            commandLine.setExecutable(getExecutable(GET_EXE));

            if (getForce() != null && getForce().equals("yes")) {
                commandLine.createArgument().setValue("-Y");
            } else {
                commandLine.createArgument().setValue("-N");
            }

            if (getPromotiongroup() != null) {
                commandLine.createArgument().setValue("-G"
                    + getPromotiongroup());
            } else {
                if (getLabel() != null) {
                    commandLine.createArgument().setValue("-r" + getLabel());
                }
            }

            if (updateOnly) {
                commandLine.createArgument().setValue("-U");
            }

            commandLine.createArgument().setValue("@" + tmp2.getAbsolutePath());
            log("Getting files", Project.MSG_INFO);
            log("Executing " + commandLine.toString(), Project.MSG_VERBOSE);
            result = runCmd(commandLine,
                new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN));
            if (result != 0 && !ignorerc) {
                String msg = "Failed executing: " + commandLine.toString()
                    + ". Return code was " + result;
                throw new BuildException(msg, getLocation());
            }

        } catch (FileNotFoundException e) {
            String msg = "Failed executing: " + commandLine.toString()
                + ". Exception: " + e.getMessage();
            throw new BuildException(msg, getLocation());
        } catch (IOException e) {
            String msg = "Failed executing: " + commandLine.toString()
                + ". Exception: " + e.getMessage();
            throw new BuildException(msg, getLocation());
        } catch (ParseException e) {
            String msg = "Failed executing: " + commandLine.toString()
                + ". Exception: " + e.getMessage();
            throw new BuildException(msg, getLocation());
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
            if (tmp2 != null) {
                tmp2.delete();
            }
        }
    }

    /**
     * Parses the file and creates the folders specified in the output section
     */
    private void createFolders(File file) throws IOException, ParseException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            MessageFormat mf = new MessageFormat(getFilenameFormat());
            String line = in.readLine();
            while (line != null) {
                log("Considering \"" + line + "\"", Project.MSG_VERBOSE);
                if (line.startsWith("\"\\") ||
                    line.startsWith("\"/") ||
                    (line.length() >3 &&
                     line.startsWith("\"") &&
                     Character.isLetter(line.charAt(1)) &&
                     String.valueOf(line.charAt(2)).equals(":") &&
                     String.valueOf(line.charAt(3)).equals("\\"))) {
                    Object[] objs = mf.parse(line);
                    String f = (String) objs[1];
                    // Extract the name of the directory from the filename
                    int index = f.lastIndexOf(File.separator);
                    if (index > -1) {
                        File dir = new File(f.substring(0, index));
                        if (!dir.exists()) {
                            log("Creating " + dir.getAbsolutePath(), 
                                Project.MSG_VERBOSE);
                            if (dir.mkdirs()) {
                                log("Created " + dir.getAbsolutePath(), 
                                    Project.MSG_INFO);
                            } else {
                                log("Failed to create " 
                                    + dir.getAbsolutePath(), 
                                    Project.MSG_INFO);
                            }
                        } else {
                            log(dir.getAbsolutePath() + " exists. Skipping", 
                                Project.MSG_VERBOSE);
                        }
                    } else {
                        log("File separator problem with " + line,
                            Project.MSG_WARN);
                    }
                } else {
                    log("Skipped \"" + line + "\"", Project.MSG_VERBOSE);
                }
                line = in.readLine();
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
        

    /**
     * Simple hack to handle the PVCS command-line tools botch when
     * handling UNC notation.
     */
    private void massagePCLI(File in, File out)
        throws FileNotFoundException, IOException {
        BufferedReader inReader = null;
        BufferedWriter outWriter = null;
        try {
            inReader = new BufferedReader(new FileReader(in));
            outWriter = new BufferedWriter(new FileWriter(out));
            String s = null;
            while ((s = inReader.readLine()) != null) {
                String sNormal = s.replace('\\', '/');
                outWriter.write(sNormal);
                outWriter.newLine();
            }
        } finally {
            if (inReader != null) {
                inReader.close();
            }
            if (outWriter != null) {
                outWriter.close();
            }
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
     *  The filenameFormat attribute defines a MessageFormat string used
     *  to parse the output of the pcli command.  It defaults to
     *  <code>{0}-arc({1})</code>.  Repositories where the archive
     *   extension is not  -arc should set this.
     */
    public String getFilenameFormat() {
        return filenameFormat;
    }

    /**
     * The format of the folder names; optional.
     * This must be in a format suitable for 
     * <code>java.text.MessageFormat</code>.
     *  Index 1 of the format will be used as the file name.
     *  Defaults to <code>{0}-arc({1})</code>
     */
    public void setFilenameFormat(String f) {
        filenameFormat = f;
    }

    /**

     * The lineStart attribute is used to parse the output of the pcli
     * command. It defaults to <code>&quot;P:</code>.  The parser already
     * knows about / and \\, this property is useful in cases where the
     * repository is accessed on a Windows platform via a drive letter
     * mapping.
     */
    public String getLineStart() {
        return lineStart;
    }

    /**
     * What a valid return value from PVCS looks like
     *  when it describes a file.  Defaults to <code>&quot;P:</code>.
     * If you are not using an UNC name for your repository and the
     * drive letter <code>P</code> is incorrect for your setup, you may
     * need to change this value, UNC names will always be
     * accepted.
     */
        
    public void setLineStart(String l) {
        lineStart = l;
    }

    /**
     * The network name of the PVCS repository; required.
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
     * The project within the PVCS repository to extract files from;
     * optional, default &quot;/&quot;
     * @param prj String
     */
    public void setPvcsproject(String prj) {
        pvcsProject = prj;
    }

    /**
     * Get name of the project in the PVCS repository
     * @return Vector
     */
    public Vector getPvcsprojects() {
        return pvcsProjects;
    }

    /**
     * Get name of the workspace to store the retrieved files
     * @return String
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Workspace to use; optional.  
     * By specifying a workspace, the files are extracted to that location.
     * A PVCS workspace is a name for a location of the workfiles and 
     * isn't as such the location itself. 
     * You define the location for a workspace using the PVCS GUI clients.
     * If this isn't specified the default workspace for the current user is used.
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
     * Specifies the location of the PVCS bin directory; optional if on the PATH.
     * On some systems the PVCS executables <i>pcli</i>
     * and <i>get</i> are not found in the PATH. In such cases this attribute
     * should be set to the bin directory of the PVCS installation containing
     * the executables mentioned before. If this attribute isn't specified the
     * tag expects the executables to be found using the PATH environment variable.
     * @param ws String
     * @todo use a File setter and resolve paths.
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
     * Specifies the value of the force argument; optional.
     * If set to <i>yes</i> all files that exists and are 
     * writable are overwritten. Default <i>no</i> causes the files 
     * that are writable to be ignored. This stops the PVCS command 
     * <i>get</i> to stop asking questions!
     * @todo make a boolean setter
     * @param repo String (yes/no)
     */
    public void setForce(String f) {
        if (f != null && f.equalsIgnoreCase("yes")) {
            force = "yes";
        } else {
            force = "no";
        }
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
        promotiongroup = w;
    }

    /**
     * Get value of label
     * @return String
     */
    public String getLabel() {
        return label;
    }

    /**
     * Only files marked with this label are extracted; optional.
     * @param repo String
     */
    public void setLabel(String l) {
        label = l;
    }

    /**
     * Get value of ignorereturncode
     * @return String
     */
    public boolean getIgnoreReturnCode() {
        return ignorerc;
    }

    /**
     * If set to true the return value from executing the pvcs
     * commands are ignored; optional, default false.
     */
    public void setIgnoreReturnCode(boolean b) {
        ignorerc = b;
    }

    /**
     * Specify a project within the PVCS repository to extract files from.
     * @param PvcsProject
     */
    public void addPvcsproject(PvcsProject p) {
        pvcsProjects.addElement(p);
    }

    public boolean getUpdateOnly() {
        return updateOnly;
    }

    /**
     * If set to <i>true</i> files are fetched only if 
     * newer than existing local files; optional, default false.
     */
    public void setUpdateOnly(boolean l) {
        updateOnly = l;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * User ID; unused.
     * @ant.attribute ignore="true"
     */
     
    public void setUserId(String u) {
        userId = u;
    }

    /**
     * Creates a Pvcs object
     */
    public Pvcs() {
        super();
        pvcsProject = null;
        pvcsProjects = new Vector();
        workspace = null;
        repository = null;
        pvcsbin = null;
        force = null;
        promotiongroup = null;
        label = null;
        ignorerc = false;
        updateOnly = false;
        lineStart = "\"P:";
        filenameFormat = "{0}-arc({1})";
    }
}

