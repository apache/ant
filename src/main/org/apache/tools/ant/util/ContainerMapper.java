/*
 * Copyright 2004 The Apache Software Foundation.
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

package org.apache.tools.ant.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * A filenamemapper that contains other filename mappers.
 * The mappers proceeded in a chain or separately.
 * @see FileNameMapper
 */

public class ContainerMapper implements FileNameMapper {

    private boolean chained = false;
    private List mappers = new ArrayList();

    /**
     * Add a file name mapper.
     *
     * @param fileNameMapper a file name mapper.
     */
    public void add(FileNameMapper fileNameMapper) {
        mappers.add(fileNameMapper);
    }

    /**
     * Add a Mapper
     * @param mapper the mapper to add
     */
    public void addConfiguredMapper(Mapper mapper) {
        mappers.add(mapper.getImplementation());
    }
                    
    /**
     * Set the chained attribute.
     *
     * @param chained if true the mappers are processed in
     *                   a chained fashion. The outputs of
     *                a mapper are the inputs for the next mapper.
     *                if false the mappers are processed indepentanly, the
     *                outputs are combined.
     */
    public void setChained(boolean chained) {
        this.chained = chained;
    }

    /**
     * This method is ignored, present to fullfill the FileNameMapper
     * interface.
     * @param ignore this parameter is ignored.
     */
    public void setFrom(String ignore) {
    }

    /**
     * This method is ignored, present to fullfill the FileNameMapper
     * interface.
     * @param ignore this parameter is ignored.
     */
    public void setTo(String ignore) {
    }

    /**
     * Map a filename using the list of mappers.
     *
     * @param sourceFileName The filename to map.
     * @return a <code>String[]</code> value or null if there
     *         are no mappings.
     */
    public String[] mapFileName(String sourceFileName) {
        List ret = new ArrayList();
        if (chained) {
            List inputs = new ArrayList();
            ret.add(sourceFileName);
            for (int i = 0; i < mappers.size(); ++i) {
                inputs = ret;
                ret = new ArrayList();
                FileNameMapper mapper = (FileNameMapper) mappers.get(i);
                for (Iterator it = inputs.iterator(); it.hasNext();) {
                    String[] mapped = mapper.mapFileName(
                        (String) it.next());
                    if (mapped != null) {
                        for (int m = 0; m < mapped.length; ++m) {
                            ret.add(mapped[m]);
                        }
                    }
                }
                if (ret.size() == 0) {
                    return null;
                }
            }
        } else {
            for (int i = 0; i < mappers.size(); ++i) {
                FileNameMapper mapper = (FileNameMapper) mappers.get(i);
                String[] mapped = mapper.mapFileName(sourceFileName);
                if (mapped != null) {
                    for (int m = 0; m < mapped.length; ++m) {
                        ret.add(mapped[m]);
                    }
                }
            }
            if (ret.size() == 0) {
                return null;
            }
        }
        return (String[]) ret.toArray(new String[ret.size()]);
    }
}

