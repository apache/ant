/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Creates a partial DTD for Ant from the currently known tasks.
 *
 * @author Stefan Bodewig
 *
 * @version $Revision$
 *
 * @since Ant 1.1
 *
 * @ant.task category="xml"
 */
public class AntStructure extends Task {

    private final String lSep = System.getProperty("line.separator");

    private final String BOOLEAN = "%boolean;";
    private final String TASKS = "%tasks;";
    private final String TYPES = "%types;";

    private Hashtable visited = new Hashtable();

    private File output;

    /**
     * The output file.
     */
    public void setOutput(File output) {
        this.output = output;
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
                 * http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html
                 *
                 * fallback to platform specific anyway.
                 */
                out = new PrintWriter(new FileWriter(output));
            }

            printHead(out, getProject().getTaskDefinitions().keys(),
                      getProject().getDataTypeDefinitions().keys());

            printTargetDecl(out);

            Enumeration dataTypes = getProject().getDataTypeDefinitions().keys();
            while (dataTypes.hasMoreElements()) {
                String typeName = (String) dataTypes.nextElement();
                printElementDecl(out, typeName,
                                 (Class) getProject().getDataTypeDefinitions().get(typeName));
            }

            Enumeration tasks = getProject().getTaskDefinitions().keys();
            while (tasks.hasMoreElements()) {
                String taskName = (String) tasks.nextElement();
                printElementDecl(out, taskName,
                                 (Class) getProject().getTaskDefinitions().get(taskName));
            }

        } catch (IOException ioe) {
            throw new BuildException("Error writing "
                + output.getAbsolutePath(), ioe, getLocation());
        } finally {
            if (out != null) {
                out.close();
            }
            visited.clear();
        }
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
            String taskName = (String) tasks.nextElement();
            if (!first) {
                out.print(" | ");
            } else {
                first = false;
            }
            out.print(taskName);
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
    private void printTargetDecl(PrintWriter out) {
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
    private void printElementDecl(PrintWriter out, String name, Class element)
        throws BuildException {

        if (visited.containsKey(name)) {
            return;
        }
        visited.put(name, "");

        IntrospectionHelper ih = null;
        try {
            ih = IntrospectionHelper.getHelper(element);
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
            sb.append("EMPTY>").append(lSep);
            sb.append("<!ATTLIST ").append(name);
            sb.append(lSep).append("          id ID #IMPLIED");
            sb.append(lSep).append("          refid IDREF #IMPLIED");
            sb.append(">").append(lSep);
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

        Enumeration enum = ih.getNestedElements();
        while (enum.hasMoreElements()) {
            v.addElement(enum.nextElement());
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

        sb.setLength(0);
        sb.append("<!ATTLIST ").append(name);
        sb.append(lSep).append("          id ID #IMPLIED");

        enum = ih.getAttributes();
        while (enum.hasMoreElements()) {
            String attrName = (String) enum.nextElement();
            if ("id".equals(attrName)) {
              continue;
            }

            sb.append(lSep).append("          ").append(attrName).append(" ");
            Class type = ih.getAttributeType(attrName);
            if (type.equals(java.lang.Boolean.class) ||
                type.equals(java.lang.Boolean.TYPE)) {
                sb.append(BOOLEAN).append(" ");
            } else if (org.apache.tools.ant.types.Reference.class.isAssignableFrom(type)) {
                sb.append("IDREF ");
            } else if (org.apache.tools.ant.types.EnumeratedAttribute.class.isAssignableFrom(type)) {
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
            } else {
                sb.append("CDATA ");
            }
            sb.append("#IMPLIED");
        }
        sb.append(">").append(lSep);
        out.println(sb);

        final int count = v.size();
        for (int i = 0; i < count; i++) {
            String nestedName = (String) v.elementAt(i);
            if (!"#PCDATA".equals(nestedName)
                 && !TASKS.equals(nestedName)
                 && !TYPES.equals(nestedName)) {
                printElementDecl(out, nestedName, ih.getElementType(nestedName));
            }
        }
    }

    /**
     * Does this String match the XML-NMTOKEN production?
     */
    protected boolean isNmtoken(String s) {
        final int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            // XXX - we are ommitting CombiningChar and Extender here
            if (!Character.isLetterOrDigit(c) &&
                c != '.' && c != '-' &&
                c != '_' && c != ':') {
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
     */
    protected boolean areNmtokens(String[] s) {
        for (int i = 0; i < s.length; i++) {
            if (!isNmtoken(s[i])) {
                return false;
            }
        }
        return true;
    }

}
