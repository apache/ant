/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.selectors.*;

import java.io.File;
import java.util.StringTokenizer;
import java.util.Enumeration;

/**
 * This is an abstract task that should be used by all those tasks that 
 * require to include or exclude files based on pattern matching.
 *
 * @author Arnout J. Kuiper 
 *         <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a> 
 * @author Stefano Mazzocchi 
 *         <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since Ant 1.1
 */

public abstract class MatchingTask extends Task implements SelectorContainer {

    protected boolean useDefaultExcludes = true;
    protected FileSet fileset = new FileSet();

    /**
     * @see org.apache.tools.ant.ProjectComponent#setProject
     */
    public void setProject(Project project) {
        super.setProject(project);
        fileset.setProject(project);
    }

    /**
     * add a name entry on the include list
     */
    public PatternSet.NameEntry createInclude() {
        return fileset.createInclude();
    }

    /**
     * add a name entry on the include files list
     */
    public PatternSet.NameEntry createIncludesFile() {
        return fileset.createIncludesFile();
    }

    /**
     * add a name entry on the exclude list
     */
    public PatternSet.NameEntry createExclude() {
        return fileset.createExclude();
    }

    /**
     * add a name entry on the include files list
     */
    public PatternSet.NameEntry createExcludesFile() {
        return fileset.createExcludesFile();
    }

    /**
     * add a set of patterns
     */
    public PatternSet createPatternSet() {
        return fileset.createPatternSet();
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes(String includes) {
        fileset.setIncludes(includes);
    }

    /**
     * Set this to be the items in the base directory that you want to be
     * included. You can also specify "*" for the items (ie: items="*")
     * and it will include all the items in the base directory.
     *
     * @param itemString the string containing the files to include.
     */
    public void XsetItems(String itemString) {
        log("The items attribute is deprecated. " +
            "Please use the includes attribute.",
            Project.MSG_WARN);
        if (itemString == null || itemString.equals("*") 
            || itemString.equals(".")) {
            createInclude().setName("**");
        } else {
            StringTokenizer tok = new StringTokenizer(itemString, ", ");
            while (tok.hasMoreTokens()) {
                String pattern = tok.nextToken().trim();
                if (pattern.length() > 0) {
                    createInclude().setName(pattern + "/**");
                }
            }
        }
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes(String excludes) {
        fileset.setExcludes(excludes);
    }

    /**
     * List of filenames and directory names to not include. They should be 
     * either , or " " (space) separated. The ignored files will be logged.
     *
     * @param ignoreString the string containing the files to ignore.
     */
    public void XsetIgnore(String ignoreString) {
        log("The ignore attribute is deprecated." +
            "Please use the excludes attribute.",
            Project.MSG_WARN);
        if (ignoreString != null && ignoreString.length() > 0) {
            StringTokenizer tok = new StringTokenizer(ignoreString, ", ",
                                                      false);
            while (tok.hasMoreTokens()) {
                createExclude().setName("**/" + tok.nextToken().trim() + "/**");
            }
        }
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(boolean useDefaultExcludes) {
        this.useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * Returns the directory scanner needed to access the files to process.
     */
    protected DirectoryScanner getDirectoryScanner(File baseDir) {
        fileset.setDir(baseDir);
        fileset.setDefaultexcludes(useDefaultExcludes);
        return fileset.getDirectoryScanner(getProject());
    }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param includesfile A string containing the filename to fetch
     * the include patterns from.
     */
    public void setIncludesfile(File includesfile) {
        fileset.setIncludesfile(includesfile);
    }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param excludesfile A string containing the filename to fetch
     * the include patterns from.
     */
    public void setExcludesfile(File excludesfile) {
        fileset.setExcludesfile(excludesfile);
    }

    /**
     * Sets case sensitivity of the file system
     *
     * @param isCaseSensitive "true"|"on"|"yes" if file system is case
     *                           sensitive, "false"|"off"|"no" when not.
     */
    public void setCaseSensitive(boolean isCaseSensitive) {
        fileset.setCaseSensitive(isCaseSensitive);
    }

    /**
     * Sets whether or not symbolic links should be followed.
     *
     * @param followSymlinks whether or not symbolic links should be followed
     */
    public void setFollowSymlinks(boolean followSymlinks) {
        fileset.setFollowSymlinks(followSymlinks);
    }

    /**
     * Indicates whether there are any selectors here.
     *
     * @return whether any selectors are in this container
     */
    public boolean hasSelectors() {
        return fileset.hasSelectors();
    }

    /**
     * Gives the count of the number of selectors in this container
     *
     * @return the number of selectors in this container
     */
    public int selectorCount() {
        return fileset.selectorCount();
    }

    /**
     * Returns the set of selectors as an array.
     *
     * @return an array of selectors in this container
     */
    public FileSelector[] getSelectors(Project p) {
        return fileset.getSelectors(p);
    }

    /**
     * Returns an enumerator for accessing the set of selectors.
     *
     * @return an enumerator that goes through each of the selectors
     */
    public Enumeration selectorElements() {
        return fileset.selectorElements();
    }

    /**
     * Add a new selector into this container.
     *
     * @param selector the new selector to add
     * @return the selector that was added
     */
    public void appendSelector(FileSelector selector) {
        fileset.appendSelector(selector);
    }

    /* Methods below all add specific selectors */

    /**
     * add a "Select" selector entry on the selector list
     */
    public void addSelector(SelectSelector selector) {
        fileset.addSelector(selector);
    }

    /**
     * add an "And" selector entry on the selector list
     */
    public void addAnd(AndSelector selector) {
        fileset.addAnd(selector);
    }

    /**
     * add an "Or" selector entry on the selector list
     */
    public void addOr(OrSelector selector) {
        fileset.addOr(selector);
    }

    /**
     * add a "Not" selector entry on the selector list
     */
    public void addNot(NotSelector selector) {
        fileset.addNot(selector);
    }

    /**
     * add a "None" selector entry on the selector list
     */
    public void addNone(NoneSelector selector) {
        fileset.addNone(selector);
    }

    /**
     * add a majority selector entry on the selector list
     */
    public void addMajority(MajoritySelector selector) {
        fileset.addMajority(selector);
    }

    /**
     * add a selector date entry on the selector list
     */
    public void addDate(DateSelector selector) {
        fileset.addDate(selector);
    }

    /**
     * add a selector size entry on the selector list
     */
    public void addSize(SizeSelector selector) {
        fileset.addSize(selector);
    }

    /**
     * add a selector filename entry on the selector list
     */
    public void addFilename(FilenameSelector selector) {
        fileset.addFilename(selector);
    }

    /**
     * add an extended selector entry on the selector list
     */
    public void addCustom(ExtendSelector selector) {
        fileset.addCustom(selector);
    }

    /**
     * add a contains selector entry on the selector list
     */
    public void addContains(ContainsSelector selector) {
        fileset.addContains(selector);
    }

    /**
     * add a present selector entry on the selector list
     */
    public void addPresent(PresentSelector selector) {
        fileset.addPresent(selector);
    }

    /**
     * add a depth selector entry on the selector list
     */
    public void addDepth(DepthSelector selector) {
        fileset.addDepth(selector);
    }

    /**
     * add a depends selector entry on the selector list
     */
    public void addDepend(DependSelector selector) {
        fileset.addDepend(selector);
    }

    /**
     * Accessor for the implict fileset.
     *
     * @since Ant 1.5.2
     */
    protected final FileSet getImplicitFileSet() {
        return fileset;
    }
}
