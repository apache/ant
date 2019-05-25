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

import java.io.Reader;
import java.io.StringReader;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.UnsupportedAttributeException;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;

/**
 * This is a FileNameMapper based on a FilterChain.
 */
public class FilterMapper extends FilterChain implements FileNameMapper {

    private static final int BUFFER_SIZE = 8192;

    /**
     * From attribute not supported.
     * @param from a string
     * @throws BuildException always
     */
    @Override
    public void setFrom(String from) {
        throw new UnsupportedAttributeException(
            "filtermapper doesn't support the \"from\" attribute.", "from");
    }

    /**
     * From attribute not supported.
     * @param to a string
     * @throws BuildException always
     */
    @Override
    public void setTo(String to) {
        throw new UnsupportedAttributeException(
            "filtermapper doesn't support the \"to\" attribute.", "to");
    }

    /**
     * Return the result of the filters on the sourcefilename.
     * @param sourceFileName the filename to map
     * @return  a one-element array of converted filenames, or null if
     *          the filterchain returns an empty string.
     */
    @Override
    public String[] mapFileName(String sourceFileName) {
        if (sourceFileName == null) {
            return null;
        }
        try {
            Reader stringReader = new StringReader(sourceFileName);
            ChainReaderHelper helper = new ChainReaderHelper();
            helper.setBufferSize(BUFFER_SIZE);
            helper.setPrimaryReader(stringReader);
            helper.setProject(getProject());
            Vector<FilterChain> filterChains = new Vector<>();
            filterChains.add(this);
            helper.setFilterChains(filterChains);
            String result = FileUtils.safeReadFully(helper.getAssembledReader());
            if (result.isEmpty()) {
                return null;
            }
            return new String[] {result};
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
    }
}
