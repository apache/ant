/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.util.jarattr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Objects;

/**
 * Class file attribute which describes a module descriptor.
 */
class ModuleAttribute
extends Attribute {
    /** Official name of this attribute, as per JVM specification. */
    static final String NAME = "Module";

    /** Describes a required module. */
    static class Requires {
        /** Index in constant pool of {@code CONSTANT_Module}. */
        private int requiresIndex;        // 2 bytes

        /** Dependency modifiers (transitive, static, etc.) */
        private int requiresFlags;        // 2 bytes

        /**
         * Index in constant pool of {@code CONSTANT_Utf8} containing
         * required module's version at compile time.
         */
        private int requiresVersionIndex; // 2 bytes
    }

    /** Describes an exported package in a module. */
    static class Exports {
        /** Index in constant pool of {@code CONSTANT_Package}. */
        private int exportsIndex;

        /** Export modifiers. */
        private int exportsFlags;

        /**
         * Array of 16-bit constant pool indexes for {@code CONSTANT_Module}s
         * which are explicit targets of export.
         */
        private byte[] exportsToIndex;    // 2 bytes each
    }

    /** Describes an opened package in a module. */
    static class Opens {
        /** Index in constant pool of {@code CONSTANT_Package}. */
        private int opensIndex;

        /** Open modifiers. */
        private int opensFlags;

        /**
         * Array of 16-bit constant pool indexes for {@code CONSTANT_Module}s
         * which are explicit targets of open.
         */
        private byte[] opensToIndex;      // 2 bytes each
    }

    /**
     * Describes an implementation of a service provider interface
     * contained in a module.
     */
    static class Provides {
        /**
         * Index in constant pool of {@code CONSTANT_Class} representing
         * service type.
         */
        private int providesIndex;

        /**
         * Array of 16-bit constant pool indexes for {@code CONSTANT_Class}es
         * indicating implementations of service type.
         */
        private byte[] providesWithIndex; // 2 bytes each
    }

    /**
     * Index in constant pool of {@code CONSTANT_Module} containing module's
     * identifying information.
     */
    private final int moduleNameIndex;

    /** Module declaration modifiers. */
    private final int moduleFlags;

    /**
     * Index in constant pool of {@code CONSTANT_Utf8} containing module's
     * version, or zero if there is no version.
     */
    private int moduleVersionIndex;

    /** List of all required modules. */
    private final Requires[] requires;

    /** List of all exported packages. */
    private final Exports[] exports;

    /** List of all opened packages. */
    private final Opens[] opens;

    /**
     * Array of 16-bit constant pool indexes for {@code CONSTANT_Class}es
     * which are SPI classes used by module.
     */
    private final byte[] uses;                  // 2 bytes each

    /** List of all implementations of service provider interfaces. */
    private final Provides[] provides;

    /**
     * Creates an instance with the specified module information.
     *
     * @param nameIndex index in constant pool of attribute's name
     * @param length size in bytes of attribute data
     * @param moduleNameIndex index in constant pool of module's name
     * @param flags module modifiers
     * @param versionIndex index in constant pool of module's version, or zero
     * @param requires list of required modules
     * @param exports list of exported packages
     * @param opens list of opened packages
     * @param uses list of 16-bit constant pool indices of
     *             service provider interfaces needed by module
     * @param provides list of concrete implements of
     *                 service provider interfaces in module
     */
    ModuleAttribute(int nameIndex,
                    int length,
                    int moduleNameIndex,
                    int flags,
                    int versionIndex,
                    Requires[] requires,
                    Exports[] exports,
                    Opens[] opens,
                    byte[] uses,
                    Provides[] provides) {

        super(nameIndex, length);

        this.moduleNameIndex = moduleNameIndex;
        this.moduleFlags = flags;
        this.moduleVersionIndex = versionIndex;

        this.requires = Objects.requireNonNull(requires,
            "Requires list cannot be null");
        this.exports = Objects.requireNonNull(exports,
            "Exports list cannot be null");
        this.opens = Objects.requireNonNull(opens,
            "Opens list cannot be null");
        this.provides = Objects.requireNonNull(provides,
            "Provides list cannot be null");
        this.uses = Objects.requireNonNull(uses,
            "Uses cannot be null");
    }

    /**
     * Returns the index in the constant pool of the {@code CONSTANT_Utf8}
     * which holds the module's version string, or zero if there is no version.
     *
     * @return index in constant pool of version string, or zero
     */
    int getModuleVersionIndex() {
        return moduleVersionIndex;
    }

    /**
     * Sets the index in the constant pool of the {@code CONSTANT_Utf8}
     * which holds the module's version string.
     *
     * @param index index in constant pool of Utf8 constant containing version
     *              string, or zero to signify there is no version defined
     */
    void setModuleVersionIndex(int index) {
        this.moduleVersionIndex = index;
    }

    /**
     * Reads a module attribute from an {@code attribute_info} block
     * in a {@code module-info.class}.
     *
     * @param in {@code module-info.class} stream, pointing to attribute_info
     *           block containing module attribute data
     * @param attributeNameIndex already-read constant pool index of attribute
     *           name (which should always be the value of {@link #NAME})
     * @param attributeLength size of attribute data in bytes
     *
     * @return new {@code ModuleAttribute} object representing attribute_info
     *         data
     *
     * @throws IOException if stream cannot be read, or contains invalid data
     */
    static ModuleAttribute readFrom(DataInputStream in,
                                    int attributeNameIndex,
                                    int attributeLength)
    throws IOException {
        int moduleNameIndex = in.readUnsignedShort();
        int moduleFlags = in.readUnsignedShort();
        int moduleVersionIndex = in.readUnsignedShort();

        int requiresCount = in.readUnsignedShort();
        Requires[] requires = new Requires[requiresCount];
        for (int i = 0; i < requiresCount; i++) {
            requires[i] = new Requires();
            requires[i].requiresIndex = in.readUnsignedShort();
            requires[i].requiresFlags = in.readShort();
            requires[i].requiresVersionIndex = in.readUnsignedShort();
        }

        int exportsCount = in.readUnsignedShort();
        Exports[] exports = new Exports[exportsCount];
        for (int i = 0; i < exportsCount; i++) {
            exports[i] = new Exports();
            exports[i].exportsIndex = in.readUnsignedShort();
            exports[i].exportsFlags = in.readShort();
            int exportsToCount = in.readUnsignedShort();
            exports[i].exportsToIndex = readNBytes(in, exportsToCount * 2);
        }

        int opensCount = in.readUnsignedShort();
        Opens[] opens = new Opens[opensCount];
        for (int i = 0; i < opensCount; i++) {
            opens[i] = new Opens();
            opens[i].opensIndex = in.readUnsignedShort();
            opens[i].opensFlags = in.readShort();
            int opensToCount = in.readUnsignedShort();
            opens[i].opensToIndex = readNBytes(in, opensToCount * 2);
        }

        int usesCount = in.readUnsignedShort();
        byte[] uses = readNBytes(in, usesCount * 2);

        int providesCount = in.readUnsignedShort();
        Provides[] provides = new Provides[providesCount];
        for (int i = 0; i < providesCount; i++) {
            provides[i] = new Provides();
            provides[i].providesIndex = in.readUnsignedShort();
            int providesWithCount = in.readUnsignedShort();
            provides[i].providesWithIndex =
                readNBytes(in, providesWithCount * 2);
        }

        return new ModuleAttribute(
            attributeNameIndex,
            attributeLength,
            moduleNameIndex,
            moduleFlags,
            moduleVersionIndex,
            requires,
            exports,
            opens,
            uses,
            provides);
    }

    @Override
    void writeTo(DataOutputStream out)
    throws IOException {
        out.writeShort(attributeNameIndex());
        out.writeInt(attributeLength());

        out.writeShort(moduleNameIndex);
        out.writeShort(moduleFlags);
        out.writeShort(moduleVersionIndex);

        out.writeShort(requires.length);
        for (Requires r : requires) {
            out.writeShort(r.requiresIndex);
            out.writeShort(r.requiresFlags);
            out.writeShort(r.requiresVersionIndex);
        }

        out.writeShort(exports.length);
        for (Exports e : exports) {
            out.writeShort(e.exportsIndex);
            out.writeShort(e.exportsFlags);
            out.writeShort(e.exportsToIndex.length / 2);
            out.write(e.exportsToIndex);
        }

        out.writeShort(opens.length);
        for (Opens o : opens) {
            out.writeShort(o.opensIndex);
            out.writeShort(o.opensFlags);
            out.writeShort(o.opensToIndex.length / 2);
            out.write(o.opensToIndex);
        }

        out.writeShort(uses.length / 2);
        out.write(uses);

        out.writeShort(provides.length);
        for (Provides p : provides) {
            out.writeShort(p.providesIndex);
            out.writeShort(p.providesWithIndex.length / 2);
            out.write(p.providesWithIndex);
        }
    }
}
