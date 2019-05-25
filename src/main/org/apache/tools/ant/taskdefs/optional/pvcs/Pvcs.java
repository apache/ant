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
package org.apache.tools.ant.taskdefs.optional.pvcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Random;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;

/**
 *
 * Extracts the latest edition of the source code from a PVCS repository.
 * PVCS is a version control system
 * developed by Merant.
 * <p>
 * Before using this tag, the user running ant must have access to the commands
 * of PVCS (get and pcli) and must have access to the repository. Note that the way to specify
 * the repository is platform dependent so use property to specify location of repository.
 * </p>
 * This version has been tested against PVCS version 6.5 and 6.6 under Windows and Solaris.

 *
 * <b>19-04-2001</b> <p>The task now has a more robust
 * parser. It allows for platform independent file paths
 * and supports file names with <i>()</i>. Thanks to Erik Husby for
 * bringing the bug to my attention.
 *
 * <b>27-04-2001</b> <p>UNC paths are now handled properly.
 * Fix provided by Don Jeffery. He also added an <i>UpdateOnly</i> flag
 * that, when true, conditions the PVCS get using the -U option to only
 * update those files that have a modification time (in PVCS) that is newer
 * than the existing workfile.
 *
 * <b>25-10-2002</b> <p>Added a revision attribute that currently is a
 * synonym for label, but in a future release the behavior of the label
 * attribute will change to use the -v option of GET.  See bug #13847 for
 * discussion.
 *
 */
public class Pvcs extends Task {
    // CheckStyle - magic numbers
    // checking for "X:\ 0=dquote,1=letter,2=:,3=\
    private static final int POS_1 = 1;
    private static final int POS_2 = 2;
    private static final int POS_3 = 3;

    /**
     * Constant for the thing to execute
     */
    private static final String PCLI_EXE = "pcli";

    /**
     * Constant for the thing to execute
     */
    private static final String GET_EXE = "get";

    private String pvcsbin;
    private String repository;
    private String pvcsProject;
    private Vector<PvcsProject> pvcsProjects;
    private String workspace;
    private String force;
    private String promotiongroup;
    private String label;
    private String revision;
    private boolean ignorerc;
    private boolean updateOnly;
    private String filenameFormat;
    private String lineStart;
    private String userId;
    private String config;

