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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.Project;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;

/**
 * Assemble the constants declared in a file in
 * key1=value1(line separator)key2=value2
 * format
 *
 * Notes:
 * =====
 * 1. This filter uses the BCEL external toolkit.
 * 2. This assembles only those constants that are not created
 *    using the syntax new whatever().
 * 3. This assembles constants declared using the basic datatypes
 *    and String only.
 * 4. The access modifiers of the declared constants do not matter.
 *
 * Example:
 * =======
 *
 * &lt;classconstants/&gt;
 *
 * Or:
 *
 * &lt;filterreader classname=&quot;org.apache.tools.ant.filters.ClassConstants&quot;/&gt;
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class ClassConstants
    extends BaseFilterReader
    implements ChainableReader
{
    /** System specific line separator. */
    private static final String LS = System.getProperty("line.separator");

    /** Data that must be read from, if not null. */
    private String queuedData = null;

    /**
     * This constructor is a dummy constructor and is
     * not meant to be used by any class other than Ant's
     * introspection mechanism. This will close the filter
     * that is created making it useless for further operations.
     */
    public ClassConstants() {
        super();
    }

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public ClassConstants(final Reader in) {
        super(in);
    }

    /**
     * Read and assemble the constants declared in a class file.
     */
    public final int read() throws IOException {

        int ch = -1;

        if (queuedData != null && queuedData.length() == 0) {
            queuedData = null;
        }

        if (queuedData != null) {
            ch = queuedData.charAt(0);
            queuedData = queuedData.substring(1);
            if (queuedData.length() == 0) {
                queuedData = null;
            }
        } else {
            String clazz = readFully();
            if (clazz == null) {
                ch = -1;
            } else {
                byte[] bytes = clazz.getBytes();
                StringBuffer sb = new StringBuffer();
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ClassParser parser = new ClassParser(bis, "");
                JavaClass javaClass = parser.parse();
                Field[] fields = javaClass.getFields();
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    if (field != null) {
                        ConstantValue cv = field.getConstantValue();
                        if (cv != null) {
                            String cvs = cv.toString();
                            //Remove start and end quotes if field is a String
                            if (cvs.startsWith("\"") && cvs.endsWith("\"")) {
                                cvs = cvs.substring(1, cvs.length() - 1);
                            }
                            sb.append(field.getName());
                            sb.append('=');
                            sb.append(cvs);
                            sb.append(LS);
                        }
                    }
                }

                if (sb.length() > 0) {
                    queuedData = sb.toString();
                    return read();
                }
            }
        }
        return ch;
    }

    /**
     * Create a new ClassConstants using the passed in
     * Reader for instantiation.
     */
    public final Reader chain(final Reader rdr) {
        ClassConstants newFilter = new ClassConstants(rdr);
        return newFilter;
    }
}
