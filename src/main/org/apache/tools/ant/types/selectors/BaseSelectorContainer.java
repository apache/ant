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
 * 4. The names "Ant" and "Apache Software
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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import  org.apache.tools.ant.types.Reference;

import java.io.File;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

/**
 * This is the base class for selectors that can contain other selectors.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since 1.5
 */
public abstract class BaseSelectorContainer extends BaseSelector
        implements SelectorContainer {

    private Vector selectorsList = new Vector();

    /**
     * Default constructor.
     */
    public BaseSelectorContainer() {
    }

    /**
     * Indicates whether there are any selectors here.
     */
    public boolean hasSelectors() {
        return !(selectorsList.isEmpty());
    }

    /**
     * Gives the count of the number of selectors in this container
     */
    public int selectorCount() {
        return selectorsList.size();
    }

    /**
     * Returns the set of selectors as an array.
     */
    public FileSelector[] getSelectors(Project p) {
        FileSelector[] result = new FileSelector[selectorsList.size()];
        selectorsList.copyInto(result);
        return result;
    }

    /**
     * Returns an enumerator for accessing the set of selectors.
     */
    public Enumeration selectorElements() {
        return selectorsList.elements();
    }

    /**
     * Convert the Selectors within this container to a string. This will
     * just be a helper class for the subclasses that put their own name
     * around the contents listed here.
     *
     * @return comma separated list of Selectors contained in this one
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        Enumeration e = selectorElements();
        if (e.hasMoreElements()) {
            while(e.hasMoreElements()) {
                buf.append(e.nextElement().toString());
                if (e.hasMoreElements()) {
                    buf.append(", ");
                }
            }
        }

        return buf.toString();
    }

    /**
     * Add a new selector into this container.
     *
     * @param selector the new selector to add
     * @return the selector that was added
     */
    public void appendSelector(FileSelector selector) {
        selectorsList.addElement(selector);
    }

    /**
     * <p>This implementation validates the container by calling
     * verifySettings() and then validates each contained selector
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
        verifySettings();
        String errmsg = getError();
        if (errmsg != null) {
            throw new BuildException(errmsg);
        }
        Enumeration e = selectorElements();
        while(e.hasMoreElements()) {
            Object o = e.nextElement();
            if (o instanceof BaseSelector) {
                ((BaseSelector)o).validate();
            }
        }
    }


    /**
     * Method that each selector will implement to create their selection
     * behaviour. This is what makes SelectorContainer abstract.
     *
     * @param basedir the base directory the scan is being done from
     * @param filename the name of the file to check
     * @param file a java.io.File object for the filename that the selector
     * can use
     * @return whether the file should be selected or not
     */
    public abstract boolean isSelected(File basedir, String filename,
                                       File file);


    /* Methods below all add specific selectors */

    /**
     * add a "Select" selector entry on the selector list
     */
    public void addSelector(SelectSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an "And" selector entry on the selector list
     */
    public void addAnd(AndSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an "Or" selector entry on the selector list
     */
    public void addOr(OrSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a "Not" selector entry on the selector list
     */
    public void addNot(NotSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a "None" selector entry on the selector list
     */
    public void addNone(NoneSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a majority selector entry on the selector list
     */
    public void addMajority(MajoritySelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector date entry on the selector list
     */
    public void addDate(DateSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector size entry on the selector list
     */
    public void addSize(SizeSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a selector filename entry on the selector list
     */
    public void addFilename(FilenameSelector selector) {
        appendSelector(selector);
    }

    /**
     * add an extended selector entry on the selector list
     */
    public void addCustom(ExtendSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a contains selector entry on the selector list
     */
    public void addContains(ContainsSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a present selector entry on the selector list
     */
    public void addPresent(PresentSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a depth selector entry on the selector list
     */
    public void addDepth(DepthSelector selector) {
        appendSelector(selector);
    }

    /**
     * add a depends selector entry on the selector list
     */
    public void addDepend(DependSelector selector) {
        appendSelector(selector);
    }

}

