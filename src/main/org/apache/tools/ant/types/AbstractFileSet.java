/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.types.selectors.AndSelector;
import org.apache.tools.ant.types.selectors.ContainsSelector;
import org.apache.tools.ant.types.selectors.DateSelector;
import org.apache.tools.ant.types.selectors.DependSelector;
import org.apache.tools.ant.types.selectors.DepthSelector;
import org.apache.tools.ant.types.selectors.ExtendSelector;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.MajoritySelector;
import org.apache.tools.ant.types.selectors.NoneSelector;
import org.apache.tools.ant.types.selectors.NotSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
import org.apache.tools.ant.types.selectors.PresentSelector;
import org.apache.tools.ant.types.selectors.SelectSelector;
import org.apache.tools.ant.types.selectors.SelectorContainer;
import org.apache.tools.ant.types.selectors.SelectorScanner;
import org.apache.tools.ant.types.selectors.SizeSelector;

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
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author Magesh Umasankar
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
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
     * add a name entry on the include files list
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
        FileUtils fileUtils = FileUtils.newFileUtils();
        setDir(fileUtils.getParentFile(file));

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
     * Sets the name of the file containing the includes patterns.
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

        p.log(getDataTypeName() + ": Setup scanner in dir " + dir +
            " with " + defaultPatterns, Project.MSG_DEBUG);

        ds.setIncludes(defaultPatterns.getIncludePatterns(p));
        ds.setExcludes(defaultPatterns.getExcludePatterns(p));
        if (ds instanceof SelectorScanner) {
            SelectorScanner ss = (SelectorScanner)ds;
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
        if (!checked) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }

        Object o = ref.getReferencedObject(p);
        if (!getClass().isAssignableFrom(o.getClass())) {
            String msg = ref.getRefId() + " doesn\'t denote a "
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

        Enumeration enum = additionalPatterns.elements();
        while (enum.hasMoreElements()) {
            PatternSet ps = (PatternSet) enum.nextElement();
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
     */
    public void addSelector(SelectSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an "And" selector entry on the selector list
     */
    public void addAnd(AndSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an "Or" selector entry on the selector list
     */
    public void addOr(OrSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a "Not" selector entry on the selector list
     */
    public void addNot(NotSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a "None" selector entry on the selector list
     */
    public void addNone(NoneSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a majority selector entry on the selector list
     */
    public void addMajority(MajoritySelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector date entry on the selector list
     */
    public void addDate(DateSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector size entry on the selector list
     */
    public void addSize(SizeSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector filename entry on the selector list
     */
    public void addFilename(FilenameSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an extended selector entry on the selector list
     */
    public void addCustom(ExtendSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a contains selector entry on the selector list
     */
    public void addContains(ContainsSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a present selector entry on the selector list
     */
    public void addPresent(PresentSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a depth selector entry on the selector list
     */
    public void addDepth(DepthSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a depends selector entry on the selector list
     */
    public void addDepend(DependSelector selector) {
        appendSelector(selector);
    }

}
