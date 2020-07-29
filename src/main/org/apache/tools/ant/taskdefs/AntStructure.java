/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Reference;

/**
 * Creates a partial DTD for Ant from the currently known tasks.
 *
 *
 * @since Ant 1.1
 *
 * @ant.task category="xml"
 */
public class AntStructure extends Task {

    private File output;
    private StructurePrinter printer = new DTDPrinter();

    /**
     * The output file.
     * @param output the output file
     */
    public void setOutput(final File output) {
        this.output = output;
    }

    /**
     * The StructurePrinter to use.
     * @param p the printer to use.
     * @since Ant 1.7
     */
    public void add(final StructurePrinter p) {
        printer = p;
    }

    /**
     * Build the antstructure DTD.
     *
     * @exception BuildException if the DTD cannot be written.
     */
    @Override
    public void execute() throws BuildException {

        if (output == null) {
            throw new BuildException("output attribute is required", getLocation());
        }

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                Files.newOutputStream(output.toPath()), StandardCharsets.UTF_8))) {

            printer.printHead(out, getProject(),
                    new Hashtable<>(getProject().getTaskDefinitions()),
                    new Hashtable<>(getProject().getDataTypeDefinitions()));

            printer.printTargetDecl(out);

            for (final String typeName : getProject().getCopyOfDataTypeDefinitions().keySet()) {
                printer.printElementDecl(out, getProject(), typeName,
                        getProject().getDataTypeDefinitions().get(typeName));
            }

            for (final String tName : getProject().getCopyOfTaskDefinitions().keySet()) {
                printer.printElementDecl(out, getProject(), tName,
                        getProject().getTaskDefinitions().get(tName));
            }

            printer.printTail(out);

            if (out.checkError()) {
                throw new IOException("Encountered an error writing Ant structure");
            }
        } catch (final IOException ioe) {
            throw new BuildException("Error writing "
                    + output.getAbsolutePath(), ioe, getLocation());
        }
    }

    /**
     * Writes the actual structure information.
     *
     * <p>{@link #printHead}, {@link #printTargetDecl} and {@link #printTail}
     * are called exactly once, {@link #printElementDecl} once for
     * each declared task and type.</p>
     */
    public interface StructurePrinter {
        /**
         * Prints the header of the generated output.
         *
         * @param out PrintWriter to write to.
         * @param p Project instance for the current task
         * @param tasks map (name to implementing class)
         * @param types map (name to implementing class)
         * data types.
         */
        void printHead(PrintWriter out, Project p, Hashtable<String, Class<?>> tasks,
                       Hashtable<String, Class<?>> types);

        /**
         * Prints the definition for the target element.
         * @param out PrintWriter to write to.
         */
        void printTargetDecl(PrintWriter out);

        /**
         * Print the definition for a given element.
         *
         * @param out PrintWriter to write to.
         * @param p Project instance for the current task
         * @param name element name.
         * @param element class of the defined element.
         */
        void printElementDecl(PrintWriter out, Project p, String name,
                              Class<?> element);

        /**
         * Prints the trailer.
         * @param out PrintWriter to write to.
         */
        void printTail(PrintWriter out);
    }

    private static class DTDPrinter implements StructurePrinter {

        private static final String BOOLEAN = "%boolean;";
        private static final String TASKS = "%tasks;";
        private static final String TYPES = "%types;";

        private final Hashtable<String, String> visited = new Hashtable<>();

        @Override
        public void printTail(final PrintWriter out) {
            visited.clear();
        }

        @Override
        public void printHead(final PrintWriter out, final Project p, final Hashtable<String, Class<?>> tasks,
                              final Hashtable<String, Class<?>> types) {
            printHead(out, tasks.keySet(), types.keySet());
        }


        /**
         * Prints the header of the generated output.
         *
         * <p>Basically this prints the XML declaration, defines some
         * entities and the project element.</p>
         */
        private void printHead(final PrintWriter out, final Set<String> tasks,
                               final Set<String> types) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
            out.println("<!ENTITY % boolean \"(true|false|on|off|yes|no)\">");

            out.println(tasks.stream().collect(
                Collectors.joining(" | ", "<!ENTITY % tasks \"", "\">")));

            out.println(types.stream().collect(
                Collectors.joining(" | ", "<!ENTITY % types \"", "\">")));

            out.println();

            out.print("<!ELEMENT project (target | extension-point | ");
            out.print(TASKS);
            out.print(" | ");
            out.print(TYPES);
            out.println(")*>");
            out.println("<!ATTLIST project");
            out.println("          name    CDATA #IMPLIED");
            out.println("          default CDATA #IMPLIED");
            out.println("          basedir CDATA #IMPLIED>");
            out.println("");
        }

        /**
         * Prints the definition for the target element.
         */
        @Override
        public void printTargetDecl(final PrintWriter out) {
            out.print("<!ELEMENT target (");
            out.print(TASKS);
            out.print(" | ");
            out.print(TYPES);
            out.println(")*>");
            out.println("");
            printTargetAttrs(out, "target");
            out.println("<!ELEMENT extension-point EMPTY>");
            out.println("");
            printTargetAttrs(out, "extension-point");
        }

        /**
         * Prints the definition for the target element.
         */
        private void printTargetAttrs(final PrintWriter out, final String tag) {
            out.print("<!ATTLIST ");
            out.println(tag);
            out.println("          id                      ID    #IMPLIED");
            out.println("          name                    CDATA #REQUIRED");
            out.println("          if                      CDATA #IMPLIED");
            out.println("          unless                  CDATA #IMPLIED");
            out.println("          depends                 CDATA #IMPLIED");
            out.println("          extensionOf             CDATA #IMPLIED");
            out.println("          onMissingExtensionPoint CDATA #IMPLIED");
            out.println("          description             CDATA #IMPLIED>");
            out.println("");
        }

        /**
         * Print the definition for a given element.
         */
        @Override
        public void printElementDecl(final PrintWriter out, final Project p,
                                     final String name, final Class<?> element) {

            if (visited.containsKey(name)) {
                return;
            }
            visited.put(name, "");

            IntrospectionHelper ih;
            try {
                ih = IntrospectionHelper.getHelper(p, element);
            } catch (final Throwable t) {
                /*
                 * TODO - failed to load the class properly.
                 *
                 * should we print a warning here?
                 */
                return;
            }

            StringBuilder sb = new StringBuilder("<!ELEMENT ").append(name).append(" ");

            if (Reference.class.equals(element)) {
                sb.append(String.format("EMPTY>%n<!ATTLIST %s%n"
                                + "          id ID #IMPLIED%n          refid IDREF #IMPLIED>%n",
                        name));
                out.println(sb);
                return;
            }

            final List<String> v = new ArrayList<>();
            if (ih.supportsCharacters()) {
                v.add("#PCDATA");
            }

            if (TaskContainer.class.isAssignableFrom(element)) {
                v.add(TASKS);
            }

            v.addAll(Collections.list(ih.getNestedElements()));

            final Collector<CharSequence, ?, String> joinAlts =
                Collectors.joining(" | ", "(", ")");

            if (v.isEmpty()) {
                sb.append("EMPTY");
            } else {
                sb.append(v.stream().collect(joinAlts));
                if (v.size() > 1 || !"#PCDATA".equals(v.get(0))) {
                    sb.append("*");
                }
            }
            sb.append(">");
            out.println(sb);

            sb = new StringBuilder();
            sb.append(String.format("<!ATTLIST %s%n          id ID #IMPLIED", name));

            for (final String attrName : Collections.list(ih.getAttributes())) {
                if ("id".equals(attrName)) {
                    continue;
                }

                sb.append(String.format("%n          %s ", attrName));
                final Class<?> type = ih.getAttributeType(attrName);
                if (type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
                    sb.append(BOOLEAN).append(" ");
                } else if (Reference.class.isAssignableFrom(type)) {
                    sb.append("IDREF ");
                } else if (EnumeratedAttribute.class.isAssignableFrom(type)) {
                    try {
                        final EnumeratedAttribute ea =
                            type.asSubclass(EnumeratedAttribute.class).getDeclaredConstructor().newInstance();
                        final String[] values = ea.getValues();
                        if (values == null || values.length == 0
                            || !areNmtokens(values)) {
                            sb.append("CDATA ");
                        } else {
                            sb.append(Stream.of(values).collect(joinAlts)).append(" ");
                        }
                    } catch (final InstantiationException | IllegalAccessException
                            | NoSuchMethodException | InvocationTargetException ie) {
                        sb.append("CDATA ");
                    }
                } else if (Enum.class.isAssignableFrom(type)) {
                    try {
                        final Enum<?>[] values =
                            (Enum<?>[]) type.getMethod("values").invoke(null);
                        if (values.length == 0) {
                            sb.append("CDATA ");
                        } else {
                            sb.append(Stream.of(values).map(Enum::name)
                                .collect(joinAlts)).append(" ");
                        }
                    } catch (final Exception x) {
                        sb.append("CDATA ");
                    }
                } else {
                    sb.append("CDATA ");
                }
                sb.append("#IMPLIED");
            }
            sb.append(String.format(">%n"));
            out.println(sb);

            for (String nestedName : v) {
                if (!"#PCDATA".equals(nestedName)
                    && !TASKS.equals(nestedName)
                    && !TYPES.equals(nestedName)) {
                    printElementDecl(out, p, nestedName, ih.getElementType(nestedName));
                }
            }
        }

        /**
         * Does this String match the XML-NMTOKEN production?
         * @param s the string to test
         * @return true if the string matches the XML-NMTOKEN
         */
        public static final boolean isNmtoken(final String s) {
            final int length = s.length();
            for (int i = 0; i < length; i++) {
                final char c = s.charAt(i);
                // TODO - we are committing CombiningChar and Extender here
                if (!Character.isLetterOrDigit(c)
                    && c != '.' && c != '-' && c != '_' && c != ':') {
                    return false;
                }
            }
            return true;
        }

        /**
         * Do the Strings all match the XML-NMTOKEN production?
         *
         * <p>Otherwise they are not suitable as an enumerated attribute,
         * for example.</p>
         * @param s the array of string to test
         * @return true if all the strings in the array math XML-NMTOKEN
         */
        public static final boolean areNmtokens(final String[] s) {
            for (String value : s) {
                if (!isNmtoken(value)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Does this String match the XML-NMTOKEN production?
     * @param s the string to test
     * @return true if the string matches the XML-NMTOKEN
     */
    protected boolean isNmtoken(final String s) {
        return DTDPrinter.isNmtoken(s);
    }

    /**
     * Do the Strings all match the XML-NMTOKEN production?
     *
     * <p>Otherwise they are not suitable as an enumerated attribute,
     * for example.</p>
     * @param s the array of string to test
     * @return true if all the strings in the array math XML-NMTOKEN
     */
    protected boolean areNmtokens(final String[] s) {
        return DTDPrinter.areNmtokens(s);
    }
}
