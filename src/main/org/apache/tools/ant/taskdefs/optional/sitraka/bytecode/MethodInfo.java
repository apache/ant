/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
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
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPool;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.attributes.AttributeInfo;

/**
 * Method info structure.
 * @todo give a more appropriate name to methods.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public final class MethodInfo {
    private int access_flags;
    private int loc = -1;
    private String name;
    private String descriptor;

    public MethodInfo() {
    }

    public void read(ConstantPool constantPool, DataInputStream dis) throws IOException {
        access_flags = dis.readShort();

        int name_index = dis.readShort();
        name = Utils.getUTF8Value(constantPool, name_index);

        int descriptor_index = dis.readShort();
        descriptor = Utils.getUTF8Value(constantPool, descriptor_index);

        int attributes_count = dis.readUnsignedShort();
        for (int i = 0; i < attributes_count; i++) {
            int attr_id = dis.readShort();
            String attr_name = Utils.getUTF8Value(constantPool, attr_id);
            int len = dis.readInt();
            if (AttributeInfo.CODE.equals(attr_name)) {
                readCode(constantPool, dis);
            } else {
                dis.skipBytes(len);
            }
        }

    }

    protected void readCode(ConstantPool constantPool, DataInputStream dis) throws IOException {
        // skip max_stack (short), max_local (short)
        dis.skipBytes(2 * 2);

        // skip bytecode...
        int bytecode_len = dis.readInt();
        dis.skip(bytecode_len);

        // skip exceptions... 1 exception = 4 short.
        int exception_count = dis.readShort();
        dis.skipBytes(exception_count * 4 * 2);

        // read attributes...
        int attributes_count = dis.readUnsignedShort();
        for (int i = 0; i < attributes_count; i++) {
            int attr_id = dis.readShort();
            String attr_name = Utils.getUTF8Value(constantPool, attr_id);
            int len = dis.readInt();
            if (AttributeInfo.LINE_NUMBER_TABLE.equals(attr_name)) {
                // we're only interested in lines of code...
                loc = dis.readShort();
                // skip the table which is 2*loc*short
                dis.skip(loc * 2 * 2);
            } else {
                dis.skipBytes(len);
            }
        }
    }

    public int getAccessFlags() {
        return access_flags;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getFullSignature() {
        return getReturnType() + " " + getShortSignature();
    }

    public String getShortSignature() {
        StringBuffer buf = new StringBuffer(getName());
        buf.append("(");
        String[] params = getParametersType();
        for (int i = 0; i < params.length; i++) {
            buf.append(params[i]);
            if (i != params.length - 1) {
                buf.append(", ");
            }
        }
        buf.append(")");
        return buf.toString();
    }

    public String getReturnType() {
        return Utils.getMethodReturnType(getDescriptor());
    }

    public String[] getParametersType() {
        return Utils.getMethodParams(getDescriptor());
    }

    public int getNumberOfLines() {
        return loc;
    }

    public String getAccess() {
        return Utils.getMethodAccess(access_flags);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Method: ").append(getAccess()).append(" ");
        sb.append(getFullSignature());
        return sb.toString();
    }
}


