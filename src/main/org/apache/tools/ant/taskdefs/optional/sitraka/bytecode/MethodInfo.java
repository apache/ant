/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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


