/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * Replace tokens with user supplied values
 *
 * Example Usage:
 * =============
 *
 * &lt;replacetokens begintoken=&quot;#&quot; endtoken=&quot;#&quot;&gt;
 *   &lt;token key=&quot;DATE&quot; value=&quot;${TODAY}&quot;/&gt;
 * &lt;/replacetokens&gt;
 *
 * Or:
 *
 * &lt;filterreader classname="org.apache.tools.ant.filters.ReplaceTokens"&gt;
 *    &lt;param type="tokenchar" name="begintoken" value="#"/&gt;
 *    &lt;param type="tokenchar" name="endtoken" value="#"/&gt;
 *    &lt;param type="token" name="DATE" value="${TODAY}"/&gt;
 * &lt;/filterreader&gt;
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class ReplaceTokens
    extends BaseFilterReader
    implements Parameterizable, ChainableReader
{
    /** Default begin token character. */
    private static final char DEFAULT_BEGIN_TOKEN = '@';

    /** Default end token character. */
    private static final char DEFAULT_END_TOKEN = '@';

    /** Data that must be read from, if not null. */
    private String queuedData = null;

    /** The passed in parameter array. */
    private Parameter[] parameters;

    /** Hashtable to hold the replacee-replacer pairs. */
    private Hashtable hash = new Hashtable();

    /** Have the parameters passed been interpreted? */
    private boolean initialized;

    /** Begin token. */
    private char beginToken = DEFAULT_BEGIN_TOKEN;

    /** End token. */
    private char endToken = DEFAULT_END_TOKEN;

    /**
     * This constructor is a dummy constructor and is
     * not meant to be used by any class other than Ant's
     * introspection mechanism. This will close the filter
     * that is created making it useless for further operations.
     */
    public ReplaceTokens() {
        super();
    }

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public ReplaceTokens(final Reader in) {
        super(in);
    }

    /**
     * Replace tokens with values.
     */
    public final int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        if (queuedData != null && queuedData.length() > 0) {
            final int ch = queuedData.charAt(0);
            if (queuedData.length() > 1) {
                queuedData = queuedData.substring(1);
            } else {
                queuedData = null;
            }
            return ch;
        }

        int ch = in.read();
        if (ch == beginToken) {
            final StringBuffer key = new StringBuffer("");
            do  {
                ch = in.read();
                if (ch != -1) {
                    key.append((char) ch);
                } else {
                    break;
                }
            } while (ch != endToken);

            if (ch == -1) {
                queuedData = beginToken + key.toString();
                return read();
            } else {
                key.setLength(key.length() - 1);
                final String replaceWith = (String) hash.get(key.toString());
                if (replaceWith != null) {
                    queuedData = replaceWith;
                    return read();
                } else {
                    queuedData = beginToken + key.toString() + endToken;
                    return read();
                }
            }
        }
        return ch;
    }

    /**
     * Set begin token.
     */
    public final void setBeginToken(final char beginToken) {
        this.beginToken = beginToken;
    }

    /**
     * Get begin token.
     */
    private final char getBeginToken() {
        return beginToken;
    }

    /**
     * Set end token.
     */
    public final void setEndToken(final char endToken) {
        this.endToken = endToken;
    }

    /**
     * Get begin token.
     */
    private final char getEndToken() {
        return endToken;
    }

    /**
     * Add a token element.
     */
    public final void addConfiguredToken(final Token token) {
        hash.put(token.getKey(), token.getValue());
    }

    /**
     * Set the tokens.
     */
    private void setTokens(final Hashtable hash) {
        this.hash = hash;
    }

    /**
     * Get the tokens.
     */
    private final Hashtable getTokens() {
        return hash;
    }

    /**
     * Set the initialized status.
     */
    private final void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Get the initialized status.
     */
    private final boolean getInitialized() {
        return initialized;
    }

    /**
     * Create a new ReplaceTokens using the passed in
     * Reader for instantiation.
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
     * Set Parameters
     */
    public final void setParameters(final Parameter[] parameters) {
        this.parameters = parameters;
        setInitialized(false);
    }

    /**
     * Initialize tokens and load the replacee-replacer hashtable.
     */
    private final void initialize() {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] != null) {
                    final String type = parameters[i].getType();
                    if ("tokenchar".equals(type)) {
                        final String name = parameters[i].getName();
                        if ("begintoken".equals(name)) {
                            beginToken = parameters[i].getValue().charAt(0);
                        } else if ("endtoken".equals(name)) {
                            endToken = parameters[i].getValue().charAt(0);
                        }
                    } else if ("token".equals(type)) {
                        final String name = parameters[i].getName();
                        final String value = parameters[i].getValue();
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

        /** token key */
        private String key;

        /** token value */
        private String value;

        /**
         * Set the token key
         */
        public final void setKey(String key) {
            this.key = key;
        }

        /**
         * Set the token value
         */
        public final void setValue(String value) {
            this.value = value;
        }

        /**
         * Get the token key
         */
        public final String getKey() {
            return key;
        }

        /**
         * Get the token value
         */
        public final String getValue() {
            return value;
        }
    }
}
