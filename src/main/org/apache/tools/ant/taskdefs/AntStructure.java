/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.io.*;

/**
 * Creates a partial DTD for Ant from the currently known tasks.
 *
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a>
 */

public class AntStructure extends Task {

    private final String lSep = System.getProperty("line.separator");

    private Hashtable visited = new Hashtable();

    private File output;

    /**
     * The output file.
     */
    public void setOutput(File output) {
        this.output = output;
    }

    public void execute() throws BuildException {

        if (output == null) {
            throw new BuildException("output attribute is required", location);
        }
        
        PrintWriter out = null;
        try {
            try {
                out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), "ISO8859_1"));
            } catch (UnsupportedEncodingException ue) {
                /*
                 * Plain impossible with ISO8859_1, see
                 * http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html
                 *
                 * fallback to platform specific anyway.
                 */
                out = new PrintWriter(new FileWriter(output));
            }
            
            Enumeration dataTypes = project.getDataTypeDefinitions().keys();
            printHead(out, dataTypes);

            Vector tasks = new Vector();
            Enumeration enum = project.getTaskDefinitions().keys();
            while (enum.hasMoreElements()) {
                String taskName = (String) enum.nextElement();
                tasks.addElement(taskName);
            }
            printTargetDecl(out, tasks);

            dataTypes = project.getDataTypeDefinitions().keys();
            while (dataTypes.hasMoreElements()) {
                String typeName = (String) dataTypes.nextElement();
                printElementDecl(out, typeName, 
                                 (Class) project.getDataTypeDefinitions().get(typeName));
            }
            
            for (int i=0; i<tasks.size(); i++) {
                String taskName = (String) tasks.elementAt(i);
                printElementDecl(out, taskName, 
                                 (Class) project.getTaskDefinitions().get(taskName));
            }

            printTail(out);

        } catch (IOException ioe) {
            throw new BuildException("Error writing "+output.getAbsolutePath(),
                                     ioe, location);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void printHead(PrintWriter out, Enumeration enum) {
        out.println("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
        out.println("<!ENTITY % boolean \"(true|false|on|off|yes|no)\">");
        out.println("");
        
        out.print("<!ELEMENT project (target | property | taskdef");
        while (enum.hasMoreElements()) {
            String typeName = (String) enum.nextElement();
            out.print(" | "+typeName);
        }

        out.println(")*>");
        out.println("<!ATTLIST project");
        out.println("          name    CDATA #REQUIRED");
        out.println("          default CDATA #REQUIRED");
        out.println("          basedir CDATA #IMPLIED>");
        out.println("");
    }

    private void printTargetDecl(PrintWriter out, Vector tasks) {
        out.print("<!ELEMENT target (");
        for (int i=0; i<tasks.size(); i++) {
            String taskName = (String) tasks.elementAt(i);
            if (i > 0) {
                out.print(" | ");
            }
            out.print(taskName);
        }
                 
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

    private void printElementDecl(PrintWriter out, String name, Class element) 
        throws BuildException {

        if (visited.containsKey(name)) {
            return;
        }
        visited.put(name, "");

        IntrospectionHelper ih = IntrospectionHelper.getHelper(element);

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

        Enumeration enum = ih.getNestedElements();
        while (enum.hasMoreElements()) {
            v.addElement((String) enum.nextElement());
        }

        if (v.isEmpty()) {
            sb.append("EMPTY");
        } else {
            sb.append("(");
            for (int i=0; i<v.size(); i++) {
                if (i != 0) {
                    sb.append(" | ");
                }
                sb.append(v.elementAt(i));
            }
            sb.append(")");
            if (v.size() > 1 || !v.elementAt(0).equals("#PCDATA")) {
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
            if ("id".equals(attrName)) continue;
            
            sb.append(lSep).append("          ").append(attrName).append(" ");
            Class type = ih.getAttributeType(attrName);
            if (type.equals(java.lang.Boolean.class) || 
                type.equals(java.lang.Boolean.TYPE)) {
                sb.append("%boolean; ");
            } else if (org.apache.tools.ant.types.Reference.class.isAssignableFrom(type)) { 
                sb.append("IDREF ");
            } else if (org.apache.tools.ant.types.EnumeratedAttribute.class.isAssignableFrom(type)) {
                try {
                    EnumeratedAttribute ea = 
                        (EnumeratedAttribute)type.newInstance();
                    String[] values = ea.getValues();
                    if (values == null || values.length == 0) {
                        sb.append("CDATA ");
                    } else {
                        sb.append("(");
                        for (int i=0; i < values.length; i++) {
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

        for (int i=0; i<v.size(); i++) {
            String nestedName = (String) v.elementAt(i);
            if (!"#PCDATA".equals(nestedName)) {
                printElementDecl(out, nestedName, ih.getElementType(nestedName));
            }
        }
    }
    
    private void printTail(PrintWriter out) {}

}
