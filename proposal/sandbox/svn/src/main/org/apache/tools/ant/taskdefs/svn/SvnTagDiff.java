/*
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.svn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

/**
 * Examines the output of svn diff between two tags or a tag and trunk.
 *
 * <p>This task only works if you follow the best-practice structure of
 * <pre>
 * BASEURL
 *   |
 *   |
 *   -----&gt; trunk
 *   -----&gt; tags
 *            |
 *            |
 *            ----------&gt; tag1
 *            ----------&gt; tag2
 * </pre>
 *
 * It produces an XML output representing the list of changes.
 * <PRE>
 * &lt;!-- Root element --&gt;
 * &lt;!ELEMENT tagdiff ( paths? ) &gt;
 * &lt;!-- First tag --&gt;
 * &lt;!ATTLIST tagdiff tag1 NMTOKEN #IMPLIED &gt;
 * &lt;!-- Second tag --&gt;
 * &lt;!ATTLIST tagdiff tag2 NMTOKEN #IMPLIED &gt;
 * &lt;!-- Subversion BaseURL --&gt;
 * &lt;!ATTLIST tagdiff svnurl NMTOKEN #IMPLIED &gt;
 *
 * &lt;!-- Path added, changed or removed --&gt;
 * &lt;!ELEMENT path ( name,action ) &gt;
 * &lt;!-- Name of the file --&gt;
 * &lt;!ELEMENT name ( #PCDATA ) &gt;
 * &lt;!ELEMENT action (added|modified|deleted)&gt;
 * </PRE>
 *
 * @ant.task name="svntagdiff"
 */
public class SvnTagDiff extends AbstractSvnTask {

    /**
     * Used to create the temp file for svn log
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * The earliest revision from which diffs are to be included in the report.
     */
    private String tag1;

    /**
     * The latest revision from which diffs are to be included in the report.
     */
    private String tag2;

    /**
     * The file in which to write the diff report.
     */
    private File mydestfile;

    /**
     * Base URL.
     */
    private String baseURL;

    /**
     * Set the first tag.
     *
     * @param s the first tag.
     */
    public void setTag1(String s) {
        tag1 = s;
    }

    /**
     * Set the second tag.
     *
     * @param s the second tag.
     */
    public void setTag2(String s) {
        tag2 = s;
    }

    /**
     * Set the output file for the diff.
     *
     * @param f the output file for the diff.
     */
    public void setDestFile(File f) {
        mydestfile = f;
    }

    /**
     * Set the base URL from which to calculate tag URLs.
     *
     * @param u the base URL from which to calculate tag URLs.
     */
    public void setBaseURL(String u) {
        baseURL = u;
        if (!u.endsWith("/")) {
            baseURL += "/";
        }
    }

    /**
     * Execute task.
     *
     * @exception BuildException if an error occurs
     */
    public void execute() throws BuildException {
        // validate the input parameters
        validate();

        // build the rdiff command
        setSubCommand("diff");
        addSubCommandArgument("--no-diff-deleted");
        if (tag1.equals("trunk") || tag1.equals("trunk/")) {
            addSubCommandArgument(baseURL + "trunk/");
        } else {
            if (tag1.endsWith("/")) {
                addSubCommandArgument(baseURL + "tags/" + tag1);
            } else {
                addSubCommandArgument(baseURL + "tags/" + tag1 + "/");
            }
        }
        if (tag2 == null || tag2.equals("trunk") || tag2.equals("trunk/")) {
            addSubCommandArgument(baseURL + "trunk/");
        } else {
            if (tag2.endsWith("/")) {
                addSubCommandArgument(baseURL + "tags/" + tag2);
            } else {
                addSubCommandArgument(baseURL + "tags/" + tag2 + "/");
            }
        }
        
        File tmpFile = null;
        try {
            tmpFile = 
                FILE_UTILS.createTempFile("svntagdiff", ".log", null);
            tmpFile.deleteOnExit();
            setOutput(tmpFile);

            // run the svn command
            super.execute();

            // parse the diff
            SvnEntry.Path[] entries = SvnDiffHandler.parseDiff(tmpFile);

            // write the revision diff
            SvnDiffHandler.writeDiff(mydestfile, entries, "tagdiff",
                                     "tag1", tag1, "tag2", 
                                     tag2 == null ? "trunk" : tag2, 
                                     baseURL);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    /**
     * Validate the parameters specified for task.
     *
     * @exception BuildException if a parameter is not correctly set
     */
    private void validate() throws BuildException {
        if (null == mydestfile) {
            throw new BuildException("Destfile must be set.");
        }

        if (null == tag1) {
            throw new BuildException("tag1 must be set.");
        }

        if (null == baseURL) {
            throw new BuildException("baseURL must be set.");
        }
    }
}
