/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
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
     */
    public boolean hasSelectors() {
        if (isReference()) {
            return getRef().hasSelectors();
        }
        return super.hasSelectors();
    }

    /**
     * Gives the count of the number of selectors in this container
     */
    public int selectorCount() {
        if (isReference()) {
            return getRef().selectorCount();
        }
        return super.selectorCount();
    }

    /**
     * Returns the set of selectors as an array.
     */
    public FileSelector[] getSelectors(Project p) {
        if (isReference()) {
            return getRef().getSelectors(p);
        }
        return super.getSelectors(p);
    }

    /**
     * Returns an enumerator for accessing the set of selectors.
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
     * @return the selector that was added
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
            setError("Only one selector is allowed within the " +
                     "<selector> tag");
        }
    }

    /**
     * Ensures that the selector passes the conditions placed
     * on it with <code>if</code> and <code>unless</code>.
     */
    public boolean passesConditions() {
        if (ifProperty != null &&
            getProject().getProperty(ifProperty) == null) {
            return false;
        } else if (unlessProperty != null &&
                   getProject().getProperty(unlessProperty) != null) {
            return false;
        }
        return true;
    }

    /**
     * Sets the if attribute to a property which must exist for the
     * selector to select any files.
     */
    public void setIf(String ifProperty) {
        this.ifProperty = ifProperty;
    }

    /**
     * Sets the unless attribute to a property which cannot exist for the
     * selector to select any files.
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
        FileSelector f = (FileSelector)e.nextElement();
        return f.isSelected(basedir,filename,file);
    }
}

