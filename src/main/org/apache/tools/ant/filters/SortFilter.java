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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Parameter;

/**
 * <p>
 * Sort a file before and/or after the file.
 * </p>
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 *   &lt;copy todir=&quot;build&quot;&gt;
 *       &lt;fileset dir=&quot;input&quot; includes=&quot;*.txt&quot;/&gt;
 *       &lt;filterchain&gt;
 *           &lt;sortfilter/&gt;
 *       &lt;/filterchain&gt;
 *   &lt;/copy&gt;
 * </pre>
 *
 * <p>
 * Sort all files <code>*.txt</code> from <i>src</i> location and copy
 * them into <i>build</i> location. The lines of each file are sorted
 * in ascendant order comparing the lines via the
 * <code>String.compareTo(Object o)</code> method.
 * </p>
 *
 * <pre>
 *   &lt;copy todir=&quot;build&quot;&gt;
 *       &lt;fileset dir=&quot;input&quot; includes=&quot;*.txt&quot;/&gt;
 *       &lt;filterchain&gt;
 *           &lt;sortfilter reverse=&quot;true&quot;/&gt;
 *       &lt;/filterchain&gt;
 *   &lt;/copy&gt;
 * </pre>
 *
 * <p>
 * Sort all files <code>*.txt</code> from <i>src</i> location into reverse
 * order and copy them into <i>build</i> location. If reverse parameter has
 * value <code>true</code> (default value), then the output line of the files
 * will be in ascendant order.
 * </p>
 *
 * <pre>
 *   &lt;copy todir=&quot;build&quot;&gt;
 *       &lt;fileset dir=&quot;input&quot; includes=&quot;*.txt&quot;/&gt;
 *       &lt;filterchain&gt;
 *           &lt;filterreader classname=&quot;org.apache.tools.ant.filters.SortFilter&quot;&gt;
 *             &lt;param name=&quot;comparator&quot; value=&quot;org.apache.tools.ant.filters.EvenFirstCmp&quot;/&gt;
 *           &lt;/filterreader&gt;
 *       &lt;/filterchain&gt;
 *   &lt;/copy&gt;
 * </pre>
 *
 * <p>
 * Sort all files <code>*.txt</code> from <i>src</i> location using as
 * sorting criterion <code>EvenFirstCmp</code> class, that sorts the file
 * lines putting even lines first then odd lines for example. The modified files
 * are copied into <i>build</i> location. The <code>EvenFirstCmp</code>,
 * has to an instantiable class via <code>Class.newInstance()</code>,
 * therefore in case of inner class has to be <em>static</em>. It also has to
 * implement <code>java.util.Comparator</code> interface, for example:
 * </p>
 *
 * <pre>
 *         package org.apache.tools.ant.filters;
 *         ...(omitted)
 *           public final class EvenFirstCmp implements &lt;b&gt;Comparator&lt;/b&gt; {
 *             public int compare(Object o1, Object o2) {
 *             ...(omitted)
 *             }
 *           }
 * </pre>
 *
 * <p>The example above is equivalent to:</p>
 *
 * <pre>
 *   &lt;componentdef name="evenfirst"
 *                 classname="org.apache.tools.ant.filters.EvenFirstCmp&quot;/&gt;
 *   &lt;copy todir=&quot;build&quot;&gt;
 *       &lt;fileset dir=&quot;input&quot; includes=&quot;*.txt&quot;/&gt;
 *       &lt;filterchain&gt;
 *           &lt;sortfilter&gt;
 *               &lt;evenfirst/&gt;
 *           &lt;/sortfilter&gt;
 *       &lt;/filterchain&gt;
 *   &lt;/copy&gt;
 * </pre>
 *
 * <p>If parameter <code>comparator</code> is present, then
 * <code>reverse</code> parameter will not be taken into account.</p>
 *
 * @since Ant 1.8.0
 */
