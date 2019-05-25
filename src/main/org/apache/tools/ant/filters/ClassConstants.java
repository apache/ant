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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.apache.tools.ant.BuildException;

/**
 * Assembles the constants declared in a Java class in
 * <code>key1=value1(line separator)key2=value2</code>
 * format.
 *<p>Notes:</p>
 * <ol>
 * <li>This filter uses the BCEL external toolkit.
 * <li>This assembles only those constants that are not created
 * using the syntax <code>new whatever()</code>
 * <li>This assembles constants declared using the basic datatypes
 * and String only.</li>
 * <li>The access modifiers of the declared constants do not matter.</li>
 *</ol>
 * <p>Example:</p>
 * <pre>&lt;classconstants/&gt;</pre>
 * Or:
 * <pre>&lt;filterreader
 *    classname=&quot;org.apache.tools.ant.filters.ClassConstants&quot;/&gt;</pre>
 */
public final class ClassConstants
    extends BaseFilterReader
    implements ChainableReader {
    /** Data that must be read from, if not null. */
    private String queuedData = null;

    /** Helper Class to be invoked via reflection. */
    private static final String JAVA_CLASS_HELPER =
        "org.apache.tools.ant.filters.util.JavaClassHelper";

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public ClassConstants() {
        super();
    }

    /**
     * Creates a new filtered reader. The contents of the passed-in reader
     * are expected to be the name of the class from which to produce a
     * list of constants.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public ClassConstants(final Reader in) {
        super(in);
    }

    /**
     * Reads and assembles the constants declared in a class file.
     *
     * @return the next character in the list of constants, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading, or if the constants for the specified class cannot
     * be read (for example due to the class not being found).
     */
    public int read() throws IOException {
        int ch = -1;

        if (queuedData != null && queuedData.isEmpty()) {
            queuedData = null;
        }

        if (queuedData == null) {
            final String clazz = readFully();
            if (clazz == null || clazz.isEmpty()) {
                ch = -1;
            } else {
                final byte[] bytes = clazz.getBytes(StandardCharsets.ISO_8859_1);
                try {
                    final Class<?> javaClassHelper = Class.forName(JAVA_CLASS_HELPER);
                    if (javaClassHelper != null) {
                        final Method getConstants =
                            javaClassHelper.getMethod("getConstants", byte[].class);
                        // getConstants is a static method, no need to
                        // pass in the object
                        final StringBuffer sb = (StringBuffer)
                                getConstants.invoke(null, (Object) bytes);
                        if (sb.length() > 0) {
                            queuedData = sb.toString();
                            return read();
                        }
                    }
                } catch (NoClassDefFoundError | RuntimeException ex) {
                    throw ex;
                } catch (InvocationTargetException ex) {
                    Throwable t = ex.getTargetException();
                    if (t instanceof NoClassDefFoundError) {
                        throw (NoClassDefFoundError) t;
                    }
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new BuildException(t);
                } catch (Exception ex) {
                    throw new BuildException(ex);
                }
            }
        } else {
            ch = queuedData.charAt(0);
            queuedData = queuedData.substring(1);
            if (queuedData.isEmpty()) {
                queuedData = null;
            }
        }
        return ch;
    }

    /**
     * Creates a new ClassConstants using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(final Reader rdr) {
        return new ClassConstants(rdr);
    }
}
