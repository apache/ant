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

package org.apache.tools.ant.types.selectors;

import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;

/**
 * A mapping selector is an abstract class adding mapping support to the base
 * selector
 * @author not specified
 */
public abstract class MappingSelector extends BaseSelector {
    protected File targetdir = null;
    protected Mapper mapperElement = null;
    protected FileNameMapper map = null;
    protected int granularity = 0;

    /**
     * Creates a new <code>MappingSelector</code> instance.
     *
     */
    public MappingSelector() {
        granularity=(int) FileUtils.newFileUtils().getFileTimestampGranularity();
    }


    /**
     * The name of the file or directory which is checked for out-of-date
     * files.
     *
     * @param targetdir the directory to scan looking for files.
     */
    public void setTargetdir(File targetdir) {
        this.targetdir = targetdir;
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     * @return a mapper to be configured
     * @throws BuildException if more that one mapper defined
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper");
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * Checks to make sure all settings are kosher. In this case, it
     * means that the dest attribute has been set and we have a mapper.
     */
    public void verifySettings() {
        if (targetdir == null) {
            setError("The targetdir attribute is required.");
        }
        if (mapperElement == null) {
            map = new IdentityMapper();
        } else {
            map = mapperElement.getImplementation();
        }
        if (map == null) {
            setError("Could not set <mapper> element.");
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset.
     *
     * @param basedir the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file is a java.io.File object the selector can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {

        // throw BuildException on error
        validate();

        // Determine file whose out-of-dateness is to be checked
        String[] destfiles = map.mapFileName(filename);
        // If filename does not match the To attribute of the mapper
        // then filter it out of the files we are considering
        if (destfiles == null) {
            return false;
        }
        // Sanity check
        if (destfiles.length != 1 || destfiles[0] == null) {
            throw new BuildException("Invalid destination file results for "
                    + targetdir.getName() + " with filename " + filename);
        }
        String destname = destfiles[0];
        File destfile = new File(targetdir, destname);

        boolean selected = selectionTest(file, destfile);
        return selected;
    }

    /**
     * this test is our selection test that compared the file with the destfile
     * @param srcfile file to test; may be null
     * @param destfile destination file
     * @return true if source file compares with destination file
     */
    protected abstract boolean selectionTest(File srcfile, File destfile);

    /**
     * Sets the number of milliseconds leeway we will give before we consider
     * a file out of date. Defaults to 2000 on MS-DOS derivatives as the FAT
     * file system.
     * @param granularity the leeway in milliseconds
     */
    public void setGranularity(int granularity) {
        this.granularity = granularity;
    }

}
