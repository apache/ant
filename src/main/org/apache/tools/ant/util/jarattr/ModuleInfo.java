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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.util.List;
import java.util.ArrayList;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * Represents, reads, and writes all data in a {@code module-info.class}.
 * Reading and writing of {@code module-info.class} is based on
 * <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html">the
 * Java Virtual Machine Specification for Java 11</a>.
 * <p>
 * As per the JVM specification, a constant pool index is always 1-based:
 * index 1 is the first constant, 2 is the second, etc.
 */
public final class ModuleInfo {
    /** Magic number signature bytes. */
    private int magic;
    /** Major version and minor version of class file. */
    private int version;

    /** Constants defined in class file. */
    private List<Constant> constantPool = List.of();

    /** Class modifiers. */
    private short accessFlags;

    /**
     * Index in constant pool of {@code CONSTANT_class} for this file's class.
     */
    private int thisClass;      // 2-byte constant pool index

    /**
     * Index in constant pool of {@code CONSTANT_class} for the superclass
     * of this file's class.
     */
    private int superClass;     // 2-byte constant pool index

    /**
     * Class attributes, which in the case of module-info is module attributes.
     */
    private List<Attribute> attributes = List.of();

    /**
     * Creates instance with no fields initialized.
     */
    private ModuleInfo() {
        // Deliberately empty.
    }

    /**
     * Returns a string suitable for debugging, which includes module
     * version and main class.
     *
     * @return debugging string
     */
    @Override
    public String toString() {
        return getClass().getName() + "["
            + "version=" + getVersion()
            + ", main class=" + getMainClass()
            + "]";
    }

    /**
     * Returns the String value of the {@code CONSTANT_Utf8} at the given
     * index in the constant pool, or {@code null} if the index in zero.
     *
     * @param index 1-based index in constant pool
     *
     * @return string constant at given index, or {@code null}
     */
    private String getUTF8Constant(int index) {
        if (index > 0) {
            Constant c = constantPool.get(index - 1);
            return ((UTF8Constant) c).value();
        } else {
            return null;
        }
    }

    /**
     * Returns the String name of the {@code CONSTANT_Class} at the given
     * index in the constant pool, or {@code null} if the index in zero.
     *
     * @param index 1-based index in constant pool
     *
     * @return name of class constant at given index, or {@code null}
     */
    private String getClassConstant(int index) {
        if (index > 0) {
            Constant c = constantPool.get(index - 1);
            int classNameIndex = ((IndexedConstant) c).index();
            return getUTF8Constant(classNameIndex);
        } else {
            return null;
        }
    }

    /**
     * Returns the currently defined version in this module-info.
     *
     * @return current version as a string, or {@code null} if no version
     *         is defined
     *
     * @see #setVersion(String)
     */
    public String getVersion() {
        for (Attribute a : attributes) {
            if (a instanceof ModuleAttribute) {
                ModuleAttribute m = (ModuleAttribute) a;
                return getUTF8Constant(m.getModuleVersionIndex());
            }
        }
        return null;
    }

    /**
     * Updates the version defined in this module-info.
     *
     * @param version new version to encode into module-info
     *
     * @see #getVersion()
     */
    public void setVersion(String version) {
        for (Attribute a : attributes) {
            if (a instanceof ModuleAttribute) {
                ModuleAttribute m = (ModuleAttribute) a;
                if (version != null) {
                    m.setModuleVersionIndex(getOrAddUTF8Constant(version));
                } else {
                    m.setModuleVersionIndex(0);
                }
                return;
            }
        }
        throw new IllegalStateException(
            "Class file has no '" + ModuleAttribute.NAME + "' attribute");
    }

    /**
     * Returns this module-info's currently defined main class entry point.
     *
     * @return name of main class, or {@code null} if none is defined
     *
     * @see #setMainClass(String)
     */
    public String getMainClass() {
        for (Attribute a : attributes) {
            if (a instanceof MainClassAttribute) {
                MainClassAttribute m = (MainClassAttribute) a;
                return getClassConstant(m.getClassIndex());
            }
        }
        return null;
    }

