/*
 * Copyright  2002-2004 Apache Software Foundation
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
 * @author <a href="mailto:ajkuiper@wxs.nl">Arnout J. Kuiper</a>
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author Stefan Bodewig
 * @author Magesh Umasankar
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @author <a href="mailto:martijn@kruithof.xs4all.nl">Martijn Kruithof</a>
 */
public abstract class AbstractFileSet extends DataType implements Cloneable,
        SelectorContainer {

    private PatternSet defaultPatterns = new PatternSet();
    private Vector additionalPatterns = new Vector();
    private Vector selectors = new Vector();

    private File dir;
    private boolean useDefaultExcludes = true;
    private boolean isCaseSensitive = true;
    private boolean followSymlinks = true;

    public AbstractFileSet() {
        super();
    }

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
     */
    public void setDir(File dir) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }

        this.dir = dir;
    }

    /**
     * Retrieves the base-directory for this instance.
     */
    public File getDir(Project p) {
        if (isReference()) {
            return getRef(p).getDir(p);
        }
        return dir;
    }

    /**
     * Creates a nested patternset.
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
     * add a name entry on the include list
     */
    public PatternSet.NameEntry createInclude() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createInclude();
    }

    /**
     * add a name entry on the include files list
     */
    public PatternSet.NameEntry createIncludesFile() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createIncludesFile();
    }

    /**
     * add a name entry on the exclude list
     */
    public PatternSet.NameEntry createExclude() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createExclude();
    }

    /**
     * add a name entry on the excludes files list
     */
    public PatternSet.NameEntry createExcludesFile() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createExcludesFile();
    }

    /**
     * Creates a single file fileset.
     */
    public void setFile(File file) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        setDir(file.getParentFile());

        PatternSet.NameEntry include = createInclude();
        include.setName(file.getName());
    }

    /**
     * Appends <code>includes</code> to the current list of include
     * patterns.
     *
     * <p>Patterns may be separated by a comma or a space.</p>
     *
     * @param includes the string containing the include patterns
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
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes(String excludes) {
        if (isReference()) {
            throw tooManyAttributes();
        }

        defaultPatterns.setExcludes(excludes);
    }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param incl The file to fetch the include patterns from.
     */
     public void setIncludesfile(File incl) throws BuildException {
         if (isReference()) {
             throw tooManyAttributes();
         }

         defaultPatterns.setIncludesfile(incl);
     }

    /**
     * Sets the name of the file containing the excludes patterns.
     *
     * @param excl The file to fetch the exclude patterns from.
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
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(boolean useDefaultExcludes) {
        if (isReference()) {
            throw tooManyAttributes();
        }

        this.useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * Sets case sensitivity of the file system
     *
     * @param isCaseSensitive "true"|"on"|"yes" if file system is case
     *                           sensitive, "false"|"off"|"no" when not.
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
     * @param followSymlinks whether or not symbolic links should be followed
     */
    public void setFollowSymlinks(boolean followSymlinks) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.followSymlinks = followSymlinks;
    }

    /**
     * find out if the fileset wants to follow symbolic links
     *
     * @return  flag indicating whether or not symbolic links should be followed
     *
     * @since ant 1.6
     */
    public boolean isFollowSymlinks() {
        if (isReference()) {
            return getRef(getProject()).isFollowSymlinks();
        } else {
            return followSymlinks;
        }
    }

    /**
     * sets the name used for this datatype instance.
     */
    protected String getDataTypeName() {
        // look up the types in project and see if they match this class
        Project project = getProject();
        if (project != null) {
            Hashtable typedefs = project.getDataTypeDefinitions();
            for (Enumeration e = typedefs.keys(); e.hasMoreElements();) {
                String typeName = (String) e.nextElement();
                Class typeClass = (Class) typedefs.get(typeName);
                if (typeClass == getClass()) {
                    return typeName;
                }
            }
        }

        String classname = getClass().getName();

        int dotIndex = classname.lastIndexOf(".");
        if (dotIndex == -1) {
            return classname;
        }
        return classname.substring(dotIndex + 1);
    }

    /**
     * Returns the directory scanner needed to access the files to process.
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
            String msg = getRefid().getRefId() + " doesn\'t denote a "
                + getDataTypeName();
            throw new BuildException(msg);
        } else {
            return (AbstractFileSet) o;
        }
    }

    // SelectorContainer methods

    /**
     * Indicates whether there are any selectors here.
     *
     * @return whether any selectors are in this container
     */
    public boolean hasSelectors() {
        if (isReference() && getProject() != null) {
            return getRef(getProject()).hasSelectors();
        }
        return !(selectors.isEmpty());
    }

    /**
     * Indicates whether there are any patterns here.
     *
     * @return whether any patterns are in this container
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
     * Gives the count of the number of selectors in this container
     *
     * @return the number of selectors in this container
     */
    public int selectorCount() {
        if (isReference() && getProject() != null) {
            return getRef(getProject()).selectorCount();
        }
        return selectors.size();
    }

    /**
     * Returns the set of selectors as an array.
     *
     * @return an array of selectors in this container
     */
    public FileSelector[] getSelectors(Project p) {
        if (isReference()) {
            return getRef(p).getSelectors(p);
        } else {
            FileSelector[] result = new FileSelector[selectors.size()];
            selectors.copyInto(result);
            return result;
        }
    }

    /**
     * Returns an enumerator for accessing the set of selectors.
     *
     * @return an enumerator that goes through each of the selectors
     */
    public Enumeration selectorElements() {
        if (isReference() && getProject() != null) {
            return getRef(getProject()).selectorElements();
        }
        return selectors.elements();
    }

    /**
     * Add a new selector into this container.
     *
     * @param selector the new selector to add
     */
    public void appendSelector(FileSelector selector) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        selectors.addElement(selector);
    }

    /* Methods below all add specific selectors */

    /**
     * add a "Select" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addSelector(SelectSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an "And" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addAnd(AndSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an "Or" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addOr(OrSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a "Not" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addNot(NotSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a "None" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addNone(NoneSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a majority selector entry on the selector list
     * @param selector the selector to add
     */
    public void addMajority(MajoritySelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector date entry on the selector list
     * @param selector the selector to add
     */
    public void addDate(DateSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector size entry on the selector list
     * @param selector the selector to add
     */
    public void addSize(SizeSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a DifferentSelector entry on the selector list
     * @param selector the selector to add
     */
    public void addDifferent(DifferentSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector filename entry on the selector list
     * @param selector the selector to add
     */
    public void addFilename(FilenameSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector type entry on the selector list
     * @param selector the selector to add
     */
    public void addType(TypeSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an extended selector entry on the selector list
     * @param selector the selector to add
     */
    public void addCustom(ExtendSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a contains selector entry on the selector list
     * @param selector the selector to add
     */
    public void addContains(ContainsSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a present selector entry on the selector list
     * @param selector the selector to add
     */
    public void addPresent(PresentSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a depth selector entry on the selector list
     * @param selector the selector to add
     */
    public void addDepth(DepthSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a depends selector entry on the selector list
     * @param selector the selector to add
     */
    public void addDepend(DependSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a regular expression selector entry on the selector list
     * @param selector the selector to add
     */
    public void addContainsRegexp(ContainsRegexpSelector selector) {
        appendSelector(selector);
    }

    /**
     * add the modified selector
     * @param selector the selector to add
     * @since ant 1.6
     */
    public void addModified(ModifiedSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an arbitary selector
     * @param selector the selector to add
     * @since Ant 1.6
     */
    public void add(FileSelector selector) {
        appendSelector(selector);
    }

    /**
     * Returns included files as a list of semicolon-separated filenames
     *
     * @return String object with included filenames
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
