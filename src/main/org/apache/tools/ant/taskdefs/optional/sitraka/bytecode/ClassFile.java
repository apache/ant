/*
 * Copyright  2001-2002,2004-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
 */
public final class ClassFile {

    private MethodInfo[] methods;

    private String sourceFile;

    private String fullname;

    private int access_flags;

    /**
     * Constructor for ClassFile.
     * @param is the input stream containing the class.
     * @throws IOException on error
     */
    public ClassFile(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        ConstantPool constantPool = new ConstantPool();

        /* int magic = */ dis.readInt(); // 0xCAFEBABE
        /* int minor = */ dis.readShort();
        /* int major = */ dis.readShort();

        constantPool.read(dis);
        constantPool.resolve();

        // class information
        access_flags = dis.readShort();
        int thisClass = dis.readShort();
        fullname = ((ClassCPInfo) constantPool.getEntry(
                        thisClass)).getClassName().replace('/', '.');
        /* int super_class = */ dis.readShort();

        // skip interfaces...
        int count = dis.readShort();
        dis.skipBytes(count * 2); // short

        // skip fields...
        int numFields = dis.readShort();
        for (int i = 0; i < numFields; i++) {
            // 3 short: access flags, name index, descriptor index
            dis.skip(2 * 3);
            // attribute list...
            int attributesCount = dis.readUnsignedShort();
            for (int j = 0; j < attributesCount; j++) {
                dis.skipBytes(2); // skip attr_id (short)
                int len = dis.readInt();
                dis.skipBytes(len);
            }
        }

        // read methods
        int methodCount = dis.readShort();
        methods = new MethodInfo[methodCount];
        for (int i = 0; i < methodCount; i++) {
            methods[i] = new MethodInfo();
            methods[i].read(constantPool, dis);
        }

        // get interesting attributes.
        int attributesCount = dis.readUnsignedShort();
        for (int j = 0; j < attributesCount; j++) {
            int attrId = dis.readShort();
            int len = dis.readInt();
            String attrName = Utils.getUTF8Value(constantPool, attrId);
            if (AttributeInfo.SOURCE_FILE.equals(attrName)) {
                int nameIndex = dis.readShort();
                sourceFile = ((Utf8CPInfo) constantPool.getEntry(nameIndex)).getValue();
            } else {
                dis.skipBytes(len);
            }
        }
    }

    /**
     * Get the access flags of the class.
     * @return the flags
     */
    public int getAccess() {
        return access_flags;
    }

    /**
     * Get the source filename
     * @return the source filename
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * Get the methods of the class.
     * @return the methods
     */
    public MethodInfo[] getMethods() {
        return methods;
    }

    /**
     * Get the full name of the class.
     * @return the full name
     */
    public String getFullName() {
        return fullname;
    }

    /**
     * Get the name of the class (minus package name)
     * @return the name
     */
    public String getName() {
        String name = getFullName();
        int pos = name.lastIndexOf('.');
        if (pos == -1) {
            return "";
        }
        return name.substring(pos + 1);
    }

    /**
     * Get the package name of the class.
     * @return the package name
     */
    public String getPackage() {
        String name = getFullName();
        int pos = name.lastIndexOf('.');
        if (pos == -1) {
            return "";
        }
        return name.substring(0, pos);
    }

}
