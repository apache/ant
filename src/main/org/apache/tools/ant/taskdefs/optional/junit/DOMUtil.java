/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.junit;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import org.w3c.dom.EntityReference;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;

import java.util.Vector;

/**
 * Some utilities that might be useful when manipulating DOM trees.
 *
 * @author <a href="bailliez@noos.fr">Stephane Bailliez</a>
 */
public final class DOMUtil {

    /** unused constructor */
    private DOMUtil(){
    }

    /**
     * Filter interface to be applied when iterating over a DOM tree.
     * Just think of it like a <tt>FileFilter</tt> clone.
     */
    public interface NodeFilter {
        /**
         * @param       node    the node to check for acceptance.
         * @return      <tt>true</tt> if the node is accepted by this filter,
         *                      otherwise <tt>false</tt>
         */
        public boolean accept(Node node);
    }

    /**
     * list a set of node that match a specific filter. The list can be made
     * recursively or not.
     * @param   parent  the parent node to search from
     * @param   filter  the filter that children should match.
     * @param   recurse <tt>true</tt> if you want the list to be made recursively
     *                  otherwise <tt>false</tt>.
     */
    public static NodeList listChildNodes(Node parent, NodeFilter filter, boolean recurse){
        NodeListImpl matches = new NodeListImpl();
        NodeList children = parent.getChildNodes();
        if (children != null) {
            final int len = children.getLength();
            for (int i = 0; i < len; i++) {
                Node child = children.item(i);
                if (filter.accept(child)) {
                    matches.addElement(child);
                }
                if (recurse) {
                    NodeList recmatches = listChildNodes(child, filter, recurse);
                    final int reclength = matches.getLength();
                    for (int j = 0; j < reclength; j++) {
                        matches.addElement( recmatches.item(i) );
                    }
                }
            }
        }
        return matches;
    }

    /** custom implementation of a nodelist */
    public static class NodeListImpl extends Vector implements NodeList {
        public int getLength(){
            return size();
        }
        public Node item(int i){
            try {
                return (Node)elementAt(i);
            } catch (ArrayIndexOutOfBoundsException e){
                return null; // conforming to NodeList interface
            }
        }
    }

    /**
     * return the attribute value of an element.
     * @param node the node to get the attribute from.
     * @param name the name of the attribute we are looking for the value.
     * @return the value of the requested attribute or <tt>null</tt> if the
     *         attribute was not found or if <tt>node</tt> is not an <tt>Element</tt>.
     */
    public static String getNodeAttribute(Node node, String name) {
        if (node instanceof Element) {
            Element element = (Element)node;
            return element.getAttribute(name);
        }
        return null;
    }


    /**
     * Iterate over the children of a given node and return the first node
     * that has a specific name.
     * @param   parent  the node to search child from. Can be <tt>null</tt>.
     * @param   tagname the child name we are looking for. Cannot be <tt>null</tt>.
     * @return  the first child that matches the given name or <tt>null</tt> if
     *                  the parent is <tt>null</tt> or if a child does not match the
     *                  given name.
     */
    public static Element getChildByTagName (Node parent, String tagname) {
        if (parent == null){
            return null;
        }
        NodeList childList = parent.getChildNodes();
        final int len = childList.getLength();
        for (int i = 0; i < len; i++) {
            Node child = childList.item(i);
            if ( child != null && child.getNodeType() == Node.ELEMENT_NODE &&
                 child.getNodeName().equals(tagname)) {
                return (Element)child;
            }
        }
        return null;
    }

    /**
     * Simple tree walker that will clone recursively a node. This is to
     * avoid using parser-specific API such as Sun's <tt>changeNodeOwner</tt>
     * when we are dealing with DOM L1 implementations since <tt>cloneNode(boolean)</tt>
     * will not change the owner document.
     * <tt>changeNodeOwner</tt> is much faster and avoid the costly cloning process.
     * <tt>importNode</tt> is in the DOM L2 interface.
     * @param   parent  the node parent to which we should do the import to.
     * @param   child   the node to clone recursively. Its clone will be
     *              appended to <tt>parent</tt>.
     * @return  the cloned node that is appended to <tt>parent</tt>
     */
    public static final Node importNode(Node parent, Node child){
        Node copy = null;
        final Document doc = parent.getOwnerDocument();

        switch (child.getNodeType()) {
        case Node.CDATA_SECTION_NODE:
            copy = doc.createCDATASection(((CDATASection) child).getData());
            break;
        case Node.COMMENT_NODE:
            copy = doc.createComment(((Comment) child).getData());
            break;
        case Node.DOCUMENT_FRAGMENT_NODE:
            copy = doc.createDocumentFragment();
            break;
        case Node.ELEMENT_NODE:
            final Element elem = doc.createElement(((Element)child).getTagName());
            copy = elem;
            final NamedNodeMap attributes = child.getAttributes();
            if ( attributes != null) {
                final int size = attributes.getLength();
                for (int i = 0; i < size; i++) {
                    final Attr attr = (Attr) attributes.item(i);
                    elem.setAttribute(attr.getName(), attr.getValue());
                }
            }
            break;
        case Node.ENTITY_REFERENCE_NODE:
            copy = doc.createEntityReference(child.getNodeName());
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            final ProcessingInstruction pi = (ProcessingInstruction)child;
            copy = doc.createProcessingInstruction(pi.getTarget(), pi.getData());
            break;
        case Node.TEXT_NODE:
            copy = doc.createTextNode(((Text) child).getData());
            break;
        default:
            // this should never happen
            throw new IllegalStateException("Invalid node type: " + child.getNodeType());
        }

        // okay we have a copy of the child, now the child becomes the parent
        // and we are iterating recursively over its children.
        try {
            final NodeList children = child.getChildNodes();
            if (children != null) {
                final int size = children.getLength();
                for (int i = 0; i < size; i++) {
                    final Node newChild = children.item(i);
                    if (newChild != null) {
                        importNode(copy, newChild);
                    }
                }
            }
        } catch (DOMException ignored) {
        }

        // bingo append it. (this should normally not be done here)
        parent.appendChild(copy);
        return copy;
    }

}
