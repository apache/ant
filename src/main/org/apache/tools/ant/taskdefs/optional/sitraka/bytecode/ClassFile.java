/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.sitraka.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ClassCPInfo;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPool;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.Utf8CPInfo;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.attributes.AttributeInfo;


/**
 * Object representing a class.
 *
 * Information are kept to the strict minimum for JProbe reports so
 * that not too many objects are created for a class, otherwise the
 * JVM can quickly run out of memory when analyzing a great deal of
 * classes and keeping them in memory for global analysis.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public final class ClassFile {

    private MethodInfo[] methods;

    private String sourceFile;

    private String fullname;

    private int access_flags;

    public ClassFile(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        ConstantPool constantPool = new ConstantPool();

        int magic = dis.readInt(); // 0xCAFEBABE
        int minor = dis.readShort();
        int major = dis.readShort();

        constantPool.read(dis);
        constantPool.resolve();

        // class information
        access_flags = dis.readShort();
        int this_class = dis.readShort();
        fullname = ((ClassCPInfo) constantPool.getEntry(this_class)).getClassName().replace('/', '.');
        int super_class = dis.readShort();

        // skip interfaces...
        int count = dis.readShort();
        dis.skipBytes(count * 2); // short

        // skip fields...
        int numFields = dis.readShort();
        for (int i = 0; i < numFields; i++) {
            // 3 short: access flags, name index, descriptor index
            dis.skip(2 * 3);
            // attribute list...
            int attributes_count = dis.readUnsignedShort();
            for (int j = 0; j < attributes_count; j++) {
                dis.skipBytes(2); // skip attr_id (short)
                int len = dis.readInt();
                dis.skipBytes(len);
            }
        }

        // read methods
        int method_count = dis.readShort();
        methods = new MethodInfo[method_count];
        for (int i = 0; i < method_count; i++) {
            methods[i] = new MethodInfo();
            methods[i].read(constantPool, dis);
        }

        // get interesting attributes.
        int attributes_count = dis.readUnsignedShort();
        for (int j = 0; j < attributes_count; j++) {
            int attr_id = dis.readShort();
            int len = dis.readInt();
            String attr_name = Utils.getUTF8Value(constantPool, attr_id);
            if (AttributeInfo.SOURCE_FILE.equals(attr_name)) {
                int name_index = dis.readShort();
                sourceFile = ((Utf8CPInfo) constantPool.getEntry(name_index)).getValue();
            } else {
                dis.skipBytes(len);
            }
        }
    }

    public int getAccess() {
        return access_flags;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public MethodInfo[] getMethods() {
        return methods;
    }

    public String getFullName() {
        return fullname;
    }

    public String getName() {
        String name = getFullName();
        int pos = name.lastIndexOf('.');
        if (pos == -1) {
            return "";
        }
        return name.substring(pos + 1);
    }

    public String getPackage() {
        String name = getFullName();
        int pos = name.lastIndexOf('.');
        if (pos == -1) {
            return "";
        }
        return name.substring(0, pos);
    }

}





