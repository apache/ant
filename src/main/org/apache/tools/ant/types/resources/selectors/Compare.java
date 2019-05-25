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

import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Comparison;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Quantifier;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.resources.comparators.DelegatedResourceComparator;
import org.apache.tools.ant.types.resources.comparators.ResourceComparator;

/**
 * ResourceSelector that compares against "control" Resource(s)
 * using ResourceComparators.
 * @since Ant 1.7
 */
public class Compare extends DataType implements ResourceSelector {

    private DelegatedResourceComparator comp = new DelegatedResourceComparator();
    private Quantifier against = Quantifier.ALL;

    private Comparison when = Comparison.EQUAL;

    private Union control;

    /**
     * Add a ResourceComparator to this Compare selector.
     * If multiple ResourceComparators are added, they will be processed in LIFO order.
     * @param c the ResourceComparator to add.
     */
    public synchronized void add(ResourceComparator c) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        comp.add(c);
        setChecked(false);
    }

    /**
     * Set the quantifier to be used. Default "all".
     * @param against the Quantifier EnumeratedAttribute to use.
     */
    public synchronized void setAgainst(Quantifier against) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.against = against;
    }

    /**
     * Set the comparison to be used. Default "equal".
     * @param when the Comparison EnumeratedAttribute to use.
     */
    public synchronized void setWhen(Comparison when) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.when = when;
    }

    /**
     * Create the nested control element. These are the
     * resources to compare against.
     * @return ResourceCollection.
     */
    public synchronized ResourceCollection createControl() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (control != null) {
            throw oneControl();
        }
        control = new Union();
        setChecked(false);
        return control;
    }

    //implement ResourceSelector; inherit doc
    /** {@inheritDoc} */
    public synchronized boolean isSelected(Resource r) {
        if (isReference()) {
            return getRef().isSelected(r);
        }
        if (control == null) {
            throw oneControl();
        }
        dieOnCircularReference();
        int t = 0, f = 0;
        for (Resource res : control) {
            if (when.evaluate(comp.compare(r, res))) {
                t++;
            } else {
                f++;
            }
        }
        return against.evaluate(t, f);
    }

    /**
     * Overrides the version from DataType
     * to recurse on nested ResourceComparators.
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (control != null) {
                DataType.pushAndInvokeCircularReferenceCheck(control, stk, p);
            }
            DataType.pushAndInvokeCircularReferenceCheck(comp, stk, p);
            setChecked(true);
        }
    }

    private ResourceSelector getRef() {
        return getCheckedRef(ResourceSelector.class);
    }

    private BuildException oneControl() {
        return new BuildException("%s the <control> element should be specified exactly once.",
                super.toString());
    }
}
