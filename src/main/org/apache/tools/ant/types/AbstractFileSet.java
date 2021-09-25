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
package org.apache.tools.ant.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.selectors.AndSelector;
import org.apache.tools.ant.types.selectors.ContainsRegexpSelector;
import org.apache.tools.ant.types.selectors.ContainsSelector;
import org.apache.tools.ant.types.selectors.DateSelector;
import org.apache.tools.ant.types.selectors.DependSelector;
import org.apache.tools.ant.types.selectors.DepthSelector;
import org.apache.tools.ant.types.selectors.DifferentSelector;
import org.apache.tools.ant.types.selectors.ExecutableSelector;
import org.apache.tools.ant.types.selectors.ExtendSelector;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.MajoritySelector;
import org.apache.tools.ant.types.selectors.NoneSelector;
import org.apache.tools.ant.types.selectors.NotSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
import org.apache.tools.ant.types.selectors.OwnedBySelector;
import org.apache.tools.ant.types.selectors.PosixGroupSelector;
import org.apache.tools.ant.types.selectors.PosixPermissionsSelector;
import org.apache.tools.ant.types.selectors.PresentSelector;
import org.apache.tools.ant.types.selectors.ReadableSelector;
import org.apache.tools.ant.types.selectors.SelectSelector;
import org.apache.tools.ant.types.selectors.SelectorContainer;
import org.apache.tools.ant.types.selectors.SelectorScanner;
import org.apache.tools.ant.types.selectors.SizeSelector;
import org.apache.tools.ant.types.selectors.SymlinkSelector;
import org.apache.tools.ant.types.selectors.TypeSelector;
import org.apache.tools.ant.types.selectors.WritableSelector;
import org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector;

/**
 * Class that holds an implicit patternset and supports nested
 * patternsets and creates a DirectoryScanner using these patterns.
 *
 * <p>Common base class for DirSet and FileSet.</p>
 *
 */
