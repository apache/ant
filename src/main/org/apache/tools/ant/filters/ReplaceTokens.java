/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.BuildException;

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
 * @author Magesh Umasankar
 */
public final class ReplaceTokens
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Default "begin token" character. */
    private static final char DEFAULT_BEGIN_TOKEN = '@';

    /** Default "end token" character. */
    private static final char DEFAULT_END_TOKEN = '@';

    /** Data to be used before reading from stream again */
    private String queuedData = null;

    /** replacement test from a token */
    private String replaceData = null;

    /** Index into replacement data */
    private int replaceIndex = -1;
    
    /** Index into queue data */
    private int queueIndex = -1;
    
    /** Hashtable to hold the replacee-replacer pairs (String to String). */
    private Hashtable hash = new Hashtable();

    /** Character marking the beginning of a token. */
    private char beginToken = DEFAULT_BEGIN_TOKEN;

    /** Character marking the end of a token. */
    private char endToken = DEFAULT_END_TOKEN;

    /**
     * Constructor for "dummy" instances.
     * 
     * @see BaseFilterReader#BaseFilterReader()
     */
    public ReplaceTokens() {
        super();
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

    private int getNextChar() throws IOException {
        if (queueIndex != -1) {
            final int ch = queuedData.charAt(queueIndex++);
            if (queueIndex >= queuedData.length()) {
                queueIndex = -1;
            }
            return ch;
        }
        
        return in.read();
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
    public final int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        if (replaceIndex != -1) {
            final int ch = replaceData.charAt(replaceIndex++);
            if (replaceIndex >= replaceData.length()) {
                replaceIndex = -1;
            }
            return ch;
        }
        
        int ch = getNextChar();

        if (ch == beginToken) {
            final StringBuffer key = new StringBuffer("");
            do  {
                ch = getNextChar();
                if (ch != -1) {
                    key.append((char) ch);
                } else {
                    break;
                }
            } while (ch != endToken);

            if (ch == -1) {
                if (queuedData == null || queueIndex == -1) {
                    queuedData = key.toString();
                } else {
                    queuedData 
                        = key.toString() + queuedData.substring(queueIndex);
                }
                queueIndex = 0;
                return beginToken;
            } else {
                key.setLength(key.length() - 1);
                
                final String replaceWith = (String) hash.get(key.toString());
                if (replaceWith != null) {
                    replaceData = replaceWith;
                    replaceIndex = 0;
                    return read();
                } else {
                    String newData = key.toString() + endToken;
                    if (queuedData == null || queueIndex == -1) {
                        queuedData = newData;
                    } else {
                        queuedData = newData + queuedData.substring(queueIndex);
                    }
                    queueIndex = 0;
                    return beginToken;
                }
            }
        }
        return ch;
    }

    /**
     * Sets the "begin token" character.
     * 
     * @param beginToken the character used to denote the beginning of a token
     */
    public final void setBeginToken(final char beginToken) {
        this.beginToken = beginToken;
    }

    /**
     * Returns the "begin token" character.
     * 
     * @return the character used to denote the beginning of a token
     */
    private final char getBeginToken() {
        return beginToken;
    }

    /**
     * Sets the "end token" character.
     * 
     * @param endToken the character used to denote the end of a token
     */
    public final void setEndToken(final char endToken) {
        this.endToken = endToken;
    }

    /**
     * Returns the "end token" character.
     * 
     * @return the character used to denote the end of a token
     */
    private final char getEndToken() {
        return endToken;
    }

    /**
     * Adds a token element to the map of tokens to replace.
     * 
     * @param token The token to add to the map of replacements.
     *              Must not be <code>null</code>.
     */
    public final void addConfiguredToken(final Token token) {
        hash.put(token.getKey(), token.getValue());
    }

    /**
     * Sets the map of tokens to replace.
     * 
     * @param hash A map (String->String) of token keys to replacement
     * values. Must not be <code>null</code>.
     */
    private void setTokens(final Hashtable hash) {
        this.hash = hash;
    }

    /**
     * Returns the map of tokens which will be replaced.
     * 
     * @return a map (String->String) of token keys to replacement
     * values
     */
    private final Hashtable getTokens() {
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
    public final Reader chain(final Reader rdr) {
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
    private final void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    final String type = params[i].getType();
                    if ("tokenchar".equals(type)) {
                        final String name = params[i].getName();
                        String value = params[i].getValue();
                        if ("begintoken".equals(name)) {
                            if (value.length() == 0) {
                                throw new BuildException("Begin token cannot " 
                                    + "be empty");
                            }
                            beginToken = params[i].getValue().charAt(0);
                        } else if ("endtoken".equals(name)) {
                            if (value.length() == 0) {
                                throw new BuildException("End token cannot " 
                                    + "be empty");
                            }
                            endToken = params[i].getValue().charAt(0);
                        }
                    } else if ("token".equals(type)) {
                        final String name = params[i].getName();
                        final String value = params[i].getValue();
                        hash.put(name, value);
                    }
                }
            }
        }
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
