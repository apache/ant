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
import java.util.Collections;

/**
 * This selector is here just to shake up your thinking a bit. Don't get
 * too caught up in boolean, there are other ways you can evaluate a
 * collection of selectors. This one takes a vote of the selectors it
 * contains, and majority wins. You could also have an "all-but-one"
 * selector, a "weighted-average" selector, and so on. These are left
 * as exercises for the reader (as are the usecases where this would
 * be necessary).
 *
 * @since 1.5
 */
public class MajoritySelector extends BaseSelectorContainer {

    private boolean allowtie = true;

    /**
     * @return a string describing this object
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (hasSelectors()) {
            buf.append("{majorityselect: ");
            buf.append(super.toString());
            buf.append("}");
        }
        return buf.toString();
    }

    /**
     * A attribute to specify what will happen if number
     * of yes votes is the same as the number of no votes
     * defaults to true
     *
     * @param tiebreaker the value to give if there is a tie
     */
    public void setAllowtie(boolean tiebreaker) {
        allowtie = tiebreaker;
    }

    /**
     * Returns true (the file is selected) if most of the other selectors
     * agree. In case of a tie, go by the allowtie setting. That defaults
     * to true, meaning in case of a tie, the file is selected.
     *
     * @param basedir the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file is a java.io.File object for the filename that the selector
     * can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {
        validate();
        int yesvotes = 0;
        int novotes = 0;

        for (FileSelector fs : Collections.list(selectorElements())) {
            if (fs.isSelected(basedir, filename, file)) {
                yesvotes++;
            } else {
                novotes++;
            }
        }
        if (yesvotes > novotes) {
            return true;
        }
        if (novotes > yesvotes) {
            return false;
        }
        // At this point, we know we have a tie.
        return allowtie;
    }
}

