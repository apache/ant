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
package org.apache.tools.ant.types;

import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.filters.ClassConstants;
import org.apache.tools.ant.filters.ExpandProperties;
import org.apache.tools.ant.filters.HeadFilter;
import org.apache.tools.ant.filters.LineContains;
import org.apache.tools.ant.filters.LineContainsRegExp;
import org.apache.tools.ant.filters.PrefixLines;
import org.apache.tools.ant.filters.ReplaceTokens;
import org.apache.tools.ant.filters.StripJavaComments;
import org.apache.tools.ant.filters.StripLineBreaks;
import org.apache.tools.ant.filters.StripLineComments;
import org.apache.tools.ant.filters.TabsToSpaces;
import org.apache.tools.ant.filters.TailFilter;

/**
 * FilterChain may contain a chained set of filter readers.
 *
 * @author Magesh Umasankar
 */
public final class FilterChain extends DataType implements Cloneable {

    private Vector filterReaders = new Vector();

    public final void addFilterReader(final AntFilterReader filterReader) {
        filterReaders.addElement(filterReader);
    }

    public final Vector getFilterReaders() {
        return filterReaders;
    }

    public final void addClassConstants(final ClassConstants classConstants) {
        filterReaders.addElement(classConstants);
    }

    public final void addExpandProperties(final ExpandProperties expandProperties) {
        filterReaders.addElement(expandProperties);
    }

    public final void addHeadFilter(final HeadFilter headFilter) {
        filterReaders.addElement(headFilter);
    }

    public final void addLineContains(final LineContains lineContains) {
        filterReaders.addElement(lineContains);
    }

    public final void addLineContainsRegExp(final LineContainsRegExp
                                                lineContainsRegExp) {
        filterReaders.addElement(lineContainsRegExp);
    }

    public final void addPrefixLines(final PrefixLines prefixLines) {
        filterReaders.addElement(prefixLines);
    }

    public final void addReplaceTokens(final ReplaceTokens replaceTokens) {
        filterReaders.addElement(replaceTokens);
    }

    public final void addStripJavaComments(final StripJavaComments
                                                stripJavaComments) {
        filterReaders.addElement(stripJavaComments);
    }

    public final void addStripLineBreaks(final StripLineBreaks
                                                stripLineBreaks) {
        filterReaders.addElement(stripLineBreaks);
    }

    public final void addStripLineComments(final StripLineComments
                                                stripLineComments) {
        filterReaders.addElement(stripLineComments);
    }

    public final void addTabsToSpaces(final TabsToSpaces tabsToSpaces) {
        filterReaders.addElement(tabsToSpaces);
    }

    public final void addTailFilter(final TailFilter tailFilter) {
        filterReaders.addElement(tailFilter);
    }

    /**
     * Makes this instance in effect a reference to another FilterChain 
     * instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     *
     * @param r the reference to which this instance is associated
     * @exception BuildException if this instance already has been configured.
     */
    public void setRefid(Reference r) throws BuildException {
        if (!filterReaders.isEmpty()) {
            throw tooManyAttributes();
        }
        // change this to get the objects from the other reference
        Object o = r.getReferencedObject(getProject());
        if (o instanceof FilterChain) {
            FilterChain fc = (FilterChain) o;
            filterReaders = fc.getFilterReaders();
        } else {
            String msg = r.getRefId() + " doesn\'t refer to a FilterChain";
            throw new BuildException(msg);
        }

        super.setRefid(r);
    }
}
