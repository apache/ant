/*
 * Copyright  2002-2004 The Apache Software Foundation
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
package org.apache.tools.ant.types;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.selectors.AndSelector;
import org.apache.tools.ant.types.selectors.ContainsSelector;
import org.apache.tools.ant.types.selectors.DateSelector;
import org.apache.tools.ant.types.selectors.DependSelector;
import org.apache.tools.ant.types.selectors.DepthSelector;
import org.apache.tools.ant.types.selectors.ExtendSelector;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.DifferentSelector;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.TypeSelector;
import org.apache.tools.ant.types.selectors.MajoritySelector;
import org.apache.tools.ant.types.selectors.NoneSelector;
import org.apache.tools.ant.types.selectors.NotSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
import org.apache.tools.ant.types.selectors.PresentSelector;
import org.apache.tools.ant.types.selectors.ContainsRegexpSelector;
import org.apache.tools.ant.types.selectors.SelectSelector;
import org.apache.tools.ant.types.selectors.SelectorContainer;
import org.apache.tools.ant.types.selectors.SelectorScanner;
import org.apache.tools.ant.types.selectors.SizeSelector;
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
    private Vector additionalPatterns = new Vector();
    private Vector selectors = new Vector();

    private File dir;
    private boolean useDefaultExcludes = true;
    private boolean isCaseSensitive = true;
    private boolean followSymlinks = true;

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
        this.isCaseSensitive = fileset.isCaseSensitive;
        this.followSymlinks = fileset.followSymlinks;
        setProject(fileset.getProject());
    }

    /**
     * Makes this instance in effect a reference to another instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     * @param r the <code>Reference</code> to use.
     */
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
     */
    public void setDir(File dir) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.dir = dir;
    }

    /**
     * Retrieves the base-directory for this instance.
     * @param p the <code>Project</code> against which the
     *          reference is resolved, if set.
     * @return <code>File</code>.
     */
    public File getDir(Project p) {
        return (isReference()) ? getRef(p).getDir(p) : dir;
    }

    /**
     * Creates a nested patternset.
     * @return <code>PatternSet</code>.
     */
    public PatternSet createPatternSet() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        PatternSet patterns = new PatternSet();
        additionalPatterns.addElement(patterns);
        return patterns;
    }

    /**
     * Add a name entry to the include list.
     * @return <code>PatternSet.NameEntry</code>.
     */
    public PatternSet.NameEntry createInclude() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createInclude();
    }

    /**
     * Add a name entry to the include files list.
     * @return <code>PatternSet.NameEntry</code>.
     */
    public PatternSet.NameEntry createIncludesFile() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createIncludesFile();
    }

    /**
     * Add a name entry to the exclude list.
     * @return <code>PatternSet.NameEntry</code>.
     */
    public PatternSet.NameEntry createExclude() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createExclude();
    }

    /**
     * Add a name entry to the excludes files list.
     * @return <code>PatternSet.NameEntry</code>.
     */
    public PatternSet.NameEntry createExcludesFile() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createExcludesFile();
    }

    /**
     * Creates a single file fileset.
     * @param file the single <code>File</code> included in this
     *             <code>AbstractFileSet</code>.
     */
    public void setFile(File file) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        setDir(file.getParentFile());
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
    public void setIncludes(String includes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        defaultPatterns.setIncludes(includes);
    }

    /**
     * Appends <code>excludes</code> to the current list of exclude
     * patterns.
     *
     * <p>Patterns may be separated by a comma or a space.</p>
     *
     * @param excludes the <code>String</code> containing the exclude patterns.
     */
    public void setExcludes(String excludes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        defaultPatterns.setExcludes(excludes);
    }

    /**
     * Sets the <code>File</code> containing the includes patterns.
     *
     * @param incl <code>File</code> instance.
     */
     public void setIncludesfile(File incl) throws BuildException {
         if (isReference()) {
             throw tooManyAttributes();
         }
         defaultPatterns.setIncludesfile(incl);
     }

    /**
     * Sets the <code>File</code> containing the excludes patterns.
     *
     * @param excl <code>File</code> instance.
     */
     public void setExcludesfile(File excl) throws BuildException {
         if (isReference()) {
             throw tooManyAttributes();
         }
         defaultPatterns.setExcludesfile(excl);
     }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes <code>boolean</code>.
     */
    public void setDefaultexcludes(boolean useDefaultExcludes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * Sets case sensitivity of the file system.
     *
     * @param isCaseSensitive <code>boolean</code>.
     */
    public void setCaseSensitive(boolean isCaseSensitive) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.isCaseSensitive = isCaseSensitive;
    }

    /**
     * Sets whether or not symbolic links should be followed.
     *
     * @param followSymlinks whether or not symbolic links should be followed.
     */
    public void setFollowSymlinks(boolean followSymlinks) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.followSymlinks = followSymlinks;
    }

    /**
     * Find out if the fileset wants to follow symbolic links.
     *
     * @return <code>boolean</code> indicating whether symbolic links
     *         should be followed.
     *
     * @since Ant 1.6
     */
    public boolean isFollowSymlinks() {
        return (isReference())
            ? getRef(getProject()).isFollowSymlinks() : followSymlinks;
    }

    /**
     * Gets as descriptive as possible a name used for this datatype instance.
     * @return <code>String</code> name.
     */
    protected String getDataTypeName() {
        // look up the types in project and see if they match this class
        Project p = getProject();
        if (p != null) {
            Hashtable typedefs = p.getDataTypeDefinitions();
            for (Enumeration e = typedefs.keys(); e.hasMoreElements();) {
                String typeName = (String) e.nextElement();
                Class typeClass = (Class) typedefs.get(typeName);
                if (typeClass == getClass()) {
                    return typeName;
                }
            }
        }
        String classname = getClass().getName();
        return classname.substring(classname.lastIndexOf('.') + 1);
    }

    /**
     * Returns the directory scanner needed to access the files to process.
     * @return a <code>DirectoryScanner</code> instance.
     */
    public DirectoryScanner getDirectoryScanner(Project p) {
        if (isReference()) {
            return getRef(p).getDirectoryScanner(p);
        }
        if (dir == null) {
            throw new BuildException("No directory specified for "
                                     + getDataTypeName() + ".");
        }
        if (!dir.exists()) {
            throw new BuildException(dir.getAbsolutePath() + " not found.");
        }
        if (!dir.isDirectory()) {
            throw new BuildException(dir.getAbsolutePath()
                                     + " is not a directory.");
        }
        DirectoryScanner ds = new DirectoryScanner();
        setupDirectoryScanner(ds, p);
        ds.setFollowSymlinks(followSymlinks);
        ds.scan();
        return ds;
    }

    /**
     * Set up the specified directory scanner against the specified project.
     * @param ds a <code>FileScanner</code> instance.
     * @param p an Ant <code>Project</code> instance.
     */
    public void setupDirectoryScanner(FileScanner ds, Project p) {
        if (isReference()) {
            getRef(p).setupDirectoryScanner(ds, p);
            return;
        }
        if (ds == null) {
            throw new IllegalArgumentException("ds cannot be null");
        }
        ds.setBasedir(dir);

        final int count = additionalPatterns.size();
        for (int i = 0; i < count; i++) {
            Object o = additionalPatterns.elementAt(i);
            defaultPatterns.append((PatternSet) o, p);
        }
        p.log(getDataTypeName() + ": Setup scanner in dir " + dir
            + " with " + defaultPatterns, Project.MSG_DEBUG);

        ds.setIncludes(defaultPatterns.getIncludePatterns(p));
        ds.setExcludes(defaultPatterns.getExcludePatterns(p));
        if (ds instanceof SelectorScanner) {
            SelectorScanner ss = (SelectorScanner) ds;
            ss.setSelectors(getSelectors(p));
        }
        if (useDefaultExcludes) {
            ds.addDefaultExcludes();
        }
        ds.setCaseSensitive(isCaseSensitive);
    }

    /**
     * Performs the check for circular references and returns the
     * referenced FileSet.
     */
    protected AbstractFileSet getRef(Project p) {
        if (!isChecked()) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }
        Object o = getRefid().getReferencedObject(p);
        if (!getClass().isAssignableFrom(o.getClass())) {
            throw new BuildException(getRefid().getRefId()
                + " doesn\'t denote a " + getDataTypeName());
        }
        return (AbstractFileSet) o;
    }

    // SelectorContainer methods

    /**
     * Indicates whether there are any selectors here.
     *
     * @return whether any selectors are in this container.
     */
    public boolean hasSelectors() {
        return (isReference() && getProject() != null)
            ? getRef(getProject()).hasSelectors() : !(selectors.isEmpty());
    }

    /**
     * Indicates whether there are any patterns here.
     *
     * @return whether any patterns are in this container.
     */
    public boolean hasPatterns() {
        if (isReference() && getProject() != null) {
            return getRef(getProject()).hasPatterns();
        }
        if (defaultPatterns.hasPatterns(getProject())) {
            return true;
        }
        Enumeration e = additionalPatterns.elements();
        while (e.hasMoreElements()) {
            PatternSet ps = (PatternSet) e.nextElement();
            if (ps.hasPatterns(getProject())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives the count of the number of selectors in this container.
     *
     * @return the number of selectors in this container as an <code>int</code>.
     */
    public int selectorCount() {
        return (isReference() && getProject() != null)
            ? getRef(getProject()).selectorCount() : selectors.size();
    }

    /**
     * Returns the set of selectors as an array.
     *
     * @return a <code>FileSelector[]</code> of the selectors in this container.
     */
    public FileSelector[] getSelectors(Project p) {
        return (isReference())
            ? getRef(p).getSelectors(p) : (FileSelector[])(selectors.toArray(
            new FileSelector[selectors.size()]));
    }

    /**
     * Returns an enumerator for accessing the set of selectors.
     *
     * @return an <code>Enumeration</code> of selectors.
     */
    public Enumeration selectorElements() {
        return (isReference() && getProject() != null)
            ? getRef(getProject()).selectorElements() : selectors.elements();
    }

    /**
     * Add a new selector into this container.
     *
     * @param selector the new <code>FileSelector</code> to add.
     */
    public void appendSelector(FileSelector selector) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        selectors.addElement(selector);
    }

    /* Methods below all add specific selectors */

    /**
     * Add a "Select" selector entry on the selector list.
     * @param selector the <code>SelectSelector</code> to add.
     */
    public void addSelector(SelectSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add an "And" selector entry on the selector list.
     * @param selector the <code>AndSelector</code> to add.
     */
    public void addAnd(AndSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add an "Or" selector entry on the selector list.
     * @param selector the <code>OrSelector</code> to add.
     */
    public void addOr(OrSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a "Not" selector entry on the selector list.
     * @param selector the <code>NotSelector</code> to add.
     */
    public void addNot(NotSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a "None" selector entry on the selector list.
     * @param selector the <code>NoneSelector</code> to add.
     */
    public void addNone(NoneSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a majority selector entry on the selector list.
     * @param selector the <code>MajoritySelector</code> to add.
     */
    public void addMajority(MajoritySelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a selector date entry on the selector list.
     * @param selector the <code>DateSelector</code> to add.
     */
    public void addDate(DateSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a selector size entry on the selector list.
     * @param selector the <code>SizeSelector</code> to add.
     */
    public void addSize(SizeSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a DifferentSelector entry on the selector list.
     * @param selector the <code>DifferentSelector</code> to add.
     */
    public void addDifferent(DifferentSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a selector filename entry on the selector list.
     * @param selector the <code>FilenameSelector</code> to add.
     */
    public void addFilename(FilenameSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a selector type entry on the selector list.
     * @param selector the <code>TypeSelector</code> to add.
     */
    public void addType(TypeSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add an extended selector entry on the selector list.
     * @param selector the <code>ExtendSelector</code> to add.
     */
    public void addCustom(ExtendSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a contains selector entry on the selector list.
     * @param selector the <code>ContainsSelector</code> to add.
     */
    public void addContains(ContainsSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a present selector entry on the selector list.
     * @param selector the <code>PresentSelector</code> to add.
     */
    public void addPresent(PresentSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a depth selector entry on the selector list.
     * @param selector the <code>DepthSelector</code> to add.
     */
    public void addDepth(DepthSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a depends selector entry on the selector list.
     * @param selector the <code>DependSelector</code> to add.
     */
    public void addDepend(DependSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add a regular expression selector entry on the selector list.
     * @param selector the <code>ContainsRegexpSelector</code> to add.
     */
    public void addContainsRegexp(ContainsRegexpSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add the modified selector.
     * @param selector the <code>ModifiedSelector</code> to add.
     * @since ant 1.6
     */
    public void addModified(ModifiedSelector selector) {
        appendSelector(selector);
    }

    /**
     * Add an arbitary selector.
     * @param selector the <code>FileSelector</code> to add.
     * @since Ant 1.6
     */
    public void add(FileSelector selector) {
        appendSelector(selector);
    }

    /**
     * Returns included files as a list of semicolon-separated filenames.
     *
     * @return a <code>String</code> of included filenames.
     */
    public String toString() {
        DirectoryScanner ds = getDirectoryScanner(getProject());
        String[] files = ds.getIncludedFiles();
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < files.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(files[i]);
        }
        return sb.toString();
    }

    /**
     * Creates a deep clone of this instance, except for the nested
     * selectors (the list of selectors is a shallow clone of this
     * instance's list).
     *
     * @since Ant 1.6
     */
    public Object clone() {
        if (isReference()) {
            return (getRef(getProject())).clone();
        } else {
            try {
                AbstractFileSet fs = (AbstractFileSet) super.clone();
                fs.defaultPatterns = (PatternSet) defaultPatterns.clone();
                fs.additionalPatterns = new Vector(additionalPatterns.size());
                Enumeration e = additionalPatterns.elements();
                while (e.hasMoreElements()) {
                    fs.additionalPatterns
                        .addElement(((PatternSet) e.nextElement()).clone());
                }
                fs.selectors = (Vector) fs.selectors.clone();
                return fs;
            } catch (CloneNotSupportedException e) {
                throw new BuildException(e);
            }
        }
    }

}
