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
 * This is the interface for selectors that can contain other selectors.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since 1.5
 */
public interface SelectorContainer {

    /**
     * Indicates whether there are any selectors here.
     *
     * @return whether any selectors are in this container
     */
    public boolean hasSelectors();

    /**
     * Gives the count of the number of selectors in this container
     *
     * @return the number of selectors in this container
     */
    public int selectorCount();

    /**
     * Returns the set of selectors as an array.
     *
     * @return an array of selectors in this container
     */
    public FileSelector[] getSelectors(Project p);

    /**
     * Returns an enumerator for accessing the set of selectors.
     *
     * @return an enumerator that goes through each of the selectors
     */
    public Enumeration selectorElements();

    /**
     * Add a new selector into this container.
     *
     * @param selector the new selector to add
     * @return the selector that was added
     */
    public void appendSelector(FileSelector selector);

    /* Methods below all add specific selectors */

    /**
     * add a "Select" selector entry on the selector list
     */
    public void addSelector(SelectSelector selector);

    /**
     * add an "And" selector entry on the selector list
     */
    public void addAnd(AndSelector selector);

    /**
     * add an "Or" selector entry on the selector list
     */
    public void addOr(OrSelector selector);

    /**
     * add a "Not" selector entry on the selector list
     */
    public void addNot(NotSelector selector);

    /**
     * add a "None" selector entry on the selector list
     */
    public void addNone(NoneSelector selector);

    /**
     * add a majority selector entry on the selector list
     */
    public void addMajority(MajoritySelector selector);

    /**
     * add a selector date entry on the selector list
     */
    public void addDate(DateSelector selector);

    /**
     * add a selector size entry on the selector list
     */
    public void addSize(SizeSelector selector);

    /**
     * add a selector filename entry on the selector list
     */
    public void addFilename(FilenameSelector selector);

    /**
     * add an extended selector entry on the selector list
     */
    public void addCustom(ExtendSelector selector);

    /**
     * add a contains selector entry on the selector list
     */
    public void addContains(ContainsSelector selector);

    /**
     * add a present selector entry on the selector list
     */
    public void addPresent(PresentSelector selector);

    /**
     * add a depth selector entry on the selector list
     */
    public void addDepth(DepthSelector selector);

    /**
     * add a depends selector entry on the selector list
     */
    public void addDepend(DependSelector selector);

}

