/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.jlink;

import java.io .*;

/**
 * Reads just enough of a class file to determine the class' full name.
 *
 * <p>Extremely minimal constant pool implementation, mainly to support extracting
 * strings from a class file.
 * @author <a href="mailto:beard@netscape.com">Patrick C. Beard</a>.
 */
class ConstantPool extends Object{

    static final 
        byte UTF8 = 1, UNUSED = 2, INTEGER = 3, FLOAT = 4, LONG = 5, DOUBLE = 6,
        CLASS = 7, STRING = 8, FIELDREF = 9, METHODREF = 10,
        INTERFACEMETHODREF = 11, NAMEANDTYPE = 12;

    byte[] types;

    Object[] values;

    ConstantPool( DataInput data ) throws IOException {
        super();

        int count = data .readUnsignedShort();
        types = new byte [ count ];
        values = new Object [ count ];
        // read in all constant pool entries.
        for ( int i = 1; i < count; i++ ) {
            byte type = data .readByte();
            types[i] = type;
            switch (type)
            {
            case UTF8 :
                values[i] = data .readUTF();
                break;
                                
            case UNUSED :
                break;
                                
            case INTEGER :
                values[i] = new Integer( data .readInt() );
                break;
                                
            case FLOAT :
                values[i] = new Float( data .readFloat() );
                break;
                                
            case LONG :
                values[i] = new Long( data .readLong() );
                ++i;
                break;
                                
            case DOUBLE :
                values[i] = new Double( data .readDouble() );
                ++i;
                break;
                                
            case CLASS :
            case STRING :
                values[i] = new Integer( data .readUnsignedShort() );
                break;
                                
            case FIELDREF :
            case METHODREF :
            case INTERFACEMETHODREF :
            case NAMEANDTYPE :
                values[i] = new Integer( data .readInt() );
                break;
            }
        }
    }


}
/**
 * Provides a quick and dirty way to determine the true name of a class
 * given just an InputStream. Reads in just enough to perform this
 * minimal task only.
 */
public class ClassNameReader extends Object{

    public static 
        String getClassName( InputStream input ) throws IOException {
        DataInputStream data = new DataInputStream( input );
        // verify this is a valid class file.
        int cookie = data .readInt();
        if ( cookie != 0xCAFEBABE ) {
            return null;
        }
        int version = data .readInt();
        // read the constant pool.
        ConstantPool constants = new ConstantPool( data );
        Object[] values = constants .values;
        // read access flags and class index.
        int accessFlags = data .readUnsignedShort();
        int classIndex = data .readUnsignedShort();
        Integer stringIndex = (Integer) values[classIndex];
        String className = (String) values[stringIndex .intValue()];
        return className;
    }


}


