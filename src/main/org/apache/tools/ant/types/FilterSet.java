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

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 * A set of filters to be applied to something.
 *
 * A filter set may have begintoken and endtokens defined.
 *
 */
public class FilterSet extends DataType implements Cloneable {

    /**
     * Individual filter component of filterset.
     *
     */
    public static class Filter {
        // CheckStyle:VisibilityModifier OFF - bc
        /** Token which will be replaced in the filter operation. */
        String token;

        /** The value which will replace the token in the filtering operation. */
        String value;
        // CheckStyle:VisibilityModifier ON

        /**
         * Constructor for the Filter object.
         *
         * @param token  The token which will be replaced when filtering.
         * @param value  The value which will replace the token when filtering.
         */
        public Filter(String token, String value) {
           setToken(token);
           setValue(value);
        }

        /**
         * No-argument conmstructor.
         */
        public Filter() {
        }

        /**
         * Sets the Token attribute of the Filter object.
         *
         * @param token  The new Token value.
         */
        public void setToken(String token) {
           this.token = token;
        }

        /**
         * Sets the Value attribute of the Filter object.
         *
         * @param value  The new Value value.
         */
        public void setValue(String value) {
           this.value = value;
        }

        /**
         * Gets the Token attribute of the Filter object.
         *
         * @return   The Token value.
         */
        public String getToken() {
           return token;
        }

        /**
         * Gets the Value attribute of the Filter object.
         *
         * @return   The Value value.
         */
        public String getValue() {
           return value;
        }
     }

    /**
     * The filtersfile nested element.
     *
     */
    public class FiltersFile {

        /**
         * Constructor for the FiltersFile object.
         */
        public FiltersFile() {
        }

        /**
         * Sets the file from which filters will be read.
         *
         * @param file the file from which filters will be read.
         */
        public void setFile(File file) {
           filtersFiles.add(file);
        }
    }

    /**
     * EnumeratedAttribute to set behavior WRT missing filtersfiles:
     * "fail" (default), "warn", "ignore".
     * @since Ant 1.7
     */
    public static class OnMissing extends EnumeratedAttribute {
        private static final String[] VALUES
            = new String[] {"fail", "warn", "ignore"};

        /** Fail value */
        public static final OnMissing FAIL = new OnMissing("fail");
        /** Warn value */
        public static final OnMissing WARN = new OnMissing("warn");
        /** Ignore value */
        public static final OnMissing IGNORE = new OnMissing("ignore");

        private static final int FAIL_INDEX = 0;
        private static final int WARN_INDEX = 1;
        private static final int IGNORE_INDEX = 2;

        /**
         * Default constructor.
         */
        public OnMissing() {
        }

        /**
         * Convenience constructor.
         * @param value the value to set.
         */
        public OnMissing(String value) {
            setValue(value);
        }

        //inherit doc
        /** {@inheritDoc}. */
        public String[] getValues() {
            return VALUES;
        }
    }

    /** The default token start string */
    public static final String DEFAULT_TOKEN_START = "@";

    /** The default token end string */
    public static final String DEFAULT_TOKEN_END = "@";

    private String startOfToken = DEFAULT_TOKEN_START;
    private String endOfToken = DEFAULT_TOKEN_END;

    /** Contains a list of parsed tokens */
    private Vector passedTokens;
    /** if a duplicate token is found, this is set to true */
    private boolean duplicateToken = false;

    private boolean recurse = true;
    private Hashtable filterHash = null;
    private Vector filtersFiles = new Vector();
    private OnMissing onMissingFiltersFile = OnMissing.FAIL;
    private boolean readingFiles = false;

    private int recurseDepth = 0;

    /**
     * List of ordered filters and filter files.
     */
    private Vector filters = new Vector();

    /**
     * Default constructor.
     */
    public FilterSet() {
    }

    /**
     * Create a Filterset from another filterset.
     *
     * @param filterset the filterset upon which this filterset will be based.
     */
    protected FilterSet(FilterSet filterset) {
        super();
        this.filters = (Vector) filterset.getFilters().clone();
    }

    /**
     * Get the filters in the filter set.
     *
     * @return a Vector of Filter instances.
     */
    protected synchronized Vector getFilters() {
        if (isReference()) {
            return getRef().getFilters();
        }
        //silly hack to avoid stack overflow...
        if (!readingFiles) {
            readingFiles = true;
            for (int i = 0, sz = filtersFiles.size(); i < sz; i++) {
                readFiltersFromFile((File) filtersFiles.get(i));
            }
            filtersFiles.clear();
            readingFiles = false;
        }
        return filters;
    }

    /**
     * Get the referenced filter set.
     *
     * @return the filterset from the reference.
     */
    protected FilterSet getRef() {
        return (FilterSet) getCheckedRef(FilterSet.class, "filterset");
    }

