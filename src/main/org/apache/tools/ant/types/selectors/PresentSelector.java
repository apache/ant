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

package org.apache.tools.ant.types.selectors;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.IdentityMapper;

/**
 * Selector that filters files based on whether they appear in another
 * directory tree. It can contain a mapper element, so isn't available
 * as an ExtendSelector (since those parameters can't hold other
 * elements).
 *
 * @since 1.5
 */
public class PresentSelector extends BaseSelector {
    private File targetdir = null;
    private Mapper mapperElement = null;
    private FileNameMapper map = null;
    private boolean destmustexist = true;

    /**
     * @return a string describing this object
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder("{presentselector targetdir: ");
        if (targetdir == null) {
            buf.append("NOT YET SET");
        } else {
            buf.append(targetdir.getName());
        }
        buf.append(" present: ");
        if (destmustexist) {
            buf.append("both");
        } else {
            buf.append("srconly");
        }
        if (map != null) {
            buf.append(map.toString());
        } else if (mapperElement != null) {
            buf.append(mapperElement.toString());
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * The name of the file or directory which is checked for matching
     * files.
     *
     * @param targetdir the directory to scan looking for matching files.
     */
    public void setTargetdir(final File targetdir) {
        this.targetdir = targetdir;
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     * @return a mapper to be configured
     * @throws BuildException if more than one mapper defined
     */
    public Mapper createMapper() throws BuildException {
        if (map != null || mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper");
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * Add a configured FileNameMapper instance.
     * @param fileNameMapper the FileNameMapper to add
     * @throws BuildException if more than one mapper defined
     * @since Ant 1.8.0
     */
    public void addConfigured(final FileNameMapper fileNameMapper) {
        if (map != null || mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper");
        }
        this.map = fileNameMapper;
    }

    /**
     * This sets whether to select a file if its dest file is present.
     * It could be a <code>negate</code> boolean, but by doing things
     * this way, we get some documentation on how the system works.
     * A user looking at the documentation should clearly understand
     * that the ONLY files whose presence is being tested are those
     * that already exist in the source directory, hence the lack of
     * a <code>destonly</code> option.
     *
     * @param fp An attribute set to either <code>srconly</code> or
     *           <code>both</code>.
     */
    public void setPresent(final FilePresence fp) {
        if (fp.getIndex() == 0) {
            destmustexist = false;
        }
    }

    /**
     * Checks to make sure all settings are kosher. In this case, it
     * means that the targetdir attribute has been set and we have a mapper.
     */
    @Override
    public void verifySettings() {
        if (targetdir == null) {
            setError("The targetdir attribute is required.");
        }
        if (map == null) {
            if (mapperElement == null) {
                map = new IdentityMapper();
            } else {
                map = mapperElement.getImplementation();
                if (map == null) {
                    setError("Could not set <mapper> element.");
                }
            }
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
    @Override
    public boolean isSelected(final File basedir, final String filename, final File file) {

        // throw BuildException on error
        validate();

        // Determine file whose existence is to be checked
        final String[] destfiles = map.mapFileName(filename);
        // If filename does not match the To attribute of the mapper
        // then filter it out of the files we are considering
        if (destfiles == null) {
            return false;
        }
        // Sanity check
        if (destfiles.length != 1 || destfiles[0] == null) {
            throw new BuildException("Invalid destination file results for "
                    + targetdir + " with filename " + filename);
        }
        final String destname = destfiles[0];
        final File destfile = FileUtils.getFileUtils().resolveFile(targetdir, destname);
        return destfile.exists() == destmustexist;
    }

    /**
     * Enumerated attribute with the values for indicating where a file's
     * presence is allowed and required.
     */
    public static class FilePresence extends EnumeratedAttribute {
        /**
         * @return the values as an array of strings
         */
        @Override
        public String[] getValues() {
            return new String[] {"srconly", "both"};
        }
    }

}
