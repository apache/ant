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
package org.apache.tools.ant.taskdefs.optional.starteam;

import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.Label;
import com.starbase.starteam.StarTeamFinder;
import com.starbase.starteam.View;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * FileBasedTask.java
 * This abstract class is the base for any tasks that are tree-based, that
 * is, for tasks which iterate over a tree of folders in StarTeam which
 * is reflected in a tree of folder the local machine.
 *
 * This class provides the tree-iteration functionality.  Derived classes
 * will implement their specific task functionality by the visitor pattern,
 * specifically by implementing the method
 * <code>visit(Folder rootStarteamFolder, java.io.File rootLocalFolder)</code>
 *
 * Created: Sat Dec 15 16:55:19 2001
 *
 * @author <a href="mailto:stevec@ignitesports.com">Steve Cohen</a>
 * @version 1.0
 * @see <A HREF="http://www.starbase.com/">StarBase Web Site</A>
 */

public abstract class TreeBasedTask extends StarTeamTask {


    ///////////////////////////////////////////////////////////////
    // default values for attributes.
    ///////////////////////////////////////////////////////////////
    /**
     * This constant sets the filter to include all files. This default has
     * the same result as <CODE>setIncludes("*")</CODE>.
     *
     * @see #getIncludes()
     * @see #setIncludes(String includes)
     */
    public static final String DEFAULT_INCLUDESETTING = "*";

    /**
     * This disables the exclude filter by default. In other words, no files
     * are excluded. This setting is equivalent to
     * <CODE>setExcludes(null)</CODE>.
     *
     * @see #getExcludes()
     * @see #setExcludes(String excludes)
     */
    public static final String DEFAULT_EXCLUDESETTING = null;

    //ATTRIBUTES settable from ant.

    /**
     * The root folder of the operation in StarTeam.
     */
    private String rootStarteamFolder = "/";

    /**
     * The local folder corresponding to starteamFolder.  If not specified
     * the Star Team default folder will be used.
     */
    private String rootLocalFolder = null;

    /**
     * All files that fit this pattern are checked out.
     */
    private String includes = DEFAULT_INCLUDESETTING;

    /**
     * All files fitting this pattern are ignored.
     */
    private String excludes = DEFAULT_EXCLUDESETTING;

    /**
     * StarTeam label on which to perform task.
     */
    private String label = null;

    /**
     * Set recursion to false to check out files in only the given folder
     * and not in its subfolders.
     */
    private boolean recursive = true;

    /**
     * If forced set to true, files in the target directory will
     * be processed regardless of status in the repository.
     * Usually this should be  true if rootlocalfolder is set
     * because status will be relative to the default folder, not
     * to the one being processed.
     */
    private boolean forced = false;

    private Label labelInUse = null;

    ///////////////////////////////////////////////////////////////
    // GET/SET methods.
    // Setters, of course are where ant user passes in values.
    ///////////////////////////////////////////////////////////////

    /**
     * Set the root of the subtree in the StarTeam repository from which to 
     * work; optional.  Defaults to the root folder of the view ('/').
     * @param rootStarteamFolder the root folder
     */
    public void setRootStarteamFolder(String rootStarteamFolder) {
        this.rootStarteamFolder = rootStarteamFolder;
    }

    /**
     * returns the root folder in the Starteam repository
     * used for this operation
     * @return the root folder in use
     */
    public String getRootStarteamFolder() {
        return this.rootStarteamFolder;
    }

    /**
     * Set the local folder that will be the root of the tree
     * to which files are checked out; optional.
     * If this is not supplied, then the StarTeam "default folder"
     * associated with <tt>rootstarteamfolder</tt> is used.
     * 
     * @param rootLocalFolder
     *               the local folder that will mirror
     *               this.rootStarteamFolder
     * 
     * @see rootLocalFolder
     */
    public void setRootLocalFolder(String rootLocalFolder) {
        this.rootLocalFolder = rootLocalFolder;
    }



