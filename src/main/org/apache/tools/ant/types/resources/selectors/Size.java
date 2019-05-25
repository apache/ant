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
package org.apache.tools.ant.types.resources.selectors;

import org.apache.tools.ant.types.Comparison;
import org.apache.tools.ant.types.Resource;

/**
 * Size ResourceSelector.
 * @since Ant 1.7
 */
public class Size implements ResourceSelector {
    private long size = -1;
    private Comparison when = Comparison.EQUAL;

    /**
     * Set the size to compare against.
     * @param l the long resource size.
     */
    public void setSize(long l) {
        size = l;
    }

    /**
     * Get the size compared to by this Size ResourceSelector.
     * @return the long resource size.
     */
    public long getSize() {
        return size;
    }

    /**
     * Set the comparison mode.
     * @param c a Comparison object.
     */
    public void setWhen(Comparison c) {
        when = c;
    }

    /**
     * Get the comparison mode.
     * @return a Comparison object.
     */
    public Comparison getWhen() {
        return when;
    }

    /**
     * Return true if this Resource is selected.
     * @param r the Resource to check.
     * @return whether the Resource was selected.
     */
    public boolean isSelected(Resource r) {
        long diff = r.getSize() - size;
        return when.evaluate(diff == 0 ? 0 : (int) (diff / Math.abs(diff)));
    }

}