    /**
     * Gets the filter hash of the FilterSet.
     *
     * @return   The hash of the tokens and values for quick lookup.
     */
    public synchronized Hashtable getFilterHash() {
        if (filterHash == null) {
            filterHash = new Hashtable(getFilters().size());
            for (Enumeration e = getFilters().elements(); e.hasMoreElements();) {
               Filter filter = (Filter) e.nextElement();
               filterHash.put(filter.getToken(), filter.getValue());
            }
        }
        return filterHash;
    }

    /**
     * Set the file containing the filters for this filterset.
     *
     * @param filtersFile sets the filter file from which to read filters
     *        for this filter set.
     * @throws BuildException if there is an error.
     */
    public void setFiltersfile(File filtersFile) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filtersFiles.add(filtersFile);
    }

    /**
     * Set the string used to id the beginning of a token.
     *
     * @param startOfToken  The new Begintoken value.
     */
    public void setBeginToken(String startOfToken) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (startOfToken == null || "".equals(startOfToken)) {
            throw new BuildException("beginToken must not be empty");
        }
        this.startOfToken = startOfToken;
    }

    /**
     * Get the begin token for this filterset.
     *
     * @return the filter set's begin token for filtering.
     */
    public String getBeginToken() {
        if (isReference()) {
            return getRef().getBeginToken();
        }
        return startOfToken;
    }

    /**
     * Set the string used to id the end of a token.
     *
     * @param endOfToken  The new Endtoken value.
     */
    public void setEndToken(String endOfToken) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (endOfToken == null || "".equals(endOfToken)) {
            throw new BuildException("endToken must not be empty");
        }
        this.endOfToken = endOfToken;
    }

    /**
     * Get the end token for this filterset.
     *
     * @return the filter set's end token for replacement delimiting.
     */
    public String getEndToken() {
        if (isReference()) {
            return getRef().getEndToken();
        }
        return endOfToken;
    }

    /**
     * Set whether recursive token expansion is enabled.
     * @param recurse <code>boolean</code> whether to recurse.
     */
    public void setRecurse(boolean recurse) {
        this.recurse = recurse;
    }

    /**
     * Get whether recursive token expansion is enabled.
     * @return <code>boolean</code> whether enabled.
     */
    public boolean isRecurse() {
        return recurse;
    }

    /**
     * Read the filters from the given file.
     *
     * @param filtersFile        the file from which filters are read.
     * @exception BuildException when the file cannot be read.
     */
    public synchronized void readFiltersFromFile(File filtersFile) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (!filtersFile.exists()) {
           handleMissingFile("Could not read filters from file "
                                     + filtersFile + " as it doesn't exist.");
        }
        if (filtersFile.isFile()) {
           log("Reading filters from " + filtersFile, Project.MSG_VERBOSE);
           FileInputStream in = null;
           try {
              Properties props = new Properties();
              in = new FileInputStream(filtersFile);
              props.load(in);

              Enumeration e = props.propertyNames();
              Vector filts = getFilters();
              while (e.hasMoreElements()) {
                 String strPropName = (String) e.nextElement();
                 String strValue = props.getProperty(strPropName);
                 filts.addElement(new Filter(strPropName, strValue));
              }
           } catch (Exception ex) {
              throw new BuildException("Could not read filters from file: "
                  + filtersFile, ex);
           } finally {
              FileUtils.close(in);
           }
        } else {
           handleMissingFile(
               "Must specify a file rather than a directory in "
               + "the filtersfile attribute:" + filtersFile);
        }
        filterHash = null;
    }

    /**
     * Does replacement on the given string with token matching.
     * This uses the defined begintoken and endtoken values which default
     * to @ for both.
     * This resets the passedTokens and calls iReplaceTokens to
     * do the actual replacements.
     *
     * @param line  The line in which to process embedded tokens.
     * @return      The input string after token replacement.
     */
    public synchronized String replaceTokens(String line) {
        return iReplaceTokens(line);
    }

    /**
     * Add a new filter.
     *
     * @param filter the filter to be added.
     */
    public synchronized void addFilter(Filter filter) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        filters.addElement(filter);
        filterHash = null;
    }

    /**
     * Create a new FiltersFile.
     *
     * @return The filtersfile that was created.
     */
    public FiltersFile createFiltersfile() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return new FiltersFile();
    }

    /**
     * Add a new filter made from the given token and value.
     *
     * @param token The token for the new filter.
     * @param value The value for the new filter.
     */
    public synchronized void addFilter(String token, String value) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        addFilter(new Filter(token, value));
    }

    /**
     * Add a Filterset to this filter set.
     *
     * @param filterSet the filterset to be added to this filterset
     */
    public synchronized void addConfiguredFilterSet(FilterSet filterSet) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        for (Enumeration e = filterSet.getFilters().elements(); e.hasMoreElements();) {
            addFilter((Filter) e.nextElement());
        }
    }

    /**
    * Test to see if this filter set has filters.
    *
    * @return Return true if there are filters in this set.
    */
    public synchronized boolean hasFilters() {
        return getFilters().size() > 0;
    }

    /**
     * Clone the filterset.
     *
     * @return a deep clone of this filterset.
     *
     * @throws BuildException if the clone cannot be performed.
     */
    public synchronized Object clone() throws BuildException {
        if (isReference()) {
            return ((FilterSet) getRef()).clone();
        }
        try {
            FilterSet fs = (FilterSet) super.clone();
            fs.filters = (Vector) getFilters().clone();
            fs.setProject(getProject());
            return fs;
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Set the behavior WRT missing filtersfiles.
     * @param onMissingFiltersFile the OnMissing describing the behavior.
     */
    public void setOnMissingFiltersFile(OnMissing onMissingFiltersFile) {
        this.onMissingFiltersFile = onMissingFiltersFile;
    }

    /**
     * Get the onMissingFiltersFile setting.
     * @return the OnMissing instance.
     */
    public OnMissing getOnMissingFiltersFile() {
        return onMissingFiltersFile;
    }

    /**
     * Does replacement on the given string with token matching.
     * This uses the defined begintoken and endtoken values which default
     * to @ for both.
     *
     * @param line  The line to process the tokens in.
     * @return      The string with the tokens replaced.
     */
    private synchronized String iReplaceTokens(String line) {
        String beginToken = getBeginToken();
        String endToken = getEndToken();
        int index = line.indexOf(beginToken);

        if (index > -1) {
            Hashtable tokens = getFilterHash();
            try {
                StringBuffer b = new StringBuffer();
                int i = 0;
                String token = null;
                String value = null;

                while (index > -1) {
                    //can't have zero-length token
                    int endIndex = line.indexOf(endToken,
                        index + beginToken.length() + 1);
                    if (endIndex == -1) {
                        break;
                    }
                    token
                        = line.substring(index + beginToken.length(), endIndex);
                    b.append(line.substring(i, index));
                    if (tokens.containsKey(token)) {
                        value = (String) tokens.get(token);
                        if (recurse && !value.equals(token)) {
                            // we have another token, let's parse it.
                            value = replaceTokens(value, token);
                        }
                        log("Replacing: " + beginToken + token + endToken
                            + " -> " + value, Project.MSG_VERBOSE);
                        b.append(value);
                        i = index + beginToken.length() + token.length()
                            + endToken.length();
                    } else {
                        // just append beginToken and search further
                        b.append(beginToken);
                        i = index + beginToken.length();
                    }
                    index = line.indexOf(beginToken, i);
                }

                b.append(line.substring(i));
                return b.toString();
            } catch (StringIndexOutOfBoundsException e) {
                return line;
            }
        } else {
           return line;
        }
    }

    /**
     * This parses tokens which point to tokens.
     * It also maintains a list of currently used tokens, so we cannot
     * get into an infinite loop.
     * @param line the value / token to parse.
     * @param parent the parent token (= the token it was parsed from).
     */
    private synchronized String replaceTokens(String line, String parent)
        throws BuildException {
        String beginToken = getBeginToken();
        String endToken = getEndToken();
        if (recurseDepth == 0) {
            passedTokens = new Vector();
        }
        recurseDepth++;
        if (passedTokens.contains(parent) && !duplicateToken) {
            duplicateToken = true;
            System.out.println(
                "Infinite loop in tokens. Currently known tokens : "
                + passedTokens.toString() + "\nProblem token : " + beginToken
                + parent + endToken + " called from " + beginToken
                + passedTokens.lastElement().toString() + endToken);
            recurseDepth--;
            return parent;
        }
        passedTokens.addElement(parent);
        String value = iReplaceTokens(line);
        if (value.indexOf(beginToken) == -1 && !duplicateToken
                && recurseDepth == 1) {
            passedTokens = null;
        } else if (duplicateToken) {
            // should always be the case...
            if (passedTokens.size() > 0) {
                value = (String) passedTokens.remove(passedTokens.size() - 1);
                if (passedTokens.size() == 0) {
                    value = beginToken + value + endToken;
                    duplicateToken = false;
                }
            }
        }
        recurseDepth--;
        return value;
    }

    private void handleMissingFile(String message) {
        switch (onMissingFiltersFile.getIndex()) {
        case OnMissing.IGNORE_INDEX:
            return;
        case OnMissing.FAIL_INDEX:
            throw new BuildException(message);
        case OnMissing.WARN_INDEX:
            log(message, Project.MSG_WARN);
            return;
        default:
            throw new BuildException("Invalid value for onMissingFiltersFile");
        }
    }

}
