/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights 
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.ant.core.types;

import java.util.*;
import java.io.*;
import java.net.URL;
import org.apache.ant.core.execution.*;

/**
 * Moved out of MatchingTask to make it a standalone object that could
 * be referenced (by scripts for example).
 *
 * @author Arnout J. Kuiper <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a> 
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class FileSet extends DataType {
    private FileSetScanner scanner = null;
    
    private PatternSet defaultPatterns = new PatternSet();
    private List patternSets = new ArrayList();

    /** 
     * The dir attribute is set when you are generating the list of files
     *  from a directory. 
     */
    private File dir = null;
    
    /** The zipfile attribute is used when the source of files is a zip file */
    private URL zipFile = null;
    
    /** 
     * The filelist attribute is a file which contains a list of file names. It must be used
     * with the base attribute which indicates where the files are stored.
     */
    private URL fileList = null;
    
    /**
     * When using the filelist this attribute indicates the base location of the files in
     * the list.
     */
    private URL fileListBase = null;
    
    private boolean useDefaultExcludes = true;

    public FileSet() {
        patternSets.add(defaultPatterns);
    }

    /**
     * Makes this instance in effect a reference to another FileSet
     * instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p> 
     */
    public void setRefid(String reference) throws ExecutionException {
        if (dir != null || defaultPatterns.hasPatterns()) {
            throw tooManyAttributes();
        }
        if (!(patternSets.size() == 1)) {
            throw noChildrenAllowed();
        }
        super.setRefid(reference);
    }

    public void setDir(File dir) throws ExecutionException {
        if (isReference()) {
            throw tooManyAttributes();
        }

        this.dir = dir;
    }

    public void setZipFile(URL zipFile) throws ExecutionException {
        if (isReference()) {
            throw tooManyAttributes();
        }

        this.zipFile = zipFile;
    }

    public void setFileList(URL fileList) throws ExecutionException {
        if (isReference()) {
            throw tooManyAttributes();
        }

        this.fileList = fileList;
    }

    public void setFileListBase(URL fileListBase) throws ExecutionException {
        if (isReference()) {
            throw tooManyAttributes();
        }

        this.fileListBase = fileListBase;
    }

    public PatternSet createPatternSet() throws ExecutionException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        PatternSet patternSet = new PatternSet();
        patternSets.add(patternSet);
        return patternSet;
    }

    /**
     * add a name entry on the include list
     */
    public PatternSet.NameEntry createInclude() throws ExecutionException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createInclude();
    }
    
    /**
     * add a name entry on the exclude list
     */
    public PatternSet.NameEntry createExclude() throws ExecutionException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createExclude();
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes(String includes) throws ExecutionException {
        if (isReference()) {
            throw tooManyAttributes();
        }

        defaultPatterns.setIncludes(includes);
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes(String excludes) throws ExecutionException {
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
     public void setIncludesFile(URL incl) throws ExecutionException {
         if (isReference()) {
             throw tooManyAttributes();
         }

         defaultPatterns.setIncludesFile(incl);
     }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param excl The file to fetch the exclude patterns from.  
     */
     public void setExcludesFile(URL excl) throws ExecutionException {
         if (isReference()) {
             throw tooManyAttributes();
         }

         defaultPatterns.setExcludesFile(excl);
     }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions 
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultExcludes(boolean useDefaultExcludes) throws ExecutionException {
        if (isReference()) {
            throw tooManyAttributes();
        }

        this.useDefaultExcludes = useDefaultExcludes;
    }

    protected FileSet getReferencedFileSet() throws ExecutionException {
        Object o = getReferencedObject();
        if (!(o instanceof FileSet)) {
            throw new ExecutionException(getReference() + " doesn\'t denote a fileset");;
        } else {
            return (FileSet) o;
        }
    }

    public void validate() throws ExecutionException {
        if (dir != null) {
            // firstly validate that the other attributes are not set
            if (zipFile != null || fileList != null || fileListBase != null) {
                throw new ExecutionException("The 'dir' attribute may not be combined with any " 
                                             + "of the 'zipfile', 'filelist' and 'base' attributes");
            }
        }
        else if (zipFile != null) {
            if (fileList != null || fileListBase != null) {
                throw new ExecutionException("The 'zipfile' attribute may not be combined with any " 
                                             + "of the 'dir', 'filelist' and 'base' attributes");
            }
        }
        else if (fileList != null) {
            if (fileListBase == null) {
                throw new ExecutionException("A 'base' attribute is required when using the 'filelist' "
                                             + "attribute");
            }
        }
        else {
            throw new ExecutionException("You must specify one of the 'dir', 'zipfile', or 'filelist' " 
                                         + "attributes");
        }
    }
    
    public FileSetScanner getScanner() throws ExecutionException {
        if (isReference()) {
            return getReferencedFileSet().getScanner();
        }
        
        if (scanner != null) {
            return scanner;
        }
        
        // need to create the fileset info. For that we are going to need
        // to determine which type of FileSetInfo implementation we should use.
        if (dir != null) {
            scanner = new DirectoryScanner(dir, patternSets, useDefaultExcludes);
        }
        else if (zipFile != null) {
        }
        else if (fileList != null) {
        }
        else {
        }
        
        return scanner;
    }
    
    public String toString() {
        try {
            return getScanner().toString();
        }
        catch (ExecutionException e) {
            return "FileSet";
        }
    }
}