    /**
     * Returns the local folder specified by the user,
     * corresponding to the starteam folder for this operation
     * or null if not specified.
     * 
     * @return the local folder that mirrors this.rootStarteamFolder
     * @see rootLocalFolder
     */
    public String getRootLocalFolder() {
        return this.rootLocalFolder;
    }


    /**
     * Declare files to include using standard <tt>includes</tt> patterns; optional. 
     * @param includes A string of filter patterns to include. Separate the
     *                 patterns by spaces.
     * @see #getIncludes()
     * @see #setExcludes(String excludes)
     * @see #getExcludes()
     */
    public void setIncludes(String includes) {
        this.includes = includes;
    }

    /**
     * Gets the patterns from the include filter. Rather that duplicate the
     * details of AntStarTeamCheckOut's filtering here, refer to these
     * links:
     *
     * @return A string of filter patterns separated by spaces.
     * @see #setIncludes(String includes)
     * @see #setExcludes(String excludes)
     * @see #getExcludes()
     */
    public String getIncludes() {
        return includes;
    }

    /**
     * if excludes have been specified, emit the list to the log
     */
    protected void logIncludes() {
        if (this.DEFAULT_INCLUDESETTING != this.includes) {
            log("  Includes specified: "+ this.includes);
        }
    }

    /**
     * Declare files to exclude using standard <tt>excludes</tt> patterns; optional. 
     * When filtering files, AntStarTeamCheckOut
     * uses an unmodified version of <CODE>DirectoryScanner</CODE>'s
     * <CODE>match</CODE> method, so here are the patterns straight from the
     * Ant source code:
     * <BR><BR>
     * Matches a string against a pattern. The pattern contains two special
     * characters:
     * <BR>'*' which means zero or more characters,
     * <BR>'?' which means one and only one character.
     * <BR><BR>
     *  For example, if you want to check out all files except .XML and
     * .HTML files, you would put the following line in your program:
     * <CODE>setExcludes("*.XML,*.HTML");</CODE>
     * Finally, note that filters have no effect on the <B>directories</B>
     * that are scanned; you could not skip over all files in directories
     * whose names begin with "project," for instance.
     * <BR><BR>
     * Treatment of overlapping inlcudes and excludes: To give a simplistic
     * example suppose that you set your include filter to "*.htm *.html"
     * and your exclude filter to "index.*". What happens to index.html?
     * AntStarTeamCheckOut will not check out index.html, as it matches an
     * exclude filter ("index.*"), even though it matches the include
     * filter, as well.
     * <BR><BR>
     * Please also read the following sections before using filters:
     *
     * @param excludes A string of filter patterns to exclude. Separate the
     *                 patterns by spaces.
     * @see #setIncludes(String includes)
     * @see #getIncludes()
     * @see #getExcludes()
     */
    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

    /**
     * Gets the patterns from the exclude filter. Rather that duplicate the
     * details of AntStarTeanCheckOut's filtering here, refer to these
     * links:
     *
     * @return A string of filter patterns separated by spaces.
     * @see #setExcludes(String excludes)
     * @see #setIncludes(String includes)
     * @see #getIncludes()
     */
    public String getExcludes() {
        return excludes;
    }

    /**
     * if excludes have been specified, emit the list to the log
     */
    protected void logExcludes() {
        if (this.DEFAULT_EXCLUDESETTING != this.excludes) {
            log("  Excludes specified: "+ this.excludes);
        }
    }


    /**
     * protected function to allow subclasses to set the label (or not).
     * sets the StarTeam label
     *
     * @param label name of the StarTeam label to be set
     */
    protected void _setLabel(String label) {
        if (null != label) {
            label = label.trim();
            if (label.length() > 0) {
                this.label = label;
            }
        }
    }

    protected String getLabel() {
        return this.label;
    }

    /**
     * Get the value of recursive.
     * @return value of recursive.
     */
    public boolean isRecursive() {
        return this.recursive;
    }