public final class SortFilter extends BaseParamFilterReader
    implements ChainableReader {

    /** Parameter name for reverse order. */
    private static final String REVERSE_KEY = "reverse";

    /**
     * Parameter name for specifying the comparator criteria via class that
     * implement <code>java.util.Comparator</code> interface.
     */
    private static final String COMPARATOR_KEY = "comparator";

    /**
     * Instance of comparator class to be used for sorting.
     */
    private Comparator<? super String> comparator = null;

    /**
     * Controls if the sorting process will be in ascendant/descendant order. If
     * If has value <code>true</code>, then the line of the file will be
     * sorted on descendant order. Default value: <code>false</code>. It will
     * be considered only if <code>comparator</code> is <code>null</code>.
     */
    private boolean reverse;

    /**
     * Stores the lines to be sorted.
     */
    private List<String> lines;

    /**
     * Remaining line to be read from this filter, or <code>null</code> if the
     * next call to <code>read()</code> should read the original stream to
     * find the next matching line.
     */
    private String line = null;

    private Iterator<String> iterator = null;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public SortFilter() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in
     *            A Reader object providing the underlying stream. Must not be
     *            <code>null</code>.
     */
    public SortFilter(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream. If the desired number
     * of lines have already been read, the resulting stream is effectively at
     * an end. Otherwise, the next character from the underlying stream is read
     * and returned.
     *
     * @return the next character in the resulting stream, or -1 if the end of
     *         the resulting stream has been reached
     *
     * @exception IOException
     *                if the underlying stream throws an IOException during
     *                reading
     */
    public int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;
        if (line != null) {
            /*
             * We are on the state: "reading the current line", lines are
             * already sorted
             */
            ch = line.charAt(0);
            if (line.length() == 1) {
                line = null;
            } else {
                line = line.substring(1);
            }
        } else {
            if (lines == null) {
                // We read all lines and sort them
                lines = new ArrayList<>();
                for (line = readLine(); line != null; line = readLine()) {
                    lines.add(line);
                }
                sort();
                iterator = lines.iterator();
            }

            if (iterator.hasNext()) {
                line = iterator.next();
            } else {
                line = null;
                lines = null;
                iterator = null;
            }
            if (line != null) {
                return read();
            }
        }
        return ch;
    }

    /**
     * Creates a new SortReader using the passed in Reader for instantiation.
     *
     * @param rdr
     *            A Reader object providing the underlying stream. Must not be
     *            <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering the
     *         specified reader
     */
    public Reader chain(final Reader rdr) {
        SortFilter newFilter = new SortFilter(rdr);
        newFilter.setReverse(isReverse());
        newFilter.setComparator(getComparator());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Returns <code>true</code> if the sorting process will be in reverse
     * order, otherwise the sorting process will be in ascendant order.
     *
     * @return <code>true</code> if the sorting process will be in reverse
     *         order, otherwise the sorting process will be in ascendant order.
     */
    public boolean isReverse() {
        return reverse;
    }

    /**
     * Sets the sorting process will be in ascendant (<code>reverse=false</code>)
     * or to descendant (<code>reverse=true</code>).
     *
     * @param reverse
     *            Boolean representing reverse ordering process.
     */
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    /**
     * Returns the comparator to be used for sorting.
     *
     * @return the comparator
     */
    public Comparator<? super String> getComparator() {
        return comparator;
    }

    /**
     * Set the comparator to be used as sorting criterion.
     *
     * @param comparator
     *            the comparator to set
     */
    public void setComparator(Comparator<? super String> comparator) {
        this.comparator = comparator;
    }

    /**
     * Set the comparator to be used as sorting criterion as nested element.
     *
     * @param comparator
     *            the comparator to set
     */
    public void add(Comparator<? super String> comparator) {
        if (this.comparator != null && comparator != null) {
            throw new BuildException("can't have more than one comparator");
        }
        setComparator(comparator);
    }

    /**
     * Scans the parameters list
     */
    private void initialize() {
        // get parameters
        Parameter[] params = getParameters();
        if (params != null) {
            for (Parameter param : params) {
                final String paramName = param.getName();
                if (REVERSE_KEY.equals(paramName)) {
                    setReverse(Boolean.parseBoolean(param.getValue()));
                } else if (COMPARATOR_KEY.equals(paramName)) {
                    try {
                        String className = param.getValue();
                        @SuppressWarnings("unchecked")
                        final Comparator<? super String> comparatorInstance
                                = (Comparator<? super String>) (Class.forName(className).getDeclaredConstructor().newInstance());
                        setComparator(comparatorInstance);
                    } catch (ClassCastException e) {
                        throw new BuildException("Value of comparator attribute"
                                                 + " should implement"
                                                 + " java.util.Comparator"
                                                 + " interface");
                    } catch (Exception e) {
                        /*
                         * IAE probably means an inner non-static class, that case is not considered
                         */
                        throw new BuildException(e);
                    }
                }
            }
        }
    }

    /**
     * Sorts the read lines (<code>lines</code>) according to the sorting
     * criteria defined by the user.
     *
     */
    private void sort() {
        if (comparator == null) {
            if (isReverse()) {
                lines.sort(Comparator.reverseOrder());
            } else {
                Collections.sort(lines);
            }
        } else {
            lines.sort(comparator);
        }
    }
}
