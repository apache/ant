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
package org.apache.tools.ant.types.mappers;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * A mapper that strips of the a configurable number of leading
 * directories from a file name.
 *
 * <p>This mapper was inspired by a user-list thread that mentioned
 * wget's --cut-dirs option.</p>
 *
 * @see <a href="https://mail-archives.apache.org/mod_mbox/ant-user/201009.mbox/%3C51772743BEA5D44A9EA5BF52AADDD6FB010E96F6@hammai008.delphi.local%3E">
 * simplify copy with regexpmapper</a>
 */
public class CutDirsMapper implements FileNameMapper {
    private int dirs = 0;

    /**
     * The number of leading directories to cut.
     * @param dirs int
     */
    public void setDirs(final int dirs) {
        this.dirs =  dirs;
    }

    /**
     * Empty implementation.
     * @param ignore ignored.
     */
    @Override
    public void setFrom(final String ignore) {
    }

    /**
     * Empty implementation.
     * @param ignore ignored.
     */
    @Override
    public void setTo(final String ignore) {
    }

    /** {@inheritDoc}. */
    @Override
    public String[] mapFileName(final String sourceFileName) {
        if (dirs <= 0) {
            throw new BuildException("dirs must be set to a positive number");
        }
        final char fileSep = File.separatorChar;
        if (sourceFileName == null) {
            return null;
        }
        final String fileSepCorrected =
            sourceFileName.replace('/', fileSep).replace('\\', fileSep);
        int nthMatch = fileSepCorrected.indexOf(fileSep);
        for (int n = 1; nthMatch > -1 && n < dirs; n++) {
            nthMatch = fileSepCorrected.indexOf(fileSep, nthMatch + 1);
        }
        if (nthMatch == -1) {
            return null;
        }
        return new String[] {sourceFileName.substring(nthMatch + 1)};
    }
}
