/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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
package org.apache.tools.ant.types;

import java.io.File;
import java.util.Stack;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

/**
 * A ZipFileSet is a FileSet with extra attributes useful in the context of 
 * Zip/Jar tasks.
 *
 * A ZipFileSet extends FileSets with the ability to extract a subset of the
 * entries of a Zip file for inclusion in another Zip file.  It also includes
 * a prefix attribute which is prepended to each entry in the output Zip file.
 *
 * At present, ZipFileSets are not surfaced in the public API.  FileSets
 * nested in a Zip task are instantiated as ZipFileSets, and their attributes
 * are only recognized in the context of the the Zip task.
 * It is not possible to define a ZipFileSet outside of the Zip task and
 * refer to it via a refid.  However a standard FileSet may be included by
 * reference in the Zip task, and attributes in the refering ZipFileSet
 * can augment FileSet definition.
 *
 * @author Don Ferguson <a href="mailto:don@bea.com">don@bea.com</a>
 */
public class ZipFileSet extends FileSet {

    private File srcFile          = null;
    private String prefix         = "";
    private String fullpath       = "";
    private boolean hasDir        = false;

    /**
     * Set the directory for the fileset.  Prevents both "dir" and "src"
     * from being specified.
     */
    public void setDir(File dir) throws BuildException {
        if (srcFile != null) {
            throw new BuildException("Cannot set both dir and src attributes");
        } else {
            super.setDir(dir);
            hasDir = true;
        }
    }

    /**
     * Set the source Zip file for the zipfileset.  Prevents both
     * "dir" and "src" from being specified.
     *
     * @param srcFile The zip file from which to extract entries.
     */
    public void setSrc(File srcFile) {
        if (hasDir) {
            throw new BuildException("Cannot set both dir and src attributes");
        }
        this.srcFile = srcFile;
    }

    /**
     * Get the zip file from which entries will be extracted.
     * References are not followed, since it is not possible
     * to have a reference to a ZipFileSet, only to a FileSet.
     */
    public File getSrc() {
        return srcFile;
    }

    /**
     * Prepend this prefix to the path for each zip entry.
     * Does not perform reference test; the referenced file set
     * can be augmented with a prefix.
     *
     * @param prefix The prefix to prepend to entries in the zip file.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Return the prefix prepended to entries in the zip file.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the full pathname of the single entry in this fileset.
     *
     * @param prefix The prefix to prepend to entries in the zip file.
     */
    public void setFullpath(String fullpath) {
        this.fullpath = fullpath;
    }

    /**
     * Return the full pathname of the single entry in this fileset.
     */
    public String getFullpath() {
        return fullpath;
    }

    /**
     * Return the DirectoryScanner associated with this FileSet.
     * If the ZipFileSet defines a source Zip file, then a ZipScanner
     * is returned instead.
     */
    public DirectoryScanner getDirectoryScanner(Project p) {
        if (isReference()) {
            return getRef(p).getDirectoryScanner(p);
        }
        if (srcFile != null) {
            ZipScanner zs = new ZipScanner();
            zs.setSrc(srcFile);
            super.setDir(p.getBaseDir());
            setupDirectoryScanner(zs, p);
            zs.init();
            return zs;
        } else {
            return super.getDirectoryScanner(p);
        }
    }    
  
}
