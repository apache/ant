/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.types.selectors;

import java.util.Enumeration;
import java.io.File;

import org.apache.tools.ant.Project;

/**
 * This selector just holds one other selector and forwards all
 * requests to it. It exists so that there is a single selector
 * type that can exist outside of any targets, as an element of
 * project. It overrides all of the reference stuff so that it
 * works as expected. Note that this is the only selector you
 * can reference.
 *
 * @since 1.5
 */
public class SelectSelector extends BaseSelectorContainer {

    private String ifProperty;
    private String unlessProperty;

    /**
     * Default constructor.
     */
    public SelectSelector() {
    }

    /**
     * @return a string describing this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (hasSelectors()) {
            buf.append("{select");
            if (ifProperty != null) {
                buf.append(" if: ");
                buf.append(ifProperty);
            }
            if (unlessProperty != null) {
                buf.append(" unless: ");
                buf.append(unlessProperty);
            }
            buf.append(" ");
            buf.append(super.toString());
            buf.append("}");
        }
        return buf.toString();
    }

    /**
     * Performs the check for circular references and returns the
     * referenced Selector.
     */
    private SelectSelector getRef() {
        Object o = getCheckedRef(this.getClass(), "SelectSelector");
        return (SelectSelector) o;
    }

    /**
     * Indicates whether there are any selectors here.
     * @return whether any selectors are in this container
     */
    public boolean hasSelectors() {
        if (isReference()) {
            return getRef().hasSelectors();
        }
        return super.hasSelectors();
    }

    /**
     * Gives the count of the number of selectors in this container
     * @return the number of selectors in this container
     */
    public int selectorCount() {
        if (isReference()) {
            return getRef().selectorCount();
        }
        return super.selectorCount();
    }

    /**
     * Returns the set of selectors as an array.
     * @param p the current project
     * @return an array of selectors in this container
     */
    public FileSelector[] getSelectors(Project p) {
        if (isReference()) {
            return getRef().getSelectors(p);
        }
        return super.getSelectors(p);
    }

    /**
     * Returns an enumerator for accessing the set of selectors.
     * @return an enumerator that goes through each of the selectors
     */
    public Enumeration selectorElements() {
        if (isReference()) {
            return getRef().selectorElements();
        }
        return super.selectorElements();
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
        super.appendSelector(selector);
    }


    /**
     * Makes sure that there is only one entry, sets an error message if
     * not.
     */
    public void verifySettings() {
        int cnt = selectorCount();
        if (cnt < 0 || cnt > 1) {
            setError("Only one selector is allowed within the "
                + "<selector> tag");
        }
    }

    /**
     * Ensures that the selector passes the conditions placed
     * on it with <code>if</code> and <code>unless</code>.
     * @return true if conditions are passed
     */
    public boolean passesConditions() {
        if (ifProperty != null
            && getProject().getProperty(ifProperty) == null) {
            return false;
        } else if (unlessProperty != null
            && getProject().getProperty(unlessProperty) != null) {
            return false;
        }
        return true;
    }

    /**
     * Sets the if attribute to a property which must exist for the
     * selector to select any files.
     * @param ifProperty the property to check
     */
    public void setIf(String ifProperty) {
        this.ifProperty = ifProperty;
    }

    /**
     * Sets the unless attribute to a property which cannot exist for the
     * selector to select any files.
     * @param unlessProperty the property to check
     */
    public void setUnless(String unlessProperty) {
        this.unlessProperty = unlessProperty;
    }

    /**
     * Returns true (the file is selected) only if the if property (if any)
     * exists, the unless property (if any) doesn't exist, and the
     * contained selector (if any) selects the file. If there is no contained
     * selector, return true (because we assume that the point was to test
     * the if and unless conditions).
     *
     * @param basedir the base directory the scan is being done from
     * @param filename the name of the file to check
     * @param file a java.io.File object for the filename that the selector
     * can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {
        validate();

        // Deal with if and unless properties first
        if (!(passesConditions())) {
            return false;
        }

        Enumeration e = selectorElements();
        if (!(e.hasMoreElements())) {
            return true;
        }
        FileSelector f = (FileSelector) e.nextElement();
        return f.isSelected(basedir, filename, file);
    }
}

