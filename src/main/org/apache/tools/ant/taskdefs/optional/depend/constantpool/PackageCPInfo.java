package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Represents the package info (within a module) constant pool entry
 */
public class PackageCPInfo extends ConstantCPInfo {

    private int packageNameIndex;
    private String packageName;

    public PackageCPInfo() {
        super(CONSTANT_PACKAGEINFO, 1);
    }

    @Override
    public void read(final DataInputStream cpStream) throws IOException {
        this.packageNameIndex = cpStream.readUnsignedShort();
    }

    @Override
    public void resolve(final ConstantPool constantPool) {
        this.packageName = ((Utf8CPInfo) constantPool.getEntry(this.packageNameIndex)).getValue();

        super.resolve(constantPool);
    }

    @Override
    public String toString() {
        return "Package info Constant Pool Entry for " + this.packageName + "[" + this.packageNameIndex + "]";
    }
}
