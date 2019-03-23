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

import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Files;

import java.util.Objects;
import java.util.jar.JarFile;

/**
 * Updates a .jar file's modular attributes.
 */
public class JarAttributeUpdater {

    /**
     * Extracts {@code module-info.class} from a .jar file.
     *
     * @param jar jar file to read
     *
     * @return representation of .jar file's {@code module-info.class}
     *
     * @throws IOException if file cannot be read, is not a valid .jar file,
     *                     or lacks a {@code module-info.class}
     */
    private static ModuleInfo readModuleInfo(Path jar)
        throws IOException {

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            return ModuleInfo.readFrom(jarFile);
        }
    }

    /**
     * Updates the module version in {@code module-info.class} data.
     *
     * @param moduleInfo data to update
     * @param version new module version
     *
     * @return {@code true} if module's version was missing or different
     *         and was modified;  {@code false} if it was already the same
     *         as the {@code version} argument and thus no change was made
     */
    public boolean setVersion(ModuleInfo moduleInfo,
                              String version) {

        String oldVersion = moduleInfo.getVersion();
        if (Objects.equals(version, oldVersion)) {
            return false;
        }

        moduleInfo.setVersion(version);
        return true;
    }

    /**
     * Updates the module version in a .jar file's {@code module-info.class}.
     *
     * @param jar .jar file to update
     * @param version new module version
     *
     * @return {@code true} if module's version was missing or different
     *         and was modified;  {@code false} if it was already the same
     *         as the {@code version} argument and thus no change was made
     *
     * @throws IOException if file could not be read or written, or file
     *                     is not a valid .jar file, or file lacks a
     *                     {@code module-info.class}
     */
    public boolean setVersion(Path jar,
                              String version)
        throws IOException {

        ModuleInfo moduleInfo = readModuleInfo(jar);

        if (setVersion(moduleInfo, version)) {
            moduleInfo.writeInto(jar);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the module main class entry point in
     * {@code module-info.class} data.
     *
     * @param moduleInfo data to update
     * @param mainClass fully qualified name of class that will be the
     *                  new module main class entry point
     *
     * @return {@code true} if module's main class was missing or different
     *         and was modified;  {@code false} if it was already the same
     *         as the {@code mainClass} argument and thus no change was made
     */
    public boolean setMainClass(ModuleInfo moduleInfo,
                                String mainClass) {

        String oldMain = moduleInfo.getMainClass();
        if (Objects.equals(mainClass, oldMain)) {
            return false;
        }

        moduleInfo.setMainClass(mainClass);
        return true;
    }

    /**
     * Updates the module main class entry point in a .jar file's
     * {@code module-info.class}.
     *
     * @param jar .jar file to update
     * @param mainClass fully qualified name of class that will be the
     *                  new module main class entry point
     *
     * @return {@code true} if module's main class was missing or different
     *         and was modified;  {@code false} if it was already the same
     *         as the {@code mainClass} argument and thus no change was made
     *
     * @throws IOException if file could not be read or written, or file
     *                     is not a valid .jar file, or file lacks a
     *                     {@code module-info.class}
     */
    public boolean setMainClass(Path jar,
                                String mainClass)
        throws IOException {

        ModuleInfo moduleInfo = readModuleInfo(jar);

        if (setMainClass(moduleInfo, mainClass)) {
            moduleInfo.writeInto(jar);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the module version and main class entry point in a .jar file's
     * {@code module-info.class}.
     *
     * @param jar .jar file to update
     * @param version new module version
     * @param mainClass fully qualified name of class that will be the
     *                  new module main class entry point
     *
     * @return {@code true} if module's version and/or main class were
     *         modified;  {@code false} if they were already the same
     *         as the arguments and thus no change was made
     *
     * @throws IOException if file could not be read or written, or file
     *                     is not a valid .jar file, or file lacks a
     *                     {@code module-info.class}
     */
    public boolean updateJar(Path jar,
                             String version,
                             String mainClass)
        throws IOException {

        ModuleInfo moduleInfo = readModuleInfo(jar);

        boolean versionChanged =
            version != null && setVersion(moduleInfo, version);
        boolean mainClassChanged =
            mainClass != null && setMainClass(moduleInfo, mainClass);

        if (versionChanged || mainClassChanged) {
            moduleInfo.writeInto(jar);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Carries out the entire module-info update operation.
     * <p>
     * This is meant to be called reflectively from the &lt;jar&gt; task,
     * in order to avoid a compile-time dependency on Java 9+ code.
     * Therefore, if this method's signature is changed, make sure the
     * reflective call in the taskdefs.Jar class is updated to match it.
     *
     * @param originalModuleInfo source whose version and/or main class
`    *                           attributes will be modified
     * @param versionStr new module version, or {@code null}
     * @param mainClass fully qualfied name new main class of module,
     *                  or {@code null}
     * @param newModuleInfoLocation file to which new module-info
     *                              will be written
     *
     * @throws IOException if original module-info cannot be read, or
     *                     new module-info file cannot be written
     * @throws RuntimeException if {@code originalModuleInfo} or
     *                          {@code newModuleInfoLocation} is {@code null}
     */
    public static void writeNewModuleInfo(InputStream originalModuleInfo,
                                          String versionStr,
                                          String mainClass,
                                          Path newModuleInfoLocation)
        throws IOException {

        ModuleInfo moduleInfo;
        try (DataInputStream original =
            new DataInputStream(originalModuleInfo)) {

            moduleInfo = ModuleInfo.readFrom(original);
        }

        JarAttributeUpdater updater = new JarAttributeUpdater();
        updater.setVersion(moduleInfo, versionStr);
        updater.setMainClass(moduleInfo, mainClass);

        try (DataOutputStream infoStream = new DataOutputStream(
            new BufferedOutputStream(
                Files.newOutputStream(newModuleInfoLocation)))) {

            moduleInfo.writeTo(infoStream);
        }
    }
}