public abstract class AbstractFileSet extends DataType
    implements Cloneable, SelectorContainer {

    private PatternSet defaultPatterns = new PatternSet();
    private List<PatternSet> additionalPatterns = new ArrayList<>();
    private List<FileSelector> selectors = new ArrayList<>();

    private File dir;
    private boolean fileAttributeUsed;
    private boolean useDefaultExcludes = true;
    private boolean caseSensitive = true;
    private boolean followSymlinks = true;
    private boolean errorOnMissingDir = true;
    private int maxLevelsOfSymlinks = DirectoryScanner.MAX_LEVELS_OF_SYMLINKS;

    /* cached DirectoryScanner instance for our own Project only */
    private DirectoryScanner directoryScanner = null;

    /**
     * Construct a new <code>AbstractFileSet</code>.
     */
    public AbstractFileSet() {
        super();
    }

    /**
     * Construct a new <code>AbstractFileSet</code>, shallowly cloned
     * from the specified <code>AbstractFileSet</code>.
     * @param fileset the <code>AbstractFileSet</code> to use as a template.
     */
    protected AbstractFileSet(AbstractFileSet fileset) {
        this.dir = fileset.dir;
        this.defaultPatterns = fileset.defaultPatterns;
        this.additionalPatterns = fileset.additionalPatterns;
        this.selectors = fileset.selectors;
        this.useDefaultExcludes = fileset.useDefaultExcludes;
        this.caseSensitive = fileset.caseSensitive;
        this.followSymlinks = fileset.followSymlinks;
        this.errorOnMissingDir = fileset.errorOnMissingDir;
        this.maxLevelsOfSymlinks = fileset.maxLevelsOfSymlinks;
        setProject(fileset.getProject());
    }

    /**
     * Makes this instance in effect a reference to another instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     * @param r the <code>Reference</code> to use.
     * @throws BuildException on error
     */
    @Override
    public void setRefid(Reference r) throws BuildException {
        if (dir != null || defaultPatterns.hasPatterns(getProject())) {
            throw tooManyAttributes();
        }
        if (!additionalPatterns.isEmpty()) {
            throw noChildrenAllowed();
        }
        if (!selectors.isEmpty()) {
            throw noChildrenAllowed();
        }
        super.setRefid(r);
    }

    /**
     * Sets the base-directory for this instance.
     * @param dir the directory's <code>File</code> instance.
     * @throws BuildException on error
     */
    public synchronized void setDir(File dir) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (fileAttributeUsed && !getDir().equals(dir)) {
            throw dirAndFileAreMutuallyExclusive();
        }
        this.dir = dir;
        directoryScanner = null;
    }

    /**
     * Retrieves the base-directory for this instance.
     * @return <code>File</code>.
     */
    public File getDir() {
        return getDir(getProject());
    }

    /**
     * Retrieves the base-directory for this instance.
     * @param p the <code>Project</code> against which the
     *          reference is resolved, if set.
     * @return <code>File</code>.
     */
    public synchronized File getDir(Project p) {
        if (isReference()) {
            return getRef(p).getDir(p);
        }
        dieOnCircularReference();
        return dir;
    }

    /**
     * Creates a nested patternset.
     * @return <code>PatternSet</code>.
     */
    public synchronized PatternSet createPatternSet() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        PatternSet patterns = new PatternSet();
        additionalPatterns.add(patterns);
        directoryScanner = null;
        return patterns;
    }

    /**
     * Add a name entry to the include list.
     * @return <code>PatternSet.NameEntry</code>.
     */
    public synchronized PatternSet.NameEntry createInclude() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        directoryScanner = null;
        return defaultPatterns.createInclude();
    }

    /**
     * Add a name entry to the include files list.
     * @return <code>PatternSet.PatternFileNameEntry</code>.
     */
    public synchronized PatternSet.NameEntry createIncludesFile() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        directoryScanner = null;
        return defaultPatterns.createIncludesFile();
    }

    /**
     * Add a name entry to the exclude list.
     * @return <code>PatternSet.NameEntry</code>.
     */
    public synchronized PatternSet.NameEntry createExclude() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        directoryScanner = null;
        return defaultPatterns.createExclude();
    }

    /**
     * Add a name entry to the excludes files list.
     * @return <code>PatternSet.PatternFileNameEntry</code>.
     */
    public synchronized PatternSet.NameEntry createExcludesFile() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        directoryScanner = null;
        return defaultPatterns.createExcludesFile();
    }

    /**
     * Creates a single file fileset.
     * @param file the single <code>File</code> included in this
     *             <code>AbstractFileSet</code>.
     */
    public synchronized void setFile(File file) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (fileAttributeUsed) {
            if (getDir().equals(file.getParentFile())) {
                String[] includes = defaultPatterns.getIncludePatterns(getProject());
                if (includes.length == 1 && includes[0].equals(file.getName())) {
                    // NOOP, setFile has been invoked twice with the same parameter
                    return;
                }
            }
            throw new BuildException("setFile cannot be called twice with different arguments");
        } else if (getDir() != null) {
            throw dirAndFileAreMutuallyExclusive();
        }
        setDir(file.getParentFile());
        fileAttributeUsed = true;
        createInclude().setName(file.getName());
    }

    /**
     * Appends <code>includes</code> to the current list of include
     * patterns.
     *
     * <p>Patterns may be separated by a comma or a space.</p>
     *
     * @param includes the <code>String</code> containing the include patterns.
     */
    public synchronized void setIncludes(String includes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        defaultPatterns.setIncludes(includes);
        directoryScanner = null;
    }

    /**
     * Appends <code>includes</code> to the current list of include
     * patterns.
     *
     * @param includes array containing the include patterns.
     * @since Ant 1.7
     */
    public synchronized void appendIncludes(String[] includes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (includes != null) {
            for (String include : includes) {
                defaultPatterns.createInclude().setName(include);
            }
            directoryScanner = null;
        }
    }

    /**
     * Appends <code>excludes</code> to the current list of exclude
     * patterns.
     *
     * <p>Patterns may be separated by a comma or a space.</p>
     *
     * @param excludes the <code>String</code> containing the exclude patterns.
     */
    public synchronized void setExcludes(String excludes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        defaultPatterns.setExcludes(excludes);
        directoryScanner = null;
    }

    /**
     * Appends <code>excludes</code> to the current list of exclude
     * patterns.
     *
     * @param excludes array containing the exclude patterns.
     * @since Ant 1.7
     */
    public synchronized void appendExcludes(String[] excludes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (excludes != null) {
            for (String exclude : excludes) {
                defaultPatterns.createExclude().setName(exclude);
            }
            directoryScanner = null;
        }
    }

    /**
     * Sets the <code>File</code> containing the includes patterns.
     *
     * @param incl <code>File</code> instance.
     * @throws BuildException on error
     */
    public synchronized void setIncludesfile(File incl) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        defaultPatterns.setIncludesfile(incl);
        directoryScanner = null;
    }

    /**
     * Sets the <code>File</code> containing the excludes patterns.
     *
     * @param excl <code>File</code> instance.
     * @throws BuildException on error
     */
    public synchronized void setExcludesfile(File excl) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        defaultPatterns.setExcludesfile(excl);
        directoryScanner = null;
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes <code>boolean</code>.
     */
    public synchronized void setDefaultexcludes(boolean useDefaultExcludes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.useDefaultExcludes = useDefaultExcludes;
        directoryScanner = null;
    }

    /**
     * Whether default exclusions should be used or not.
     * @return the default exclusions value.
     * @since Ant 1.6.3
     */
    public synchronized boolean getDefaultexcludes() {
        if (isReference()) {
            return getRef(getProject()).getDefaultexcludes();
        }
        dieOnCircularReference();
        return useDefaultExcludes;
    }

    /**
     * Sets case sensitivity of the file system.
     *
     * @param caseSensitive <code>boolean</code>.
     */
    public synchronized void setCaseSensitive(boolean caseSensitive) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.caseSensitive = caseSensitive;
        directoryScanner = null;
    }

    /**
     * Find out if the fileset is case sensitive.
     *
     * @return <code>boolean</code> indicating whether the fileset is
     * case sensitive.
     *
     * @since Ant 1.7
     */
    public synchronized boolean isCaseSensitive() {
        if (isReference()) {
            return getRef(getProject()).isCaseSensitive();
        }
        dieOnCircularReference();
        return caseSensitive;
    }

    /**
     * Sets whether or not symbolic links should be followed.
     *
     * @param followSymlinks whether or not symbolic links should be followed.
     */
    public synchronized void setFollowSymlinks(boolean followSymlinks) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.followSymlinks = followSymlinks;
        directoryScanner = null;
    }

    /**
     * Find out if the fileset wants to follow symbolic links.
     *
     * @return <code>boolean</code> indicating whether symbolic links
     *         should be followed.
     *
     * @since Ant 1.6
     */
    public synchronized boolean isFollowSymlinks() {
        if (isReference()) {
            return getRef(getProject()).isCaseSensitive();
        }
        dieOnCircularReference();
        return followSymlinks;
    }

    /**
     * The maximum number of times a symbolic link may be followed
     * during a scan.
     *
     * @param max int
     * @since Ant 1.8.0
     */
    public void setMaxLevelsOfSymlinks(int max) {
        maxLevelsOfSymlinks = max;
    }

    /**
     * The maximum number of times a symbolic link may be followed
     * during a scan.
     *
     * @return int
     * @since Ant 1.8.0
     */
    public int getMaxLevelsOfSymlinks() {
        return maxLevelsOfSymlinks;
    }

    /**
     * Sets whether an error is thrown if a directory does not exist.
     *
     * @param errorOnMissingDir true if missing directories cause errors,
     *                        false if not.
     */
     public void setErrorOnMissingDir(boolean errorOnMissingDir) {
         this.errorOnMissingDir = errorOnMissingDir;
     }

    /**
     * Gets whether an error is/should be thrown if the base directory
     * does not exist.
     *
     * @return boolean
     * @since Ant 1.8.2
     */
     public boolean getErrorOnMissingDir() {
         return errorOnMissingDir;
     }

    /**
     * Returns the directory scanner needed to access the files to process.
     * @return a <code>DirectoryScanner</code> instance.
     */
    public DirectoryScanner getDirectoryScanner() {
        return getDirectoryScanner(getProject());
    }

    /**
     * Returns the directory scanner needed to access the files to process.
     * @param p the Project against which the DirectoryScanner should be configured.
     * @return a <code>DirectoryScanner</code> instance.
     */
    public DirectoryScanner getDirectoryScanner(Project p) {
        if (isReference()) {
            return getRef(p).getDirectoryScanner(p);
        }
        dieOnCircularReference();
        final DirectoryScanner ds;
        synchronized (this) {
            if (directoryScanner != null && p == getProject()) {
                ds = directoryScanner;
            } else {
                if (dir == null) {
                    throw new BuildException("No directory specified for %s.",
                        getDataTypeName());
                }
                if (!dir.exists() && errorOnMissingDir) {
                    throw new BuildException(dir.getAbsolutePath()
                                             + DirectoryScanner
                                             .DOES_NOT_EXIST_POSTFIX);
                }
                if (!dir.isDirectory() && dir.exists()) {
                    throw new BuildException("%s is not a directory.",
                        dir.getAbsolutePath());
                }
                ds = new DirectoryScanner();
                setupDirectoryScanner(ds, p);
                ds.setFollowSymlinks(followSymlinks);
                ds.setErrorOnMissingDir(errorOnMissingDir);
                ds.setMaxLevelsOfSymlinks(maxLevelsOfSymlinks);
                directoryScanner = (p == getProject()) ? ds : directoryScanner;
            }
        }
        ds.scan();
        return ds;
    }

    /**
     * Set up the specified directory scanner against this
     * AbstractFileSet's Project.
     * @param ds a <code>FileScanner</code> instance.
     */
    public void setupDirectoryScanner(FileScanner ds) {
        setupDirectoryScanner(ds, getProject());
    }

    /**
     * Set up the specified directory scanner against the specified project.
     * @param ds a <code>FileScanner</code> instance.
     * @param p an Ant <code>Project</code> instance.
     */
    public synchronized void setupDirectoryScanner(FileScanner ds, Project p) {
        if (isReference()) {
            getRef(p).setupDirectoryScanner(ds, p);
            return;
        }
        dieOnCircularReference(p);
        if (ds == null) {
            throw new IllegalArgumentException("ds cannot be null");
        }
        ds.setBasedir(dir);

        PatternSet ps = mergePatterns(p);
        p.log(getDataTypeName() + ": Setup scanner in dir " + dir
            + " with " + ps, Project.MSG_DEBUG);

        ds.setIncludes(ps.getIncludePatterns(p));
        ds.setExcludes(ps.getExcludePatterns(p));
        if (ds instanceof SelectorScanner) {
            SelectorScanner ss = (SelectorScanner) ds;
            ss.setSelectors(getSelectors(p));
        }
        if (useDefaultExcludes) {
            ds.addDefaultExcludes();
        }
        ds.setCaseSensitive(caseSensitive);
    }

    /**
     * Performs the check for circular references and returns the
     * referenced FileSet.
     * This method can be overridden together with {@link ArchiveFileSet#getRef() getRef()}
     * providing implementations containing the special support
     * for FileSet references, which can be handled by all ArchiveFileSets.
     * NB! This method must be overridden in subclasses such as FileSet and DirSet
     * to distinguish between the data types.
     * @param p the current project
     * @return the dereferenced object.
     */
    protected AbstractFileSet getRef(Project p) {
        return getCheckedRef(AbstractFileSet.class, getDataTypeName(), p);
    }

    // SelectorContainer methods

    /**
     * Indicates whether there are any selectors here.
     *
     * @return whether any selectors are in this container.
     */
    @Override
    public synchronized boolean hasSelectors() {
        if (isReference()) {
            return getRef(getProject()).hasSelectors();
        }
        dieOnCircularReference();
        return !selectors.isEmpty();
    }

    /**
     * Indicates whether there are any patterns here.
     *
     * @return whether any patterns are in this container.
     */
    public synchronized boolean hasPatterns() {
        if (isReference() && getProject() != null) {
            return getRef(getProject()).hasPatterns();
        }
        dieOnCircularReference();
        return defaultPatterns.hasPatterns(getProject())
                || additionalPatterns.stream().anyMatch(ps -> ps.hasPatterns(getProject()));
    }

    /**
     * Gives the count of the number of selectors in this container.
     *
     * @return the number of selectors in this container as an <code>int</code>.
     */
    @Override
    public synchronized int selectorCount() {
        if (isReference()) {
            return getRef(getProject()).selectorCount();
        }
        dieOnCircularReference();
        return selectors.size();
    }

    /**
     * Returns the set of selectors as an array.
     * @param p the current project
     * @return a <code>FileSelector[]</code> of the selectors in this container.
     */
    @Override
    public synchronized FileSelector[] getSelectors(Project p) {
        if (isReference()) {
            return getRef(getProject()).getSelectors(p);
        }
        dieOnCircularReference(p);
        return selectors.toArray(new FileSelector[0]);
    }

    /**
     * Returns an enumerator for accessing the set of selectors.
     *
     * @return an <code>Enumeration</code> of selectors.
     */
    @Override
    public synchronized Enumeration<FileSelector> selectorElements() {
        if (isReference()) {
            return getRef(getProject()).selectorElements();
        }
        dieOnCircularReference();
        return Collections.enumeration(selectors);
    }

    /**
     * Add a new selector into this container.
     *
     * @param selector the new <code>FileSelector</code> to add.
     */
    @Override
    public synchronized void appendSelector(FileSelector selector) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        selectors.add(selector);
        directoryScanner = null;
        setChecked(false);
    }

    /* Methods below all add specific selectors */

    /**
     * Add a "Select" selector entry on the selector list.
     * @param selector the <code>SelectSelector</code> to add.
     */
    @Override
    public void addSelector(SelectSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add an "And" selector entry on the selector list.
     * @param selector the <code>AndSelector</code> to add.
     */
    @Override
    public void addAnd(AndSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add an "Or" selector entry on the selector list.
     * @param selector the <code>OrSelector</code> to add.
     */
    @Override
    public void addOr(OrSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a "Not" selector entry on the selector list.
     * @param selector the <code>NotSelector</code> to add.
     */
    @Override
    public void addNot(NotSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a "None" selector entry on the selector list.
     * @param selector the <code>NoneSelector</code> to add.
     */
    @Override
    public void addNone(NoneSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a majority selector entry on the selector list.
     * @param selector the <code>MajoritySelector</code> to add.
     */
    @Override
    public void addMajority(MajoritySelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a selector date entry on the selector list.
     * @param selector the <code>DateSelector</code> to add.
     */
    @Override
    public void addDate(DateSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a selector size entry on the selector list.
     * @param selector the <code>SizeSelector</code> to add.
     */
    @Override
    public void addSize(SizeSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a DifferentSelector entry on the selector list.
     * @param selector the <code>DifferentSelector</code> to add.
     */
    @Override
    public void addDifferent(DifferentSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a selector filename entry on the selector list.
     * @param selector the <code>FilenameSelector</code> to add.
     */
    @Override
    public void addFilename(FilenameSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a selector type entry on the selector list.
     * @param selector the <code>TypeSelector</code> to add.
     */
    @Override
    public void addType(TypeSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add an extended selector entry on the selector list.
     * @param selector the <code>ExtendSelector</code> to add.
     */
    @Override
    public void addCustom(ExtendSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a contains selector entry on the selector list.
     * @param selector the <code>ContainsSelector</code> to add.
     */
    @Override
    public void addContains(ContainsSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a present selector entry on the selector list.
     * @param selector the <code>PresentSelector</code> to add.
     */
    @Override
    public void addPresent(PresentSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a depth selector entry on the selector list.
     * @param selector the <code>DepthSelector</code> to add.
     */
    @Override
    public void addDepth(DepthSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a depends selector entry on the selector list.
     * @param selector the <code>DependSelector</code> to add.
     */
    @Override
    public void addDepend(DependSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a regular expression selector entry on the selector list.
     * @param selector the <code>ContainsRegexpSelector</code> to add.
     */
    @Override
    public void addContainsRegexp(ContainsRegexpSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add the modified selector.
     * @param selector the <code>ModifiedSelector</code> to add.
     * @since Ant 1.6
     */
    @Override
    public void addModified(ModifiedSelector selector) {
        appendSelector(selector);
    }

    public void addReadable(ReadableSelector r) {
        appendSelector(r);
    }

    public void addWritable(WritableSelector w) {
        appendSelector(w);
    }

    /**
     * @param e ExecutableSelector
     * @since 1.10.0
     */
    public void addExecutable(ExecutableSelector e) {
        appendSelector(e);
    }

    /**
     * @param e SymlinkSelector
     * @since 1.10.0
     */
    public void addSymlink(SymlinkSelector e) {
        appendSelector(e);
    }

    /**
     * @param o OwnedBySelector
     * @since 1.10.0
     */
    public void addOwnedBy(OwnedBySelector o) {
        appendSelector(o);
    }

    /**
     * @param o PosixGroupSelector
     * @since 1.10.4
     */
    public void addPosixGroup(PosixGroupSelector o) {
        appendSelector(o);
    }

    /**
     * @param o PosixPermissionsSelector
     * @since 1.10.4
     */
    public void addPosixPermissions(PosixPermissionsSelector o) {
        appendSelector(o);
    }

    /**
     * Add an arbitrary selector.
     * @param selector the <code>FileSelector</code> to add.
     * @since Ant 1.6
     */
    @Override
    public void add(FileSelector selector) {
        appendSelector(selector);
    }

    /**
     * Returns included files as a list of semicolon-separated filenames.
     *
     * @return a <code>String</code> of included filenames.
     */
    @Override
    public String toString() {
        if (isReference()) {
            return getRef(getProject()).toString();
        }
        dieOnCircularReference();
        return String.join(";", getDirectoryScanner().getIncludedFiles());
    }

    /**
     * Creates a deep clone of this instance, except for the nested
     * selectors (the list of selectors is a shallow clone of this
     * instance's list).
     * @return the cloned object
     * @since Ant 1.6
     */
    @Override
    public synchronized Object clone() {
        if (isReference()) {
            return (getRef(getProject())).clone();
        }
        try {
            AbstractFileSet fs = (AbstractFileSet) super.clone();
            fs.defaultPatterns = (PatternSet) defaultPatterns.clone();
            fs.additionalPatterns = additionalPatterns.stream().map(PatternSet::clone)
                    .map(PatternSet.class::cast).collect(Collectors.toList());
            fs.selectors = new ArrayList<>(selectors);
            return fs;
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Get the merged include patterns for this AbstractFileSet.
     * @param p the project to use.
     * @return the include patterns of the default pattern set and all
     * nested patternsets.
     *
     * @since Ant 1.7
     */
    public String[] mergeIncludes(Project p) {
        return mergePatterns(p).getIncludePatterns(p);
    }

    /**
     * Get the merged exclude patterns for this AbstractFileSet.
     * @param p the project to use.
     * @return the exclude patterns of the default pattern set and all
     * nested patternsets.
     *
     * @since Ant 1.7
     */
    public String[] mergeExcludes(Project p) {
        return mergePatterns(p).getExcludePatterns(p);
    }

    /**
     * Get the merged patterns for this AbstractFileSet.
     * @param p the project to use.
     * @return the default patternset merged with the additional sets
     * in a new PatternSet instance.
     *
     * @since Ant 1.7
     */
    public synchronized PatternSet mergePatterns(Project p) {
        if (isReference()) {
            return getRef(p).mergePatterns(p);
        }
        dieOnCircularReference();
        PatternSet ps = (PatternSet) defaultPatterns.clone();
        additionalPatterns.forEach(pat -> ps.append(pat, p));
        return ps;
    }

    @Override
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            selectors.stream().filter(DataType.class::isInstance).map(DataType.class::cast)
                    .forEach(type -> pushAndInvokeCircularReferenceCheck(type, stk, p));
            additionalPatterns.forEach(ps -> pushAndInvokeCircularReferenceCheck(ps, stk, p));
            setChecked(true);
        }
    }

    private BuildException dirAndFileAreMutuallyExclusive() {
        return new BuildException("you can only specify one of the dir and file attributes");
    }
}
