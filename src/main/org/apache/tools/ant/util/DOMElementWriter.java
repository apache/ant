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

package org.apache.tools.ant.util;

import java.io.*;
import org.w3c.dom.*;

/**
 * Writes a DOM tree to a given Writer.
 *
 * <p>Utility class used by {@link org.apache.tools.ant.XmlLogger
 * XmlLogger} and
 * org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter
 * XMLJUnitResultFormatter}.</p>
 *
 * @author The original author of XmlLogger
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</tt>
 */
public class DOMElementWriter {

    private static String lSep = System.getProperty("line.separator");
    private StringBuffer sb = new StringBuffer();

    /**
     * Don't try to be too smart but at least recognize the predefined
     * entities.
     */
    protected String[] knownEntities = {"gt", "amp", "lt", "apos", "quot"};
    
    /**
     * Writes a DOM tree to a stream.
     *
     * @param element the Root DOM element of the tree
     * @param out where to send the output
     * @param indent number of 
     * @param indentWith strings, 
     *       that should be used to indent the corresponding tag.
     */
    public void write(Element element, Writer out, int indent, 
                      String indentWith)
        throws IOException {

        // Write indent characters
        for (int i = 0; i < indent; i++) {
            out.write(indentWith);
        }

        // Write element
        out.write("<");
        out.write(element.getTagName());

        // Write attributes
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            out.write(" ");
            out.write(attr.getName());
            out.write("=\"");
            out.write(encode(attr.getValue()));
            out.write("\"");
        }
        out.write(">");

        // Write child elements and text
        boolean hasChildren = false;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            switch (child.getNodeType()) {
                
            case Node.ELEMENT_NODE:
                if (!hasChildren) {
                    out.write(lSep);
                    hasChildren = true;
                }
                write((Element)child, out, indent + 1, indentWith);
                break;
                
            case Node.TEXT_NODE:
                out.write(encode(child.getNodeValue()));
                break;
                
            case Node.CDATA_SECTION_NODE:
                out.write("<![CDATA[");
                out.write(((Text)child).getData());
                out.write("]]>");
                break;

            case Node.ENTITY_REFERENCE_NODE:
                out.write('&');
                out.write(child.getNodeName());
                out.write(';');
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                out.write("<?");
                out.write(child.getNodeName());
                String data = child.getNodeValue();
                if ( data != null && data.length() > 0 ) {
                    out.write(' ');
                    out.write(data);
                }
                out.write("?>");
                break;
            }
        }

        // If we had child elements, we need to indent before we close
        // the element, otherwise we're on the same line and don't need
        // to indent
        if (hasChildren) {
            for (int i = 0; i < indent; i++) {
                out.write(indentWith);
            }
        }

        // Write element close
        out.write("</");
        out.write(element.getTagName());
        out.write(">");
        out.write(lSep);
        out.flush();
    }

    /**
     * Escape &lt;, &gt; &amp; &apos; and &quot; as their entities.
     */
    public String encode(String value) {
        sb.setLength(0);
        for (int i=0; i<value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '\'':
                sb.append("&apos;");
                break;
            case '\"':
                sb.append("&quot;");
                break;
            case '&':
                int nextSemi = value.indexOf(";", i);
                if (nextSemi < 0
                    || !isReference(value.substring(i, nextSemi+1))) {
                    sb.append("&amp;");
                } else {
                    sb.append('&');
                }
                break;
            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Is the given argument a character or entity reference?
     */
    public boolean isReference(String ent) {
        if (!(ent.charAt(0) == '&') || !ent.endsWith(";")) {
            return false;
        }

        if (ent.charAt(1) == '#') {
            if (ent.charAt(2) == 'x') {
                try {
                    Integer.parseInt(ent.substring(3, ent.length()-1), 16);
                    return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            } else {
                try {
                    Integer.parseInt(ent.substring(2, ent.length()-1));
                    return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            }
        }

        String name = ent.substring(1, ent.length() - 1);
        for (int i=0; i<knownEntities.length; i++) {
            if (name.equals(knownEntities[i])) {
                return true;
            }
        }
        return false;
    }
}
