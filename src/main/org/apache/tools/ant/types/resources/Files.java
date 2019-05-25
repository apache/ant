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
package org.apache.tools.ant.types.resources;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.selectors.AbstractSelectorContainer;
import org.apache.tools.ant.types.selectors.FileSelector;

/**
 * ResourceCollection implementation; like AbstractFileSet with absolute paths.
 * @since Ant 1.7
 */
public class Files extends AbstractSelectorContainer
    implements ResourceCollection {

    private PatternSet defaultPatterns = new PatternSet();
    private Vector<PatternSet> additionalPatterns = new Vector<>();

    private boolean useDefaultExcludes = true;
    private boolean caseSensitive = true;
    private boolean followSymlinks = true;

    /* cached DirectoryScanner instance */
    private DirectoryScanner ds = null;

    /**
     * Construct a new <code>Files</code> collection.
     */
    public Files() {
        super();
    }

    /**
     * Construct a new <code>Files</code> collection, shallowly cloned
     * from the specified <code>Files</code>.
     * @param f the <code>Files</code> to use as a template.
     */
    protected Files(Files f) {
        this.defaultPatterns = f.defaultPatterns;
        this.additionalPatterns = f.additionalPatterns;
        this.useDefaultExcludes = f.useDefaultExcludes;
        this.caseSensitive = f.caseSensitive;
        this.followSymlinks = f.followSymlinks;
        this.ds = f.ds;
        setProject(f.getProject());
    }

    /**
     * Make this instance in effect a reference to another instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     * @param r the <code>Reference</code> to use.
     * @throws BuildException if there is a problem.
     */
    @Override
    public void setRefid(Reference r) throws BuildException {
        if (hasPatterns(defaultPatterns)) {
            throw tooManyAttributes();
        }
        if (!additionalPatterns.isEmpty()) {
            throw noChildrenAllowed();
        }
        if (hasSelectors()) {
            throw noChildrenAllowed();
        }
        super.setRefid(r);
    }

    /**
     * Create a nested patternset.
     * @return <code>PatternSet</code>.
     */
    public synchronized PatternSet createPatternSet() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        PatternSet patterns = new PatternSet();
        additionalPatterns.addElement(patterns);
        ds = null;
        setChecked(false);
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
        ds = null;
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
        ds = null;
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
        ds = null;
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
        ds = null;
        return defaultPatterns.createExcludesFile();
    }

    /**
     * Append <code>includes</code> to the current list of include
     * patterns.
     *
     * <p>Patterns may be separated by a comma or a space.</p>
     *
     * @param includes the <code>String</code> containing the include patterns.
     */
    public synchronized void setIncludes(String includes) {
        checkAttributesAllowed();
        defaultPatterns.setIncludes(includes);
        ds = null;
    }

    /**
     * Append <code>includes</code> to the current list of include
     * patterns.
     *
     * @param includes array containing the include patterns.
     */
    public synchronized void appendIncludes(String[] includes) {
        checkAttributesAllowed();
        if (includes != null) {
            for (String include : includes) {
                defaultPatterns.createInclude().setName(include);
            }
            ds = null;
        }
    }

    /**
     * Append <code>excludes</code> to the current list of exclude
     * patterns.
     *
     * <p>Patterns may be separated by a comma or a space.</p>
     *
     * @param excludes the <code>String</code> containing the exclude patterns.
     */
    public synchronized void setExcludes(String excludes) {
        checkAttributesAllowed();
        defaultPatterns.setExcludes(excludes);
        ds = null;
    }

    /**
     * Append <code>excludes</code> to the current list of include
     * patterns.
     *
     * @param excludes array containing the exclude patterns.
     */
    public synchronized void appendExcludes(String[] excludes) {
        checkAttributesAllowed();
        if (excludes != null) {
            for (String exclude : excludes) {
                defaultPatterns.createExclude().setName(exclude);
            }
            ds = null;
        }
    }

    /**
     * Set the <code>File</code> containing the includes patterns.
     *
     * @param incl <code>File</code> instance.
     * @throws BuildException if there is a problem.
     */
    public synchronized void setIncludesfile(File incl) throws BuildException {
        checkAttributesAllowed();
        defaultPatterns.setIncludesfile(incl);
        ds = null;
    }

    /**
     * Set the <code>File</code> containing the excludes patterns.
     *
     * @param excl <code>File</code> instance.
     * @throws BuildException if there is a problem.
     */
    public synchronized void setExcludesfile(File excl) throws BuildException {
        checkAttributesAllowed();
        defaultPatterns.setExcludesfile(excl);
        ds = null;
    }

    /**
     * Set whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes <code>boolean</code>.
     */
    public synchronized void setDefaultexcludes(boolean useDefaultExcludes) {
        checkAttributesAllowed();
        this.useDefaultExcludes = useDefaultExcludes;
        ds = null;
    }

    /**
     * Get whether default exclusions should be used or not.
     * @return the defaultexclusions value.
     */
    public synchronized boolean getDefaultexcludes() {
        return isReference()
            ? getRef().getDefaultexcludes() : useDefaultExcludes;
    }

    /**
     * Set case-sensitivity of the Files collection.
     *
     * @param caseSensitive <code>boolean</code>.
     */
    public synchronized void setCaseSensitive(boolean caseSensitive) {
        checkAttributesAllowed();
        this.caseSensitive = caseSensitive;
        ds = null;
    }

    /**
     * Find out if this Files collection is case-sensitive.
     *
     * @return <code>boolean</code> indicating whether the Files
     * collection is case-sensitive.
     */
    public synchronized boolean isCaseSensitive() {
        return isReference()
            ? getRef().isCaseSensitive() : caseSensitive;
    }

    /**
     * Set whether or not symbolic links should be followed.
     *
     * @param followSymlinks whether or not symbolic links should be followed.
     */
    public synchronized void setFollowSymlinks(boolean followSymlinks) {
        checkAttributesAllowed();
        this.followSymlinks = followSymlinks;
        ds = null;
    }

    /**
     * Find out whether symbolic links should be followed.
     *
     * @return <code>boolean</code> indicating whether symbolic links
     *         should be followed.
     */
    public synchronized boolean isFollowSymlinks() {
        return isReference()
            ? getRef().isFollowSymlinks() : followSymlinks;
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return an Iterator of Resources.
     */
    @Override
    public synchronized Iterator<Resource> iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        ensureDirectoryScannerSetup();
        ds.scan();
        int fct = ds.getIncludedFilesCount();
        int dct = ds.getIncludedDirsCount();
        if (fct + dct == 0) {
            return Collections.emptyIterator();
        }
        FileResourceIterator result = new FileResourceIterator(getProject());
        if (fct > 0) {
            result.addFiles(ds.getIncludedFiles());
        }
        if (dct > 0) {
            result.addFiles(ds.getIncludedDirectories());
        }
        return result;
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     */
    @Override
    public synchronized int size() {
        if (isReference()) {
            return getRef().size();
        }
        ensureDirectoryScannerSetup();
        ds.scan();
        return ds.getIncludedFilesCount() + ds.getIncludedDirsCount();
    }

    /**
     * Find out whether this Files collection has patterns.
     *
     * @return whether any patterns are in this container.
     */
    public synchronized boolean hasPatterns() {
        if (isReference()) {
            return getRef().hasPatterns();
        }
        dieOnCircularReference();
        return hasPatterns(defaultPatterns)
            || additionalPatterns.stream().anyMatch(this::hasPatterns);
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
        super.appendSelector(selector);
        ds = null;
    }

    /**
     * Format this Files collection as a String.
     * @return a descriptive <code>String</code>.
     */
    @Override
    public String toString() {
        if (isReference()) {
            return getRef().toString();
        }
        return isEmpty() ? "" : stream().map(Object::toString)
            .collect(Collectors.joining(File.pathSeparator));
    }

    /**
     * Create a deep clone of this instance, except for the nested selectors
     * (the list of selectors is a shallow clone of this instance's list).
     * @return a cloned Object.
     */
    @Override
    public synchronized Object clone() {
        if (isReference()) {
            return getRef().clone();
        }
        Files f = (Files) super.clone();
        f.defaultPatterns = (PatternSet) defaultPatterns.clone();
        f.additionalPatterns = new Vector<>(additionalPatterns.size());
        for (PatternSet ps : additionalPatterns) {
            f.additionalPatterns.add((PatternSet) ps.clone());
        }
        return f;
    }

    /**
     * Get the merged include patterns for this Files collection.
     * @param p Project instance.
     * @return the include patterns of the default pattern set and all
     * nested patternsets.
     */
    public String[] mergeIncludes(Project p) {
        return mergePatterns(p).getIncludePatterns(p);
    }

    /**
     * Get the merged exclude patterns for this Files collection.
     * @param p Project instance.
     * @return the exclude patterns of the default pattern set and all
     * nested patternsets.
     */
    public String[] mergeExcludes(Project p) {
        return mergePatterns(p).getExcludePatterns(p);
    }

    /**
     * Get the merged patterns for this Files collection.
     * @param p Project instance.
     * @return the default patternset merged with the additional sets
     * in a new PatternSet instance.
     */
    public synchronized PatternSet mergePatterns(Project p) {
        if (isReference()) {
            return getRef().mergePatterns(p);
        }
        dieOnCircularReference();
        PatternSet ps = new PatternSet();
        ps.append(defaultPatterns, p);
        additionalPatterns.forEach(pat -> ps.append(pat, p));
        return ps;
    }

    /**
     * Always returns true.
     * @return true indicating that all elements of a Files collection
     *              will be FileResources.
     */
    @Override
    public boolean isFilesystemOnly() {
        return true;
    }

    /**
     * Perform the check for circular references and return the
     * referenced Files collection.
     * @return <code>FileCollection</code>.
     */
    protected Files getRef() {
        return getCheckedRef(Files.class);
    }

    private synchronized void ensureDirectoryScannerSetup() {
        dieOnCircularReference();
        if (ds == null) {
            ds = new DirectoryScanner();
            PatternSet ps = mergePatterns(getProject());
            ds.setIncludes(ps.getIncludePatterns(getProject()));
            ds.setExcludes(ps.getExcludePatterns(getProject()));
            ds.setSelectors(getSelectors(getProject()));
            if (useDefaultExcludes) {
                ds.addDefaultExcludes();
            }
            ds.setCaseSensitive(caseSensitive);
            ds.setFollowSymlinks(followSymlinks);
        }
    }

    private boolean hasPatterns(PatternSet ps) {
        String[] includePatterns = ps.getIncludePatterns(getProject());
        String[] excludePatterns = ps.getExcludePatterns(getProject());
        return (includePatterns != null && includePatterns.length > 0)
            || (excludePatterns != null && excludePatterns.length > 0);
    }

}