    /**
     * Flag to set to include files in subfolders in the operation; optional,
     * default true.
     * @param v  Value to assign to recursive.
     */
    public void setRecursive(boolean v) {
        this.recursive = v;
    }

    /**
     * Get the value of forced.
     * @return value of forced.
     */
    public boolean isForced() {
        return this.forced;
    }

    /**
     * Flag to force actions regardless of the status 
     * that StarTeam is maintaining for the file; optional, default false.  
     * If <tt>rootlocalfolder</tt> is set then 
     * this should be set "true" as otherwise the checkout will be based on statuses 
     * which do not relate to the target folder.  
     * @param v  Value to assign to forced.
     */
    public void setForced(boolean v) {
        this.forced = v;
    }

    /**
     *  returns true if a label has been specified and it is a view label.
     * 
     * @return  true if a label has been specified and it is a view label
     */
    protected boolean isUsingViewLabel() {
        return null != this.labelInUse && 
            this.labelInUse.isViewLabel();
    }
    /**
     *  returns true if a label has been specified and it is a revision label.
     * 
     * @return  true if a label has been specified and it is a revision label
     */
    protected boolean isUsingRevisionLabel() {
        return null != this.labelInUse && 
            this.labelInUse.isRevisionLabel();
    }

    /**
     * returns the label being used
     * 
     * @return 
     */
    protected Label getLabelInUse() {
        return this.labelInUse;
    }

    /**
     * show the label in the log and its type.
     */
    protected void logLabel() {
        if (this.isUsingViewLabel()) {
            log("  Using view label " + getLabel());
        }
        else if (this.isUsingRevisionLabel()) {
            log("  Using revision label " + getLabel());
        }
    }




    ///////////////////////////////////////////////////////////////
    // INCLUDE-EXCLUDE processing
    ///////////////////////////////////////////////////////////////

    /**
     * Look if the file should be processed by the task.
     * Don't process it if it fits no include filters or if
     * it fits an exclude filter.
     * 
     * @param pName  the item name to look for being included.
     * 
     * @return whether the file should be processed or not.
     */
    protected boolean shouldProcess(String pName) {
        boolean includeIt = matchPatterns(getIncludes(), pName);
        boolean excludeIt = matchPatterns(getExcludes(), pName);
        return (includeIt && !excludeIt);
    }