    /**
     * Updates this module-info's main class entry point.  The class name
     * must be a class which exists in the module, or the Java runtime will
     * refuse to load the module.
     *
     * @param className new main class entry point
     *
     * @see #getMainClass()
     */
    public void setMainClass(String className) {
        if (className == null) {
            attributes.removeIf(MainClassAttribute.class::isInstance);
            return;
        }

        // Internally, class names use slashes in place of periods.
        // See section 4.2.1 "Binary Class and Interface Names":
        // https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.2.1
        className = className.replace('.', '/');

        int mainClassNameIndex = getOrAddClassConstant(className);

        for (Attribute a : attributes) {
            if (a instanceof MainClassAttribute) {
                MainClassAttribute main = (MainClassAttribute) a;
                main.setClassIndex(mainClassNameIndex);
                return;
            }
        }

        int nameIndex = getOrAddUTF8Constant(MainClassAttribute.NAME);
        attributes.add(new MainClassAttribute(nameIndex, mainClassNameIndex));
    }

    /**
     * Locates the {@code CONSTANT_Utf8} in this module-info's constant pool
     * with the given string value.  If none exists, it is created and added
     * to the constant pool.
     *
     * @param value constant value to check for or add
     *
     * @return index in constant pool of specified value
     */
    private int getOrAddUTF8Constant(String value) {
        int index = 0;
        int constantCount = constantPool.size();
        for (int i = 0; i < constantCount; i++) {
            Constant c = constantPool.get(i);
            if (c instanceof UTF8Constant
                && value.equals(((UTF8Constant) c).value())) {

                index = i + 1;
                break;
            }
        }

        if (index == 0) {
            constantPool.add(new UTF8Constant(value));
            index = constantPool.size();
        }

        return index;
    }

    /**
     * Locates the {@code CONSTANT_Class} in this module-info's constant pool
     * which has a class name matching the argument.  Note that binary class
     * names use "{@code /}" as a separator rather than "{@code .}".
     * If no match class constant exists, it is created and added
     * to the constant pool.
     *
     * @param value constant value to check for or add
     *
     * @return index in constant pool of specified value
     */
    private int getOrAddClassConstant(String className) {
        int classNameIndex = getOrAddUTF8Constant(className);

        int index = 0;
        int constantCount = constantPool.size();
        for (int i = 0; i < constantCount; i++) {
            Constant c = constantPool.get(i);
            if (c instanceof IndexedConstant) {
                IndexedConstant ic = (IndexedConstant) c;
                if (ic.tag() == Constant.CLASS
                    && ic.index() == classNameIndex) {

                    index = i + 1;
                    break;
                }
            }
        }

        if (index == 0) {
            constantPool.add(
                new IndexedConstant(Constant.CLASS, classNameIndex));
            index = constantPool.size();
        }

        return index;
    }

    /**
     * Reads from a streamed .jar file's {@code module-info.class}.
     *
     * @param source .jar file containing module-info.class to read
     *
     * @return object representing .jar file's {@code module-info.class} data
     *
     * @throws IOException if stream cannot be read or does not contain
     *                     {@code module-info.class}
     */
    public static ModuleInfo readFrom(JarInputStream source)
    throws IOException {
        JarEntry entry;
        while ((entry = source.getNextJarEntry()) != null) {
            if ("module-info.class".equals(entry.getName())) {
                return readFrom(new DataInputStream(source));
            }
            source.closeEntry();
        }

        throw new IOException("No module-info.class found");
    }

    /**
     * Reads from a jar file's {@code module-info.class}, taking any
     * multi-release configuration into account.
     *
     * @param source .jar file containing module-info.class to read
     *
     * @return object representing .jar file's {@code module-info.class} data
     *
     * @throws IOException if .jar file cannot be read or does not contain
     *                     {@code module-info.class}
     */
    public static ModuleInfo readFrom(JarFile source)
    throws IOException {

        JarEntry moduleInfoClass = source.getJarEntry("module-info.class");

        if (moduleInfoClass == null) {
            throw new IOException("No module-info.class found");
        }

        try (DataInputStream stream = new DataInputStream(
            new BufferedInputStream(
                source.getInputStream(moduleInfoClass)))) {

            return readFrom(stream);
        }
    }

