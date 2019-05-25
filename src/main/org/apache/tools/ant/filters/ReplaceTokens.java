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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;

/**
 * Replaces tokens in the original input with user-supplied values.
 *
 * Example:
 *
 * <pre>&lt;replacetokens begintoken=&quot;#&quot; endtoken=&quot;#&quot;&gt;
 *   &lt;token key=&quot;DATE&quot; value=&quot;${TODAY}&quot;/&gt;
 * &lt;/replacetokens&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader classname="org.apache.tools.ant.filters.ReplaceTokens"&gt;
 *   &lt;param type="tokenchar" name="begintoken" value="#"/&gt;
 *   &lt;param type="tokenchar" name="endtoken" value="#"/&gt;
 *   &lt;param type="token" name="DATE" value="${TODAY}"/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 */
public final class ReplaceTokens
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Default "begin token" character. */
    private static final String DEFAULT_BEGIN_TOKEN = "@";

    /** Default "end token" character. */
    private static final String DEFAULT_END_TOKEN = "@";

    /** Hashtable to holds the original replacee-replacer pairs (String to String). */
    private Hashtable<String, String> hash = new Hashtable<>();

    /** This map holds the "resolved" tokens (begin- and end-tokens are added to make searching simpler) */
    private final TreeMap<String, String> resolvedTokens = new TreeMap<>();
    private boolean resolvedTokensBuilt = false;
    /** Used for comparisons and lookup into the resolvedTokens map. */
    private String readBuffer = "";

    /** replacement test from a token */
    private String replaceData = null;

    /** Index into replacement data */
    private int replaceIndex = -1;

    /** Character marking the beginning of a token. */
    private String beginToken = DEFAULT_BEGIN_TOKEN;

    /** Character marking the end of a token. */
    private String endToken = DEFAULT_END_TOKEN;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public ReplaceTokens() {
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public ReplaceTokens(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream, replacing tokens
     * from the original stream.
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading
     */
    public int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        if (!resolvedTokensBuilt) {
            // build the resolved tokens tree map.
            for (Map.Entry<String, String> entry : hash.entrySet()) {
                resolvedTokens.put(beginToken + entry.getKey() + endToken, entry.getValue());
            }
            resolvedTokensBuilt = true;
        }

        // are we currently serving replace data?
        if (replaceData != null) {
            if (replaceIndex < replaceData.length()) {
                return replaceData.charAt(replaceIndex++);
            } else {
                replaceData = null;
            }
        }

        // is the read buffer empty?
        if (readBuffer.isEmpty()) {
            int next = in.read();
            if (next == -1) {
                return next; // end of stream. all buffers empty.
            }
            readBuffer += (char) next;
        }

        for (;;) {
            // get the closest tokens
            SortedMap<String, String> possibleTokens = resolvedTokens.tailMap(readBuffer);
            if (possibleTokens.isEmpty() || !possibleTokens.firstKey().startsWith(readBuffer)) { // if there is none, then deliver the first char from the buffer.
                return getFirstCharacterFromReadBuffer();
            } else if (readBuffer.equals(possibleTokens.firstKey())) { // there exists a nearest token - is it an exact match?
                // we have found a token. prepare the replaceData buffer.
                replaceData = resolvedTokens.get(readBuffer);
                replaceIndex = 0;
                readBuffer = ""; // destroy the readBuffer - it's contents are being replaced entirely.
                // get the first character via recursive call.
                return read();
            } else { // nearest token is not matching exactly - read one character more.
                int next = in.read();
                if (next != -1) {
                    readBuffer += (char) next;
                } else {
                    return getFirstCharacterFromReadBuffer(); // end of stream. deliver remaining characters from buffer.
                }
            }
        }
    }

    /**
     * @return the first character from the read buffer or -1 if read buffer is empty.
     */
    private int getFirstCharacterFromReadBuffer() {
        if (readBuffer.isEmpty()) {
            return -1;
        }

        int chr = readBuffer.charAt(0);
        readBuffer = readBuffer.substring(1);
        return chr;
    }

    /**
     * Sets the "begin token" character.
     *
     * @param beginToken the character used to denote the beginning of a token
     */
    public void setBeginToken(final String beginToken) {
        this.beginToken = beginToken;
    }

    /**
     * Returns the "begin token" character.
     *
     * @return the character used to denote the beginning of a token
     */
    private String getBeginToken() {
        return beginToken;
    }

    /**
     * Sets the "end token" character.
     *
     * @param endToken the character used to denote the end of a token
     */
    public void setEndToken(final String endToken) {
        this.endToken = endToken;
    }

    /**
     * Returns the "end token" character.
     *
     * @return the character used to denote the end of a token
     */
    private String getEndToken() {
        return endToken;
    }

    /**
     * A resource containing properties, each of which is interpreted
     * as a token/value pair.
     *
     * @param r Resource
     * @since Ant 1.8.0
     */
    public void setPropertiesResource(Resource r) {
        makeTokensFromProperties(r);
    }

    /**
     * Adds a token element to the map of tokens to replace.
     *
     * @param token The token to add to the map of replacements.
     *              Must not be <code>null</code>.
     */
    public void addConfiguredToken(final Token token) {
        hash.put(token.getKey(), token.getValue());
        resolvedTokensBuilt = false; // invalidate to build them again if they have been built already.
    }

    /**
     * Returns properties from a specified properties file.
     *
     * @param resource The resource to load properties from.
     */
    private Properties getProperties(Resource resource) {
        InputStream in = null;
        Properties props = new Properties();
        try {
            in = resource.getInputStream();
            props.load(in);
        } catch (IOException ioe) {
            if (getProject() != null) {
                getProject().log("getProperties failed, " + ioe.getMessage(), Project.MSG_ERR);
            } else {
                ioe.printStackTrace(); //NOSONAR
            }
        } finally {
            FileUtils.close(in);
        }

        return props;
    }

    /**
     * Sets the map of tokens to replace.
     *
     * @param hash A map (String->String) of token keys to replacement
     * values. Must not be <code>null</code>.
     */
    private void setTokens(final Hashtable<String, String> hash) {
        this.hash = hash;
    }

    /**
     * Returns the map of tokens which will be replaced.
     *
     * @return a map (String->String) of token keys to replacement
     * values
     */
    private Hashtable<String, String> getTokens() {
        return hash;
    }

    /**
     * Creates a new ReplaceTokens using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(final Reader rdr) {
        ReplaceTokens newFilter = new ReplaceTokens(rdr);
        newFilter.setBeginToken(getBeginToken());
        newFilter.setEndToken(getEndToken());
        newFilter.setTokens(getTokens());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Initializes tokens and loads the replacee-replacer hashtable.
     */
    private void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (Parameter param : params) {
                if (param != null) {
                    final String type = param.getType();
                    if ("tokenchar".equals(type)) {
                        final String name = param.getName();
                        if ("begintoken".equals(name)) {
                            beginToken = param.getValue();
                        } else if ("endtoken".equals(name)) {
                            endToken = param.getValue();
                        }
                    } else if ("token".equals(type)) {
                        final String name = param.getName();
                        final String value = param.getValue();
                        hash.put(name, value);
                    } else if ("propertiesfile".equals(type)) {
                        makeTokensFromProperties(
                                new FileResource(new File(param.getValue())));
                    }
                }
            }
        }
    }

    private void makeTokensFromProperties(Resource r) {
        Properties props = getProperties(r);
        props.stringPropertyNames().forEach(key -> hash.put(key, props.getProperty(key)));
    }

    /**
     * Holds a token
     */
    public static class Token {

        /** Token key */
        private String key;

        /** Token value */
        private String value;

        /**
         * Sets the token key
         *
         * @param key The key for this token. Must not be <code>null</code>.
         */
        public final void setKey(String key) {
            this.key = key;
        }

        /**
         * Sets the token value
         *
         * @param value The value for this token. Must not be <code>null</code>.
         */
        public final void setValue(String value) {
            this.value = value;
        }

        /**
         * Returns the key for this token.
         *
         * @return the key for this token
         */
        public final String getKey() {
            return key;
        }

        /**
         * Returns the value for this token.
         *
         * @return the value for this token
         */
        public final String getValue() {
            return value;
        }
    }
}
