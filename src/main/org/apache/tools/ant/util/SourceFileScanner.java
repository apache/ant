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

package org.apache.tools.ant.util;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.taskdefs.condition.Os;

import java.io.File;
import java.util.Vector;

/**
 * Utility class that collects the functionality of the various
 * scanDir methods that have been scattered in several tasks before.
 *
 * <p>The only method returns an array of source files. The array is a
 * subset of the files given as a parameter and holds only those that
 * are newer than their corresponding target files.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</a>
 */
public class SourceFileScanner implements ResourceFactory {

    protected Task task;

    private FileUtils fileUtils;
    private File destDir;     // base directory of the fileset

    /**
     * @param task The task we should log messages through
     */
    public SourceFileScanner(Task task) {
        this.task = task;
        fileUtils = FileUtils.newFileUtils();
    }

    /**
     * Restrict the given set of files to those that are newer than
     * their corresponding target files.
     *
     * @param files   the original set of files
     * @param srcDir  all files are relative to this directory
     * @param destDir target files live here. if null file names
     *                returned by the mapper are assumed to be absolute.
     * @param mapper  knows how to construct a target file names from
     *                source file names.
     */
    public String[] restrict(String[] files, File srcDir, File destDir,
                             FileNameMapper mapper) {
        // record destdir for later use in getResource
        this.destDir = destDir;
        Vector v = new Vector();
        for (int i = 0; i < files.length; i++) {
            File src = fileUtils.resolveFile(srcDir, files[i]);
            v.addElement(new Resource(files[i], src.exists(),
                                      src.lastModified(), src.isDirectory()));
        }
        Resource[] sourceresources= new Resource[v.size()];
        v.copyInto(sourceresources);

        // build the list of sources which are out of date with
        // respect to the target
        Resource[] outofdate = 
            ResourceUtils.selectOutOfDateSources(task, sourceresources,
                                                 mapper, this);
        String[] result = new String[outofdate.length];
        for (int counter=0; counter < outofdate.length; counter++) {
            result[counter] = outofdate[counter].getName();
        }
        return result;
    }

    /**
     * Convinience layer on top of restrict that returns the source
     * files as File objects (containing absolute paths if srcDir is
     * absolute).
     */
    public File[] restrictAsFiles(String[] files, File srcDir, File destDir,
                                  FileNameMapper mapper) {
        String[] res = restrict(files, srcDir, destDir, mapper);
        File[] result = new File[res.length];
        for (int i = 0; i < res.length; i++) {
            result[i] = new File(srcDir, res[i]);
        }
        return result;
    }

    /**
     * returns resource information for a file at destination
     * @param name relative path of file at destination
     * @return data concerning a file whose relative path to destDir is name
     *
     * @since Ant 1.5.2
     */
    public Resource getResource(String name) {
        File src = fileUtils.resolveFile(destDir, name);
        return new Resource(name, src.exists(), src.lastModified(),
                            src.isDirectory());
    }
}
