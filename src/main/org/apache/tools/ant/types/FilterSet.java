/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

// java io classes
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

// java util classes
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

// ant classes
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;


/**
 * A set of filters to be applied to something.
 *
 * A filter set may have begintoken and endtokens defined.
 *
 * @author     <A href="mailto:gholam@xtra.co.nz">  Michael McCallum  </A>
 */
public class FilterSet extends DataType implements Cloneable {
    
    /**
     * Individual filter component of filterset
     *
     * @author    Michael McCallum
     */
    public static class Filter {
        /** Token which will be replaced in the filter operation */
        String token;
        
        /** The value which will replace the token in the filtering operation */
        String value;
        
        /**
         * Constructor for the Filter object
         *
         * @param token  The token which will be replaced when filtering
         * @param value  The value which will replace the token when filtering
         */
        public Filter(String token, String value) {
           this.token = token;
           this.value = value;
        }
        
        /**
         * No argument conmstructor
         */
        public Filter() {
        }
        
        /**
         * Sets the Token attribute of the Filter object
         *
         * @param token  The new Token value
         */
        public void setToken(String token) {
           this.token = token;
        }
        
        /**
         * Sets the Value attribute of the Filter object
         *
         * @param value  The new Value value
         */
        public void setValue(String value) {
           this.value = value;
        }
        
        /**
         * Gets the Token attribute of the Filter object
         *
         * @return   The Token value
         */
        public String getToken() {
           return token;
        }
        
        /**
         * Gets the Value attribute of the Filter object
         *
         * @return   The Value value
         */
        public String getValue() {
           return value;
        }
     }
    
    /**
     * The filtersfile nested element.
     *
     * @author    Michael McCallum
     */
    public class FiltersFile {
        
        /**
         * Constructor for the Filter object
         */
        public FiltersFile() {
        }
        
        /**
         * Sets the file from which filters will be read.
         *
         * @param file the file from which filters will be read.
         */
        public void setFile(File file) {
           readFiltersFromFile(file);
        }
    }
    
    /** The default token start string */
    public static final String DEFAULT_TOKEN_START = "@";
    
    /** The default token end string */
    public static final String DEFAULT_TOKEN_END = "@";
    
    private String startOfToken = DEFAULT_TOKEN_START;
    private String endOfToken = DEFAULT_TOKEN_END;
    
    /**
     * List of ordered filters and filter files.
     */
    private Vector filters = new Vector();
    
    public FilterSet() {
    }
    
    /**
     * Create a Filterset from another filterset
     *
     * @param filterset the filterset upon which this filterset will be based.
     */
    protected FilterSet(FilterSet filterset) {
        super();
        this.filters = (Vector) filterset.getFilters().clone();
    }

    protected Vector getFilters() {
        if (isReference()) {
            return getRef().getFilters();
        }
        return filters;
    }

    protected FilterSet getRef() {
        return (FilterSet) getCheckedRef(FilterSet.class, "filterset");
    }
    
    /**
     * Gets the filter hash of the FilterSet.
     *
     * @return   The hash of the tokens and values for quick lookup.
     */
    public Hashtable getFilterHash() {
        int filterSize = getFilters().size();
        Hashtable filterHash = new Hashtable(filterSize + 1);
        for (Enumeration e = getFilters().elements(); e.hasMoreElements();) {
           Filter filter = (Filter) e.nextElement();
           filterHash.put(filter.getToken(), filter.getValue());
        }
        return filterHash;
    }
    
    /**
     * set the file containing the filters for this filterset.
     *
     * @param filtersFile sets the filter fil to read filters for this filter set from.
     * @exception BuildException if there is a problem reading the filters
     */
    public void setFiltersfile(File filtersFile) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        readFiltersFromFile(filtersFile);
    }
    
    /**
     * The string used to id the beginning of a token.
     *
     * @param startOfToken  The new Begintoken value
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

    public String getBeginToken() {
        if (isReference()) {
            return getRef().getBeginToken();
        }
        return startOfToken;
    }
    
    
    /**
     * The string used to id the end of a token.
     *
     * @param endOfToken  The new Endtoken value
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

    public String getEndToken() {
        if (isReference()) {
            return getRef().getEndToken();
        }
        return endOfToken;
    }
    
    
    /**
     * Read the filters from the given file.
     *
     * @param filtersFile         the file from which filters are read
     * @exception BuildException  Throw a build exception when unable to read the
     * file.
     */
    public void readFiltersFromFile(File filtersFile) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }

        if (filtersFile.isFile()) {
           log("Reading filters from " + filtersFile, Project.MSG_VERBOSE);
           FileInputStream in = null;
           try {
              Properties props = new Properties();
              in = new FileInputStream(filtersFile);
              props.load(in);
              
              Enumeration enum = props.propertyNames();
              Vector filters = getFilters();
              while (enum.hasMoreElements()) {
                 String strPropName = (String) enum.nextElement();
                 String strValue = props.getProperty(strPropName);
                 filters.addElement(new Filter(strPropName, strValue));
              }
           } catch (Exception e) {
              throw new BuildException("Could not read filters from file: " 
                + filtersFile);
           } finally {
              if (in != null) {
                 try {
                    in.close();
                 } catch (IOException ioex) {
                 }
              }
           }
        } else {
           throw new BuildException("Must specify a file not a directory in " 
            + "the filtersfile attribute:" + filtersFile);
        }
    }
    
    /**
     * Does replacement on the given string with token matching.
     * This uses the defined begintoken and endtoken values which default to @ for both.
     *
     * @param line  The line to process the tokens in.
     * @return      The string with the tokens replaced.
     */
    public String replaceTokens(String line) {
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
                
                do {
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
                } while ((index = line.indexOf(beginToken, i)) > -1);
                
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
     * Create a new filter
     *
     * @param  the filter to be added
     */
    public void addFilter(Filter filter) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        filters.addElement(filter);
    }
    
    /**
     * Create a new FiltersFile
     *
     * @return   The filter that was created.
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
    * @param token  The token for the new filter.
    * @param value  The value for the new filter.
    */
    public void addFilter(String token, String value) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        filters.addElement(new Filter(token, value));
    }
    
    /**
    * Add a Filterset to this filter set
    *
    * @param filterSet the filterset to be added to this filterset
    */
    public void addConfiguredFilterSet(FilterSet filterSet) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        for (Enumeration e = filterSet.getFilters().elements(); e.hasMoreElements();) {
            filters.addElement(e.nextElement());
        }
    }
    
    /**
    * Test to see if this filter set it empty.
    *
    * @return   Return true if there are filter in this set otherwise false.
    */
    public boolean hasFilters() {
        return getFilters().size() > 0;
    }

    public Object clone() throws BuildException {
        if (isReference()) {
            return new FilterSet(getRef());
        } else {
            return new FilterSet(this);
        }
    }

}
 


