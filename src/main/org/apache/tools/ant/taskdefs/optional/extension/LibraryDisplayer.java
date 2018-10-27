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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import java.text.ParseException;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;

/**
 * Utility class to output the information in a jar relating
 * to "Optional Packages" (formely known as "Extensions")
 * and Package Specifications.
 *
 */
class LibraryDisplayer {
    /**
     * Display the extensions and specifications contained
     * within specified file.
     *
     * @param file the file
     * @throws BuildException if fail to read file
     */
    void displayLibrary(final File file)
        throws BuildException {
        final Manifest manifest = ExtensionUtil.getManifest(file);
        displayLibrary(file, manifest);
    }

    /**
     * Display the extensions and specifications contained
     * within specified file.
     *
     * @param file the file to use while reporting
     * @param manifest the manifest of file
     * @throws BuildException if fail to read file
     */
    void displayLibrary(final File file,
                         final Manifest manifest)
        throws BuildException {
        final Extension[] available = Extension.getAvailable(manifest);
        final Extension[] required = Extension.getRequired(manifest);
        final Extension[] options = Extension.getOptions(manifest);
        final Specification[] specifications = getSpecifications(manifest);

        if (0 == available.length && 0 == required.length && 0 == options.length
            && 0 == specifications.length) {
            return;
        }

        final String message = "File: " + file;
        final int size = message.length();
        printLine(size);
        System.out.println(message);
        printLine(size);
        if (0 != available.length) {
            System.out.println("Extensions Supported By Library:");
            for (int i = 0; i < available.length; i++) {
                System.out.println(available[i]);
            }
        }

        if (0 != required.length) {
            System.out.println("Extensions Required By Library:");
            for (int i = 0; i < required.length; i++) {
                System.out.println(required[i]);
            }
        }

        if (0 != options.length) {
            System.out.println("Extensions that will be used by Library if present:");
            for (int i = 0; i < options.length; i++) {
                System.out.println(options[i]);
            }
        }

        if (0 != specifications.length) {
            System.out.println("Specifications Supported By Library:");
            for (int i = 0; i < specifications.length; i++) {
                displaySpecification(specifications[i]);
            }
        }
    }

    /**
     * Print out a line of '-'s equal to specified size.
     *
     * @param size the number of dashes to printout
     */
    private void printLine(final int size) {
        for (int i = 0; i < size; i++) {
            System.out.print("-");
        }
        System.out.println();
    }

    /**
     * Get specifications from manifest.
     *
     * @param manifest the manifest
     * @return the specifications or null if none
     * @throws BuildException if malformed specification sections
     */
    private Specification[] getSpecifications(final Manifest manifest)
        throws BuildException {
        try {
            return Specification.getSpecifications(manifest);
        } catch (final ParseException pe) {
            throw new BuildException(pe.getMessage(), pe);
        }
    }

    /**
     * Print out specification details.
     *
     * @param specification the specification
     */
    private void displaySpecification(final Specification specification) {
        final String[] sections = specification.getSections();
        if (null != sections) {
            System.out.print("Sections:  ");
            System.out
                .println(Stream.of(sections).collect(Collectors.joining(" ")));
        }
        System.out.println(specification.toString());
    }
}