    /**
     * Reads from {@code module-info.class}.
     *
     * @param source content of {@code module-info.class}
     *
     * @return object representing .jar file's {@code module-info.class} data
     *
     * @throws IOException if .jar file cannot be read or does not contain
     *                     {@code module-info.class}
     */
    public static ModuleInfo readFrom(DataInputStream source)
    throws IOException {
        ModuleInfo info = new ModuleInfo();

        info.magic = source.readInt();
        info.version = source.readInt();    // major_version, minor_version

        int constantPoolCount = source.readUnsignedShort();
        info.constantPool = new ArrayList<>(constantPoolCount);
        for (int i = 1; i < constantPoolCount; i++) {
            info.constantPool.add(Constant.readFrom(source));
        }

        info.accessFlags = source.readShort();
        info.thisClass = source.readUnsignedShort();
        info.superClass = source.readUnsignedShort();

        int interfacesCount = source.readUnsignedShort();
        if (interfacesCount != 0) {
            throw new ClassFormatException(
                "interfaces_count in class file should be zero"
                + ", but is " + interfacesCount);
        }

        int fieldsCount = source.readUnsignedShort();
        if (fieldsCount != 0) {
            throw new ClassFormatException(
                "fields_count in class file should be zero"
                + ", but is " + fieldsCount);
        }

        int methodsCount = source.readUnsignedShort();
        if (methodsCount != 0) {
            throw new ClassFormatException(
                "methods_count in class file should be zero"
                + ", but is " + methodsCount);
        }

        int attributesCount = source.readUnsignedShort();
        if (attributesCount < 1) {
            throw new ClassFormatException(
                "attributes_count in class file should be positive"
                + ", but is " + attributesCount);
        }

        info.attributes = new ArrayList<>(attributesCount);
        for (int i = 0; i < attributesCount; i++) {
            info.attributes.add(Attribute.readFrom(source, info.constantPool));
        }

        return info;
    }

    /**
     * Writes a new {@code module-info.class}.
     *
     * @param dest destination to write {@code module-info.class} to
     *
     * @throws IOException if stream cannot be written
     */
    public void writeTo(DataOutputStream dest)
    throws IOException {
        dest.writeInt(magic);
        dest.writeInt(version);

        dest.writeShort(constantPool.size() + 1);
        for (Constant c : constantPool) {
            c.writeTo(dest);
        }

        dest.writeShort(accessFlags);
        dest.writeShort(thisClass);
        dest.writeShort(superClass);

        dest.writeShort(0);     // interfaces_count
        dest.writeShort(0);     // fields_count
        dest.writeShort(0);     // methods_count

        dest.writeShort(attributes.size());
        for (Attribute a : attributes) {
            a.writeTo(dest);
        }
    }

    /**
     * Writes a new {@code module-info.class} in the specified .jar file.
     *
     * @param jar .jar file to update
     *
     * @throws IOException if .jar file cannot be read, or new file cannot be
     *                     written
     */
    public void writeInto(Path jar)
    throws IOException {
        Path newJar = Files.createTempFile(null, ".jar");

        try (JarInputStream in = new JarInputStream(
                new BufferedInputStream(
                    Files.newInputStream(jar)));
             JarOutputStream out = new JarOutputStream(
                new BufferedOutputStream(
                    Files.newOutputStream(newJar)))) {

            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                JarEntry newEntry = (JarEntry) entry.clone();

                out.putNextEntry(newEntry);

                String name = entry.getName();
                if (name.equals("module-info.class")
                    || name.endsWith("/module-info.class")) {

                    DataOutputStream d = new DataOutputStream(out);
                    writeTo(d);
                    d.flush();
                } else {
                    in.transferTo(out);
                }

                out.closeEntry();
                in.closeEntry();
            }
        }

        Files.move(newJar, jar, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Tests this class by treating each argument as a file name,
     * displaying the module-info for each such file, along with its
     * class file attributes.
     *
     * @param args command line arguments
     *
     * @throws IOException if an file cannot be read
     */
    public static void main(String[] args)
    throws IOException {
        for (String arg : args) {
            try (JarInputStream stream = new JarInputStream(
                new BufferedInputStream(
                    Files.newInputStream(
                        Paths.get(arg))))) {
                ModuleInfo info = readFrom(stream);
                System.out.println(arg + ": " + info);

                System.out.println("Attributes:");
                for (Attribute a : info.attributes) {
                    System.out.println("  "
                        + info.getUTF8Constant(a.attributeNameIndex()));
                }
            }
        }
    }
}
