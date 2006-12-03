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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
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

    private static final String LINE_SEP
        = System.getProperty("line.separator");

    private File output;
    private StructurePrinter printer = new DTDPrinter();

    /**
     * The output file.
     * @param output the output file
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * The StructurePrinter to use.
     * @param p the printer to use.
     * @since Ant 1.7
     */
    public void add(StructurePrinter p) {
        printer = p;
    }

    /**
     * Build the antstructure DTD.
     *
     * @exception BuildException if the DTD cannot be written.
     */
    public void execute() throws BuildException {

        if (output == null) {
            throw new BuildException("output attribute is required", getLocation());
        }

        PrintWriter out = null;
        try {
            try {
                out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF8"));
            } catch (UnsupportedEncodingException ue) {
                /*
                 * Plain impossible with UTF8, see
                 * http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html
                 *
                 * fallback to platform specific anyway.
                 */
                out = new PrintWriter(new FileWriter(output));
            }

            printer.printHead(out, getProject(),
                              getProject().getTaskDefinitions(),
                              getProject().getDataTypeDefinitions());

            printer.printTargetDecl(out);

            Enumeration dataTypes = getProject().getDataTypeDefinitions().keys();
            while (dataTypes.hasMoreElements()) {
                String typeName = (String) dataTypes.nextElement();
                printer.printElementDecl(
                    out, getProject(), typeName,
                    (Class) getProject().getDataTypeDefinitions().get(typeName));
            }

            Enumeration tasks = getProject().getTaskDefinitions().keys();
            while (tasks.hasMoreElements()) {
                String tName = (String) tasks.nextElement();
                printer.printElementDecl(out, getProject(), tName,
                                         (Class) getProject().getTaskDefinitions().get(tName));
            }

            printer.printTail(out);

        } catch (IOException ioe) {
            throw new BuildException("Error writing "
                                     + output.getAbsolutePath(), ioe, getLocation());
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Writes the actual structure information.
     *
     * <p>{@link #printHead}, {@link #printTargetDecl} and {@link #printTail}
     * are called exactly once, {@link #printElementDecl} once for
     * each declared task and type.</p>
     */
    public static interface StructurePrinter {
        /**
         * Prints the header of the generated output.
         *
         * @param out PrintWriter to write to.
         * @param p Project instance for the current task
         * @param tasks map (name to implementing class)
         * @param types map (name to implementing class)
         * data types.
         */
        void printHead(PrintWriter out, Project p, Hashtable tasks,
                       Hashtable types);

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
                              Class element);

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

        private Hashtable visited = new Hashtable();

        public void printTail(PrintWriter out) {
            visited.clear();
        }

        public void printHead(PrintWriter out, Project p, Hashtable tasks, Hashtable types) {
            printHead(out, tasks.keys(), types.keys());
        }


        /**
         * Prints the header of the generated output.
         *
         * <p>Basically this prints the XML declaration, defines some
         * entities and the project element.</p>
         */
        private void printHead(PrintWriter out, Enumeration tasks,
                               Enumeration types) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
            out.println("<!ENTITY % boolean \"(true|false|on|off|yes|no)\">");
            out.print("<!ENTITY % tasks \"");
            boolean first = true;
            while (tasks.hasMoreElements()) {
                String tName = (String) tasks.nextElement();
                if (!first) {
                    out.print(" | ");
                } else {
                    first = false;
                }
                out.print(tName);
            }
            out.println("\">");
            out.print("<!ENTITY % types \"");
            first = true;
            while (types.hasMoreElements()) {
                String typeName = (String) types.nextElement();
                if (!first) {
                    out.print(" | ");
                } else {
                    first = false;
                }
                out.print(typeName);
            }
            out.println("\">");

            out.println("");

            out.print("<!ELEMENT project (target | ");
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
        public void printTargetDecl(PrintWriter out) {
            out.print("<!ELEMENT target (");
            out.print(TASKS);
            out.print(" | ");
            out.print(TYPES);
            out.println(")*>");
            out.println("");

            out.println("<!ATTLIST target");
            out.println("          id          ID    #IMPLIED");
            out.println("          name        CDATA #REQUIRED");
            out.println("          if          CDATA #IMPLIED");
            out.println("          unless      CDATA #IMPLIED");
            out.println("          depends     CDATA #IMPLIED");
            out.println("          description CDATA #IMPLIED>");
            out.println("");
        }

        /**
         * Print the definition for a given element.
         */
        public void printElementDecl(PrintWriter out, Project p,
                                     String name, Class element) {

            if (visited.containsKey(name)) {
                return;
            }
            visited.put(name, "");

            IntrospectionHelper ih = null;
            try {
                ih = IntrospectionHelper.getHelper(p, element);
            } catch (Throwable t) {
                /*
                 * XXX - failed to load the class properly.
                 *
                 * should we print a warning here?
                 */
                return;
            }

            StringBuffer sb = new StringBuffer("<!ELEMENT ");
            sb.append(name).append(" ");

            if (org.apache.tools.ant.types.Reference.class.equals(element)) {
                sb.append("EMPTY>").append(LINE_SEP);
                sb.append("<!ATTLIST ").append(name);
                sb.append(LINE_SEP).append("          id ID #IMPLIED");
                sb.append(LINE_SEP).append("          refid IDREF #IMPLIED");
                sb.append(">").append(LINE_SEP);
                out.println(sb);
                return;
            }

            Vector v = new Vector();
            if (ih.supportsCharacters()) {
                v.addElement("#PCDATA");
            }

            if (TaskContainer.class.isAssignableFrom(element)) {
                v.addElement(TASKS);
            }

            Enumeration e = ih.getNestedElements();
            while (e.hasMoreElements()) {
                v.addElement(e.nextElement());
            }

            if (v.isEmpty()) {
                sb.append("EMPTY");
            } else {
                sb.append("(");
                final int count = v.size();
                for (int i = 0; i < count; i++) {
                    if (i != 0) {
                        sb.append(" | ");
                    }
                    sb.append(v.elementAt(i));
                }
                sb.append(")");
                if (count > 1 || !v.elementAt(0).equals("#PCDATA")) {
                    sb.append("*");
                }
            }
            sb.append(">");
            out.println(sb);

            sb = new StringBuffer("<!ATTLIST ");
            sb.append(name);
            sb.append(LINE_SEP).append("          id ID #IMPLIED");

            e = ih.getAttributes();
            while (e.hasMoreElements()) {
                String attrName = (String) e.nextElement();
                if ("id".equals(attrName)) {
                    continue;
                }

                sb.append(LINE_SEP).append("          ")
                    .append(attrName).append(" ");
                Class type = ih.getAttributeType(attrName);
                if (type.equals(java.lang.Boolean.class)
                    || type.equals(java.lang.Boolean.TYPE)) {
                    sb.append(BOOLEAN).append(" ");
                } else if (Reference.class.isAssignableFrom(type)) {
                    sb.append("IDREF ");
                } else if (EnumeratedAttribute.class.isAssignableFrom(type)) {
                    try {
                        EnumeratedAttribute ea =
                            (EnumeratedAttribute) type.newInstance();
                        String[] values = ea.getValues();
                        if (values == null
                            || values.length == 0
                            || !areNmtokens(values)) {
                            sb.append("CDATA ");
                        } else {
                            sb.append("(");
                            for (int i = 0; i < values.length; i++) {
                                if (i != 0) {
                                    sb.append(" | ");
                                }
                                sb.append(values[i]);
                            }
                            sb.append(") ");
                        }
                    } catch (InstantiationException ie) {
                        sb.append("CDATA ");
                    } catch (IllegalAccessException ie) {
                        sb.append("CDATA ");
                    }
                } else if (type.getSuperclass() != null
                           && type.getSuperclass().getName().equals("java.lang.Enum")) {
                    try {
                        Object[] values = (Object[]) type.getMethod("values", (Class[])  null)
                            .invoke(null, (Object[]) null);
                        if (values.length == 0) {
                            sb.append("CDATA ");
                        } else {
                            sb.append('(');
                            for (int i = 0; i < values.length; i++) {
                                if (i != 0) {
                                    sb.append(" | ");
                                }
                                sb.append(type.getMethod("name", (Class[]) null)
                                          .invoke(values[i], (Object[]) null));
                            }
                            sb.append(") ");
                        }
                    } catch (Exception x) {
                        sb.append("CDATA ");
                    }
                } else {
                    sb.append("CDATA ");
                }
                sb.append("#IMPLIED");
            }
            sb.append(">").append(LINE_SEP);
            out.println(sb);

            final int count = v.size();
            for (int i = 0; i < count; i++) {
                String nestedName = (String) v.elementAt(i);
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
        public static final boolean isNmtoken(String s) {
            final int length = s.length();
            for (int i = 0; i < length; i++) {
                char c = s.charAt(i);
                // XXX - we are committing CombiningChar and Extender here
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
        public static final boolean areNmtokens(String[] s) {
            for (int i = 0; i < s.length; i++) {
                if (!isNmtoken(s[i])) {
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
    protected boolean isNmtoken(String s) {
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
    protected boolean areNmtokens(String[] s) {
        return DTDPrinter.areNmtokens(s);
    }
}
