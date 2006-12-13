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
package org.apache.tools.ant.types;

import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.filters.ChainableReader;
import org.apache.tools.ant.filters.ClassConstants;
import org.apache.tools.ant.filters.EscapeUnicode;
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
import org.apache.tools.ant.filters.TokenFilter;


/**
 * FilterChain may contain a chained set of filter readers.
 *
 */
public class FilterChain extends DataType
    implements Cloneable {

    private Vector filterReaders = new Vector();

    /**
     * Add an AntFilterReader filter.
     *
     * @param filterReader an <code>AntFilterReader</code> value
     */
    public void addFilterReader(final AntFilterReader filterReader) {
        filterReaders.addElement(filterReader);
    }

    /**
     * Return the filters.
     *
     * @return a <code>Vector</code> value containing the filters
     */
    public Vector getFilterReaders() {
        return filterReaders;
    }

    /**
     * Add a ClassConstants filter.
     *
     * @param classConstants a <code>ClassConstants</code> value
     */
    public void addClassConstants(final ClassConstants classConstants) {
        filterReaders.addElement(classConstants);
    }

    /**
     * Add an ExpandProperties filter.
     *
     * @param expandProperties an <code>ExpandProperties</code> value
     */
    public void addExpandProperties(final ExpandProperties expandProperties) {
        filterReaders.addElement(expandProperties);
    }

    /**
     * Add a HeadFilter filter.
     *
     * @param headFilter a <code>HeadFilter</code> value
     */
    public void addHeadFilter(final HeadFilter headFilter) {
        filterReaders.addElement(headFilter);
    }

    /**
     * Add a LineContains filter.
     *
     * @param lineContains a <code>LineContains</code> value
     */
    public void addLineContains(final LineContains lineContains) {
        filterReaders.addElement(lineContains);
    }

    /**
     * Add a LineContainsRegExp filter.
     *
     * @param lineContainsRegExp a <code>LineContainsRegExp</code> value
     */
    public void addLineContainsRegExp(final LineContainsRegExp
                                                lineContainsRegExp) {
        filterReaders.addElement(lineContainsRegExp);
    }

    /**
     * Add a PrefixLines filter.
     *
     * @param prefixLines a <code>PrefixLines</code> value
     */
    public void addPrefixLines(final PrefixLines prefixLines) {
        filterReaders.addElement(prefixLines);
    }

    /**
     * Add a ReplaceTokens filter.
     *
     * @param replaceTokens a <code>ReplaceTokens</code> value
     */
    public void addReplaceTokens(final ReplaceTokens replaceTokens) {
        filterReaders.addElement(replaceTokens);
    }

    /**
     * Add a StripJavaCommands filter.
     *
     * @param stripJavaComments a <code>StripJavaComments</code> value
     */
    public void addStripJavaComments(final StripJavaComments
                                                stripJavaComments) {
        filterReaders.addElement(stripJavaComments);
    }

    /**
     * Add a StripLineBreaks filter.
     *
     * @param stripLineBreaks a <code>StripLineBreaks</code> value
     */
    public void addStripLineBreaks(final StripLineBreaks
                                                stripLineBreaks) {
        filterReaders.addElement(stripLineBreaks);
    }

    /**
     * Add a StripLineComments filter.
     *
     * @param stripLineComments a <code>StripLineComments</code> value
     */
    public void addStripLineComments(final StripLineComments
                                                stripLineComments) {
        filterReaders.addElement(stripLineComments);
    }

    /**
     * Add a TabsToSpaces filter.
     *
     * @param tabsToSpaces a <code>TabsToSpaces</code> value
     */
    public void addTabsToSpaces(final TabsToSpaces tabsToSpaces) {
        filterReaders.addElement(tabsToSpaces);
    }

    /**
     * Add a TailFilter filter.
     *
     * @param tailFilter a <code>TailFilter</code> value
     */
    public void addTailFilter(final TailFilter tailFilter) {
        filterReaders.addElement(tailFilter);
    }

    /**
     * Add an EscapeUnicode filter.
     *
     * @param escapeUnicode an <code>EscapeUnicode</code> value
     * @since Ant 1.6
     */
    public void addEscapeUnicode(final EscapeUnicode escapeUnicode) {
        filterReaders.addElement(escapeUnicode);
    }

    /**
     * Add a TokenFilter filter.
     *
     * @param tokenFilter a <code>TokenFilter</code> value
     * @since Ant 1.6
     */
    public void addTokenFilter(final TokenFilter tokenFilter) {
        filterReaders.addElement(tokenFilter);
    }

    /**
     * Add a delete characters filter.
     *
     * @param filter a <code>TokenFilter.DeleteCharacters</code> value
     * @since Ant 1.6
     */
    public void addDeleteCharacters(TokenFilter.DeleteCharacters filter) {
        filterReaders.addElement(filter);
    }

    /**
     * Add a containsregex filter.
     *
     * @param filter a <code>TokenFilter.ContainsRegex</code> value
     * @since Ant 1.6
     */
    public void addContainsRegex(TokenFilter.ContainsRegex filter) {
        filterReaders.addElement(filter);
    }

    /**
     * Add a replaceregex filter.
     *
     * @param filter a <code>TokenFilter.ReplaceRegex</code> value
     */
    public void addReplaceRegex(TokenFilter.ReplaceRegex filter) {
        filterReaders.addElement(filter);
    }

    /**
     * Add a trim filter.
     *
     * @param filter a <code>TokenFilter.Trim</code> value
     * @since Ant 1.6
     */
    public void addTrim(TokenFilter.Trim filter) {
        filterReaders.addElement(filter);
    }

    /**
     * Add a replacestring filter.
     *
     * @param filter a <code>TokenFilter.ReplaceString</code> value
     * @since Ant 1.6
     */
    public void addReplaceString(
        TokenFilter.ReplaceString filter) {
        filterReaders.addElement(filter);
    }

    /**
     * Add an ignoreBlank filter.
     *
     * @param filter a <code>TokenFilter.IgnoreBlank</code> value
     * @since Ant 1.6
     */
    public void addIgnoreBlank(
        TokenFilter.IgnoreBlank filter) {
        filterReaders.addElement(filter);
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

    /**
     * Add a chainfilter filter.
     *
     * @param filter a <code>ChainableReader</code> value
     * @since Ant 1.6
     */

    public void add(ChainableReader filter) {
        filterReaders.addElement(filter);
    }

}