    /**
     * Convenience method to see if a string match a one pattern
     * in given set of space-separated patterns.
     * @param patterns the space-separated list of patterns.
     * @param pName the name to look for matching.
     * @return whether the name match at least one pattern.
     */
    protected boolean matchPatterns(String patterns, String pName) {
        if (patterns == null) {
            return false;
        }
        StringTokenizer exStr = new StringTokenizer(patterns, ",");
        while (exStr.hasMoreTokens()) {
            if (DirectoryScanner.match(exStr.nextToken(), pName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds and opens the root starteam folder of the operation specified
     * by this task.  This will be one of the following cases:
     * 
     * @return Starteam's root folder for the operation.
     * @exception BuildException
     *                   if the root folder cannot be found in the repository
     */
    private final Folder configureRootStarteamFolder() 
        throws BuildException 
    {
        Folder starteamrootfolder = null;
        try {
            // no root local mapping has been specified.
            View snapshot = openView();

            // find the starteam folder specified to be the root of the
            // operation.  Throw if it can't be found.

            starteamrootfolder =
                    StarTeamFinder.findFolder(snapshot.getRootFolder(),
                            this.rootStarteamFolder);

        } 
        catch (BuildException e) {
            throw e;
        }
        catch (Exception e) {
            throw new BuildException(
                "Unable to find root folder " + this.rootStarteamFolder + 
                " in repository at " + getURL(), e);

        }

        if (null == starteamrootfolder) {
            throw new BuildException(
                "Unable to find root folder " + this.rootStarteamFolder + 
                " in repository at " + getURL());
        }  
        
        return starteamrootfolder;
    }
    
    /**
     * Returns the local folder mapped to the given StarTeam root folder
     * of the operation.  There are two cases here, depending on whether
     * <code>rootLocalFolder</code> is defined.  
     * If <code>rootLocalFolder</code> is defined, it will be used to 
     * establish a root mapping.  Otherwise, the repository's default root 
     * folder will be used.
     * 
     * @param starteamrootfolder
     *               root Starteam folder initialized for the operation
     * 
     * @return the local folder corresponding to the root Starteam folder.
     * @see findRootStarteamFolder
     */
    private final java.io.File getLocalRootMapping(Folder starteamrootfolder) {
        // set the local folder.
        String localrootfolder;
        if (null != this.rootLocalFolder) {
            localrootfolder = rootLocalFolder;
        }
        else  {
            // either use default path or root local mapping,
            // which is now embedded in the root folder
            localrootfolder = starteamrootfolder.getPathFragment();
        }

        return new java.io.File(localrootfolder);
        
    }

    /**
     * extenders should emit to the log an entry describing the parameters
     * that will be used by this operation.
     * 
     * @param starteamrootFolder
     *               root folder in StarTeam for the operation
     * @param targetrootFolder
     *               root local folder for the operation (whether specified by the user or not.
     */
    protected abstract void logOperationDescription( 
        Folder starteamrootFolder, java.io.File targetrootFolder);

    /**
     * This method does the work of opening the supplied  Starteam view and
     * calling the <code>visit()</code> method to perform the task.
     * Derived classes can customize the called methods 
     * <code>testPreconditions()</code> and <code>visit()</code>.
     *
     * @exception BuildException if any error occurs in the processing
     * @see <code>testPreconditions()</code>
     * @see <code>visit()</code>
     */

    public final void execute() throws BuildException {
        try {

            Folder starteamrootfolder = configureRootStarteamFolder();

            // set the local folder.
            java.io.File localrootfolder = 
                getLocalRootMapping(starteamrootfolder);

            testPreconditions();
            
            // Tell user what he is doing
            logOperationDescription(starteamrootfolder, localrootfolder);
            
            // Inspect everything in the root folder and then recursively
            visit(starteamrootfolder, localrootfolder);

        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private void findLabel(View v) throws BuildException {
        Label[] allLabels = v.getLabels();
        for (int i = 0; i < allLabels.length; i++) {
            Label stLabel = allLabels[i];
            log("checking label " + stLabel.getName(), Project.MSG_DEBUG);
            if (stLabel != null && !stLabel.isDeleted() && stLabel.getName().equals(this.label)) {
                if (!stLabel.isRevisionLabel() && !stLabel.isViewLabel()) {
                    throw new BuildException("Unexpected label type.");
                }
                log("using label " + stLabel.getName(), Project.MSG_DEBUG);
                this.labelInUse = stLabel;
                return;
            }
        }
        throw new BuildException("Error: label "
                + this.label
                + " does not exist in view "
                + v.getFullName());

    }

    /**
     * Helper method calls on the StarTeam API to retrieve an ID number
     * for the specified view, corresponding to this.label.
     * @param v the <code>View</code> in which to search for <code>this.label</code>
     * @return the ID number corresponding to <code>this.label</code> or -1 if
     *         no label was provided.
     * @exception BuildException if <code>this.label</code> does not correspond
     *                           to any label in the supplied view
     */
    protected int getLabelID(View v) throws BuildException {
        if (null != this.label) {
            findLabel(v);
            return this.labelInUse.getID();
        }
        return -1;
    }

    protected int getIDofLabelInUse() {
        if (null != this.labelInUse) {
            return this.labelInUse.getID();
        }
        return -1;
    }

    /**
     * Derived classes must override this class to define actual processing
     * to be performed on each folder in the tree defined for the task
     * 
     * @param rootStarteamFolder
     *               the StarTeam folderto be visited
     * @param rootLocalFolder
     *               the local mapping of rootStarteamFolder
     * 
     * @exception BuildException
     */
    protected abstract void visit(Folder rootStarteamFolder,
                                  java.io.File rootLocalFolder)
            throws BuildException;




    /**
     * Derived classes must override this method to define tests for
     * any preconditons required by the task.  This method is called at
     * the beginning of the execute() method.
     *
     * @exception BuildException throw if any fatal error exists in the
     * parameters supplied.  If there is a non-fatal condition, just writing
     * to the log may be appropriate.
     * @see <code>execute()</code>
     */
    protected abstract void testPreconditions() throws BuildException;


    /**
     * Return the full repository path name of a file.  Surprisingly there's
     * no method in com.starbase.starteam.File to provide this.
     * 
     * @param remotefile the Star Team file whose path is to be returned
     * 
     * @return the full repository path name of a file.
     */
    public static String getFullRepositoryPath(
        com.starbase.starteam.File remotefile)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(remotefile.getParentFolderHierarchy())
          .append(remotefile.getName());
        return sb.toString();
    }

    /**
     * This class implements a map of existing local files to possibly
     * existing repository files.  The map is created by a TreeBasedTask
     * upon recursing into a directory.  Each local item is mapped to an
     * unattached StarTeam object of the proper type, File->File and
     * Directory->Folder.
     * 
     * As the TreeBased does its work, it deletes from the map all items
     * it has processed.  
     * 
     * When the TreeBased task processes all the items from the repository,
     * whatever items left in the UnmatchedFileMap are uncontrolled items
     * and can be processed as appropriate to the task.  In the case of
     * Checkouts, they can be optionally deleted from the local tree.  In the 
     * case of Checkins they can optionally be added to the resository.
     */
    protected abstract class UnmatchedFileMap extends Hashtable {
    
        /**
         * initializes the UnmatchedFileMap with entries from the local folder
         * These will be mapped to the corresponding StarTeam entry even though
         * it will not, in fact, exist in the repository.  But through it, it 
         * can be added, listed, etc.
         * 
         * @param localFolder
         *        the local folder from which the mappings will be made.
         * @param remoteFolder
         *        the corresponding StarTeam folder which will be processed.
         */
        UnmatchedFileMap init(java.io.File localFolder, Folder remoteFolder) {
            if (!localFolder.exists()) {
                return this;
            }

            String[] localFiles = localFolder.list();
    
            for (int i=0; i < localFiles.length; i++) {
                String fn = localFiles[i];
                java.io.File localFile = 
                    new java.io.File(localFolder, localFiles[i]).getAbsoluteFile();
                
                log("adding " + localFile + " to UnmatchedFileMap",
                    Project.MSG_DEBUG);
    
                if (localFile.isDirectory()) {
                    this.put(localFile, new Folder( remoteFolder, fn, fn));
                } 
                else {
                    com.starbase.starteam.File remoteFile = 
                        new com.starbase.starteam.File(remoteFolder);
                    remoteFile.setName(fn);
                    this.put(localFile, remoteFile);
                }
            }
            return this;
        }
    
        /**
         * remove an item found to be controlled from the map.
         * 
         * @param localFile the local item found to be controlled.
         */
        void removeControlledItem(java.io.File localFile) {
            if (isActive()) {
                log("removing processed " + localFile.getAbsoluteFile() + 
                    " from UnmatchedFileMap", Project.MSG_DEBUG);
                this.remove(localFile.getAbsoluteFile());
            }
        }
        /**
         * override will perform the action appropriate for its task to perform
         * on items which are on the local tree but not in StarTeam.  It is 
         * assumed that this method will not be called until all the items in
         * the corresponding folder have been processed, and that the internal 
         * map * will contain only uncontrolled items.
         */
        abstract void processUncontrolledItems() throws BuildException;
    
        /**
         * overrides must define this to declare how this method knows if it 
         * is active.  This presents extra clock cycles when the functionality
         * is not called for.
         * 
         * @return True if this object is to perform its functionality.
         */
        abstract protected boolean isActive();
    
    }

}