    /**
     * Creates a Pvcs object
     */
    public Pvcs() {
        super();
        pvcsProject = null;
        pvcsProjects = new Vector<>();
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

    /**
     * Run the command.
     * @param cmd the command line to use.
     * @param out the output stream handler to use.
     * @return the exit code of the command.
     */
    protected int runCmd(Commandline cmd, ExecuteStreamHandler out) {
        try {
            Project aProj = getProject();
            Execute exe = new Execute(out);
            exe.setAntRun(aProj);
            exe.setWorkingDirectory(aProj.getBaseDir());
            exe.setCommandline(cmd.getCommandline());
            return exe.execute();
        } catch (IOException e) {
            String msg = "Failed executing: " + cmd.toString()
                + ". Exception: " + e.getMessage();
            throw new BuildException(msg, getLocation());
        }
    }

    private String getExecutable(String exe) {
        StringBuilder correctedExe = new StringBuilder();
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
     * @throws BuildException Something is stopping the build...
     */
    @Override
    public void execute() throws BuildException {
        int result = 0;

        if (repository == null || repository.trim().isEmpty()) {
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
            for (PvcsProject pvcsProject : getPvcsprojects()) {
                String projectName = pvcsProject.getName();
                if (projectName == null || projectName.trim().isEmpty()) {
                    throw new BuildException("name is a required attribute of pvcsproject");
                }
                commandLine.createArgument().setValue(projectName);
            }
        }

        File tmp = null;
        File tmp2 = null;
        try {
            Random rand = new Random(System.currentTimeMillis());
            tmp = new File("pvcs_ant_" + rand.nextLong() + ".log");
            OutputStream fos = Files.newOutputStream(tmp.toPath());
            tmp2 = new File("pvcs_ant_" + rand.nextLong() + ".log");
            log(commandLine.describeCommand(), Project.MSG_VERBOSE);
            try {
                result = runCmd(commandLine, new PumpStreamHandler(fos,
                        new LogOutputStream(this, Project.MSG_WARN)));
            } finally {
                FileUtils.close(fos);
            }

            if (Execute.isFailure(result) && !ignorerc) {
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

            if (getConfig() != null && !getConfig().isEmpty()) {
                commandLine.createArgument().setValue("-c" + getConfig());
            }

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
                    commandLine.createArgument().setValue("-v" + getLabel());
                } else {
                    if (getRevision() != null) {
                        commandLine.createArgument().setValue("-r" + getRevision());
                    }
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

        } catch (ParseException | IOException e) {
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
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            MessageFormat mf = new MessageFormat(getFilenameFormat());
            String line = in.readLine();
            while (line != null) {
                log("Considering \"" + line + "\"", Project.MSG_VERBOSE);
                if (line.startsWith("\"\\")       // Checking for "\
                        || line.startsWith("\"/") // or           "/
                                                  // or           "X:\...
                        || (line.length() > POS_3 && line.startsWith("\"")
                        && Character.isLetter(line.charAt(POS_1))
                        && String.valueOf(line.charAt(POS_2)).equals(":")
                        && String.valueOf(line.charAt(POS_3)).equals("\\"))) {
                    Object[] objs = mf.parse(line);
                    String f = (String) objs[1];
                    // Extract the name of the directory from the filename
                    int index = f.lastIndexOf(File.separator);
                    if (index > -1) {
                        File dir = new File(f.substring(0, index));
                        if (dir.exists()) {
                            log(dir.getAbsolutePath() + " exists. Skipping",
                                Project.MSG_VERBOSE);
                        } else {
                            log("Creating " + dir.getAbsolutePath(),
                                Project.MSG_VERBOSE);
                            if (dir.mkdirs() || dir.isDirectory()) {
                                log("Created " + dir.getAbsolutePath(),
                                    Project.MSG_INFO);
                            } else {
                                log("Failed to create "
                                    + dir.getAbsolutePath(),
                                    Project.MSG_INFO);
                            }
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
        }
    }


    /**
     * Simple hack to handle the PVCS command-line tools botch when
     * handling UNC notation.
     * @throws IOException if there is an error.
     */
    private void massagePCLI(File in, File out)
        throws IOException {
        try (BufferedReader inReader = new BufferedReader(new FileReader(in));
                BufferedWriter outWriter = new BufferedWriter(new FileWriter(out))) {
            for (String line : (Iterable<String>) () -> inReader.lines()
                    .map(s -> s.replace('\\', '/')).iterator()) {
                outWriter.write(line);
                outWriter.newLine();
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
     * @return the filename format attribute.
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
     * @param f the format to use.
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
     * @return the lineStart attribute.
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
     * @param l the value to use.
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
    public Vector<PvcsProject> getPvcsprojects() {
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
     * @param bin PVCS bin directory
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
     * @param f String (yes/no)
     */
    public void setForce(String f) {
        force = "yes".equalsIgnoreCase(f) ? "yes" : "no";
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
     * @param w String
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
     * @param l String
     */
    public void setLabel(String l) {
        label = l;
    }

    /**
     * Get value of revision
     * @return String
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Only files with this revision are extract; optional.
     * @param r String
     */
    public void setRevision(String r) {
        revision = r;
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
     * @param b a <code>boolean</code> value.
     */
    public void setIgnoreReturnCode(boolean b) {
        ignorerc = b;
    }

    /**
     * Specify a project within the PVCS repository to extract files from.
     * @param p the pvcs project to use.
     */
    public void addPvcsproject(PvcsProject p) {
        pvcsProjects.addElement(p);
    }

    /**
     * get the updateOnly attribute.
     * @return the updateOnly attribute.
     */
    public boolean getUpdateOnly() {
        return updateOnly;
    }

    /**
     * If set to <i>true</i> files are fetched only if
     * newer than existing local files; optional, default false.
     * @param l a <code>boolean</code> value.
     */
    public void setUpdateOnly(boolean l) {
        updateOnly = l;
    }

    /**
     * returns the path of the configuration file to be used
     * @return the path of the config file
     */
    public String getConfig() {
        return config;
    }

    /**
     * Sets a configuration file other than the default to be used.
     * These files have a .cfg extension and are often found in archive or pvcsprop folders.
     * @param f config file - can be given absolute or relative to ant basedir
     */
    public void setConfig(File f) {
        config = f.toString();
    }


    /**
     * Get the userid.
     * @return the userid.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * User ID
     * @param u the value to use.
     */
    public void setUserId(String u) {
        userId = u;
    }

}

