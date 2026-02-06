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

package org.apache.tools.ant.taskdefs.optional.windows;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Runs <a href="https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/mklink">mklink</a> on Win32 systems.
 *
 * @since Ant 1.10.16
 */
public class Mklink extends Task {
    private static final String FILE_SYMLINK = "file-symlink";
    private static final String DIR_SYMLINK = "dir-symlink";
    private static final String HARDLINK = "hardlink";
    private static final String JUNCTION = "junction";

    private File link;
    private File targetFile;
    private String targetText;
    private LinkType linkType;
    private boolean overwrite;

    /**
     * The link to create.
     *
     * @param link the path of the link to create
     */
    public void setLink(File link) {
        this.link = link;
    }

    /**
     * The link target specified as file.
     *
     * @param target the path of the link taget as resolved file
     */
    public void setTargetFile(File target) {
        this.targetFile = target;
    }

    /**
     * The link target specified as text.
     *
     * @param target the path of the link taget as string
     */
    public void setTargetText(String target) {
        this.targetText = target;
    }

    /**
     * The type of link to create.
     *
     * <p>If not specified explicitly and target is given as a file
     * the link type will be guessed to be the proper type of symlink
     * for the target type.</p>
     *
     * @param type one of "file-symlink", "dir-symlink", "hardlink" or "junction".
     */
    public void setLinkType(LinkType type) {
        linkType = type;
    }

    /**
     * Set overwrite mode. If set to false (default) the task will not
     * overwrite existing links.
     *
     * @param owrite If true overwrite existing links.
     */
    public void setOverwrite(boolean owrite) {
        this.overwrite = owrite;
    }

    @Override
    public void execute() throws BuildException {
        validate();
        runMklink();
    }

    private void validate() throws BuildException {
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            throw new BuildException("this task only works on Windows");
        }
        if (link == null) {
            throw new BuildException("this \"link\" attribute is required");
        }
        if (targetFile == null && targetText == null) {
            throw new BuildException("either \"targetFile\" the \"targetText\" attribute is required");
        }
        if (targetFile != null && targetText != null) {
            throw new BuildException("only one of \"targetFile\" and \"targetText\" can be specified");
        }
        if (linkType == null && targetFile == null) {
            throw new BuildException("\"linkType\" is required when using \"targetText");
        }
    }

    private void runMklink() throws BuildException {
        Commandline cmd = createCommandLine();

        if (link.exists()) {
            if (!overwrite) {
                log("Skipping link creation, since file at " + link + " already exists and overwrite is set to false", Project.MSG_INFO);
                return;
            }
            boolean deleted = link.delete();
            if (!deleted) {
                throw new BuildException("Deletion of existing file at " + link + " failed");
            }
        }
        Execute exe = new Execute(new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN),
                                  null);
        exe.setCommandline(cmd.getCommandline());
        exe.setWorkingDirectory(getProject().getBaseDir());
        log(cmd.describeCommand(), Project.MSG_VERBOSE);
        try {
            int returncode = exe.execute();
            if (Execute.isFailure(returncode)) {
                throw new BuildException("'mklink' failed with exit code " + returncode);
            }
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

    private Commandline createCommandLine() throws BuildException {
        StringBuilder sb = new StringBuilder();
        sb.append("mklink ");
        String linkValue = linkType != null ? linkType.getValue()
            : targetFile.isDirectory() ? DIR_SYMLINK : FILE_SYMLINK;
        if (DIR_SYMLINK.equals(linkValue)) {
            sb.append("/d ");
            if (targetFile != null && !targetFile.isDirectory()) {
                throw new BuildException("target of a directory symlink must be a directory");
            }
        } else if (HARDLINK.equals(linkValue)) {
            sb.append("/h ");
            if (targetFile != null && !targetFile.isFile()) {
                throw new BuildException("target of a hardlink must be a file");
            }
        } else if (JUNCTION.equals(linkValue)) {
            sb.append("/j ");
            if (targetFile != null && !targetFile.isDirectory()) {
                throw new BuildException("target of a directory junction must be a directory");
            }
        } else if (targetFile != null && !targetFile.isFile()) {
            throw new BuildException("target of a file symlink must be a file");
        }
        sb.append(Commandline.quoteArgument(link.getAbsolutePath()));
        sb.append(" ");
        if (targetFile != null) {
            sb.append(Commandline.quoteArgument(targetFile.getAbsolutePath()));
        } else {
            sb.append(Commandline.quoteArgument(targetText));
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable("cmd.exe");
        cmd.createArgument().setValue("/c");
        cmd.createArgument().setValue(sb.toString());
        return cmd;
    }

    public static class LinkType extends EnumeratedAttribute {
        /**
         * @see EnumeratedAttribute#getValues
         * {@inheritDoc}.
         */
        @Override
        public String[] getValues() {
            return new String[] {FILE_SYMLINK, DIR_SYMLINK, HARDLINK, JUNCTION};
        }
    }

}
