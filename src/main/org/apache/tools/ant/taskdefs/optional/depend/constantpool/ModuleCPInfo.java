package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Represents the module info constant pool entry
 */
public class ModuleCPInfo extends ConstantCPInfo {

    private int moduleNameIndex;
    private String moduleName;

    public ModuleCPInfo() {
        super(CONSTANT_MODULEINFO, 1);
    }

    @Override
    public void read(final DataInputStream cpStream) throws IOException {
        this.moduleNameIndex = cpStream.readUnsignedShort();
    }

    @Override
    public void resolve(final ConstantPool constantPool) {
        this.moduleName = ((Utf8CPInfo) constantPool.getEntry(this.moduleNameIndex)).getValue();

        super.resolve(constantPool);
    }

    @Override
    public String toString() {
        return "Module info Constant Pool Entry for " + this.moduleName + "[" + this.moduleNameIndex + "]";
    }
}
