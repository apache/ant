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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector;

/**
 * This is the a base class a container of selectors - it does
 * not need do be a selector itself.
 *
 * @since 1.7
 */
public abstract class AbstractSelectorContainer extends DataType
    implements Cloneable, SelectorContainer {

    private List<FileSelector> selectorsList =
        Collections.synchronizedList(new ArrayList<>());

    /**
     * Indicates whether there are any selectors here.
     * @return true if there are selectors
     */
    @Override
    public boolean hasSelectors() {
        if (isReference()) {
            return getRef().hasSelectors();
        }
        dieOnCircularReference();
        return !selectorsList.isEmpty();
    }

    /**
     * Gives the count of the number of selectors in this container
     * @return the number of selectors
     */
    public int selectorCount() {
        if (isReference()) {
            return getRef().selectorCount();
        }
        dieOnCircularReference();
        return selectorsList.size();
    }

    /**
     * Returns the set of selectors as an array.
     * @param p the current project
     * @return an array of selectors
     */
    public FileSelector[] getSelectors(Project p) {
        if (isReference()) {
            return getRef(p).getSelectors(p);
        }
        dieOnCircularReference(p);
        return selectorsList.toArray(new FileSelector[0]);
    }

    /**
     * Returns an enumerator for accessing the set of selectors.
     * @return an enumerator for the selectors
     */
    public Enumeration<FileSelector> selectorElements() {
        if (isReference()) {
            return getRef().selectorElements();
        }
        dieOnCircularReference();
        return Collections.enumeration(selectorsList);
    }

    /**
     * Convert the Selectors within this container to a string. This will
     * just be a helper class for the subclasses that put their own name
     * around the contents listed here.
     *
     * @return comma separated list of Selectors contained in this one
     */
    public String toString() {
        return selectorsList.stream().map(Object::toString)
            .collect(Collectors.joining(", "));
    }

    /**
     * Add a new selector into this container.
     *
     * @param selector the new selector to add
     */
    public void appendSelector(FileSelector selector) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        selectorsList.add(selector);
        setChecked(false);
    }

    /**
     * <p>
     * This validates each contained selector
     * provided that the selector implements the validate interface.
     * </p>
     * <p>Ordinarily, this will validate all the elements of a selector
     * container even if the isSelected() method of some elements is
     * never called. This has two effects:</p>
     * <ul>
     * <li>Validation will often occur twice.
     * <li>Since it is not required that selectors derive from
     * BaseSelector, there could be selectors in the container whose
     * error conditions are not detected if their isSelected() call
     * is never made.
     * </ul>
     */
    public void validate() {
        if (isReference()) {
            getRef().validate();
        }
        dieOnCircularReference();
        selectorsList.stream().filter(BaseSelector.class::isInstance)
            .map(BaseSelector.class::cast).forEach(BaseSelector::validate);
    }


    /* Methods below all add specific selectors */

    /**
     * add a "Select" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addSelector(SelectSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an "And" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addAnd(AndSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an "Or" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addOr(OrSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a "Not" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addNot(NotSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a "None" selector entry on the selector list
     * @param selector the selector to add
     */
    public void addNone(NoneSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a majority selector entry on the selector list
     * @param selector the selector to add
     */
    public void addMajority(MajoritySelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector date entry on the selector list
     * @param selector the selector to add
     */
    public void addDate(DateSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector size entry on the selector list
     * @param selector the selector to add
     */
    public void addSize(SizeSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector filename entry on the selector list
     * @param selector the selector to add
     */
    public void addFilename(FilenameSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an extended selector entry on the selector list
     * @param selector the selector to add
     */
    public void addCustom(ExtendSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a contains selector entry on the selector list
     * @param selector the selector to add
     */
    public void addContains(ContainsSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a present selector entry on the selector list
     * @param selector the selector to add
     */
    public void addPresent(PresentSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a depth selector entry on the selector list
     * @param selector the selector to add
     */
    public void addDepth(DepthSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a depends selector entry on the selector list
     * @param selector the selector to add
     */
    public void addDepend(DependSelector selector) {
        appendSelector(selector);
    }

    /**
     * adds a different selector to the selector list
     * @param selector the selector to add
     */
    public void addDifferent(DifferentSelector selector) {
        appendSelector(selector);
    }

    /**
     * adds a type selector to the selector list
     * @param selector the selector to add
     */
    public void addType(TypeSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a regular expression selector entry on the selector list
     * @param selector the selector to add
     */
    public void addContainsRegexp(ContainsRegexpSelector selector) {
        appendSelector(selector);
    }

    /**
     * add the modified selector
     * @param selector the selector to add
     * @since ant 1.6
     */
    public void addModified(ModifiedSelector selector) {
        appendSelector(selector);
    }

    public void addReadable(ReadableSelector r) {
        appendSelector(r);
    }

    public void addWritable(WritableSelector w) {
        appendSelector(w);
    }

    /**
     * @param e ExecutableSelector
     * @since 1.10.0
     */
    public void addExecutable(ExecutableSelector e) {
        appendSelector(e);
    }

    /**
     * @param e SymlinkSelector
     * @since 1.10.0
     */
    public void addSymlink(SymlinkSelector e) {
        appendSelector(e);
    }

    /**
     * @param o OwnedBySelector
     * @since 1.10.0
     */
    public void addOwnedBy(OwnedBySelector o) {
        appendSelector(o);
    }

    /**
     * @param o PosixGroupSelector
     * @since 1.10.4
     */
    public void addPosixGroup(PosixGroupSelector o) {
        appendSelector(o);
    }

    /**
     * @param o PosixPermissionsSelector
     * @since 1.10.4
     */
    public void addPosixPermissions(PosixPermissionsSelector o) {
        appendSelector(o);
    }

    /**
     * add an arbitrary selector
     * @param selector the selector to add
     * @since Ant 1.6
     */
    public void add(FileSelector selector) {
        appendSelector(selector);
    }

    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p) {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            for (FileSelector fileSelector : selectorsList) {
                if (fileSelector instanceof DataType) {
                    pushAndInvokeCircularReferenceCheck((DataType) fileSelector, stk, p);
                }
            }
            setChecked(true);
        }
    }

    /**
     * Clone the selector container.
     * @return a deep clone of a selector container.
     */
    public synchronized Object clone() {
        if (isReference()) {
            return getRef().clone();
        }
        try {
            AbstractSelectorContainer sc =
                (AbstractSelectorContainer) super.clone();
            sc.selectorsList = new Vector<>(selectorsList);
            return sc;
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
    }


    private AbstractSelectorContainer getRef(Project p) {
        return getCheckedRef(AbstractSelectorContainer.class, getDataTypeName(), p);
    }

    private AbstractSelectorContainer getRef() {
        return getCheckedRef(AbstractSelectorContainer.class);
    }

}
