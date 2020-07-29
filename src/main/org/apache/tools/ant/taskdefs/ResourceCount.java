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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.Comparison;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Count resources from a ResourceCollection, storing to a property or
 * writing to the log.  Can also be used as a Condition.
 * @since Ant 1.7
 */
public class ResourceCount extends Task implements Condition {

    private static final String ONE_NESTED_MESSAGE
        = "ResourceCount can count resources from exactly one nested ResourceCollection.";

    private static final String COUNT_REQUIRED
        = "Use of the ResourceCount condition requires that the count attribute be set.";

    private ResourceCollection rc;
    private Comparison when = Comparison.EQUAL;
    private Integer count;
    private String property;

    /**
     * Add the ResourceCollection to count.
     * @param r the ResourceCollection to count.
     * @throws BuildException if already set.
     */
    public void add(ResourceCollection r) {
        if (rc != null) {
            throw new BuildException(ONE_NESTED_MESSAGE);
        }
        rc = r;
    }

    /**
     * Set the ResourceCollection reference.
     * @param r the Reference.
     */
    public void setRefid(Reference r) {
        Object o = r.getReferencedObject();
        if (!(o instanceof ResourceCollection)) {
            throw new BuildException("%s doesn't denote a ResourceCollection",
                r.getRefId());
        }
        add((ResourceCollection) o);
    }

    /**
     * Execute as a Task.
     */
    @Override
    public void execute() {
        if (rc == null) {
            throw new BuildException(ONE_NESTED_MESSAGE);
        }
        if (property == null) {
            log("resource count = " + rc.size());
        } else {
            getProject().setNewProperty(property, Integer.toString(rc.size()));
        }
    }

    /**
     * Fulfill the condition contract.
     * @return true if the specified ResourceCollection satisfies the set criteria.
     * @throws BuildException if an error occurs.
     */
    @Override
    public boolean eval() {
        if (rc == null) {
            throw new BuildException(ONE_NESTED_MESSAGE);
        }
        if (count == null) {
            throw new BuildException(COUNT_REQUIRED);
        }
        return when.evaluate(Integer.valueOf(rc.size()).compareTo(count));
    }

    /**
     * Set the target count number for use as a Condition.
     * @param c number of Resources as int.
     */
    public void setCount(int c) {
        count = c;
    }

    /**
     * Set the comparison for use as a Condition.
     * @param c Comparison (an EnumeratedAttribute) When.
     * @see org.apache.tools.ant.types.Comparison
     */
    public void setWhen(Comparison c) {
        when = c;
    }

    /**
     * Set the name of the property to set in task mode.
     * @param p the property name to set.
     */
    public void setProperty(String p) {
        property = p;
    }

}
