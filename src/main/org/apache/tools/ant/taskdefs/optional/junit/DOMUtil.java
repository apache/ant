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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * Some utilities that might be useful when manipulating DOM trees.
 *
 */
public final class DOMUtil {

    /** unused constructor */
    private DOMUtil() {
    }

    /**
     * Filter interface to be applied when iterating over a DOM tree.
     * Just think of it like a <code>FileFilter</code> clone.
     */
    public interface NodeFilter {
        /**
         * @param       node    the node to check for acceptance.
         * @return      <code>true</code> if the node is accepted by this filter,
         *                      otherwise <code>false</code>
         */
        boolean accept(Node node);
    }

    /**
     * list a set of node that match a specific filter. The list can be made
     * recursively or not.
     * @param   parent  the parent node to search from
     * @param   filter  the filter that children should match.
     * @param   recurse <code>true</code> if you want the list to be made recursively
     *                  otherwise <code>false</code>.
     * @return the node list that matches the filter.
     */
    public static NodeList listChildNodes(Node parent, NodeFilter filter, boolean recurse) {
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
                    final int reclength = recmatches.getLength();
                    for (int j = 0; j < reclength; j++) {
                        matches.addElement(recmatches.item(i));
                    }
                }
            }
        }
        return matches;
    }

    /** custom implementation of a nodelist */
    public static class NodeListImpl extends Vector<Node> implements NodeList {
        private static final long serialVersionUID = 3175749150080946423L;

        /**
         * Get the number of nodes in the list.
         * @return the length of the list.
         */
        @Override
        public int getLength() {
            return size();
        }
        /**
         * Get a particular node.
         * @param i the index of the node to get.
         * @return the node if the index is in bounds, null otherwise.
         */
        @Override
        public Node item(int i) {
            try {
                return elementAt(i);
            } catch (ArrayIndexOutOfBoundsException e) {
                return null; // conforming to NodeList interface
            }
        }
    }

    /**
     * return the attribute value of an element.
     * @param node the node to get the attribute from.
     * @param name the name of the attribute we are looking for the value.
     * @return the value of the requested attribute or <code>null</code> if the
     *         attribute was not found or if <code>node</code> is not an <code>Element</code>.
     */
    public static String getNodeAttribute(Node node, String name) {
        if (node instanceof Element) {
            Element element = (Element) node;
            return element.getAttribute(name);
        }
        return null;
    }


    /**
     * Iterate over the children of a given node and return the first node
     * that has a specific name.
     * @param   parent  the node to search child from. Can be <code>null</code>.
     * @param   tagname the child name we are looking for. Cannot be <code>null</code>.
     * @return  the first child that matches the given name or <code>null</code> if
     *                  the parent is <code>null</code> or if a child does not match the
     *                  given name.
     */
    public static Element getChildByTagName(Node parent, String tagname) {
        if (parent == null) {
            return null;
        }
        NodeList childList = parent.getChildNodes();
        final int len = childList.getLength();
        for (int i = 0; i < len; i++) {
            Node child = childList.item(i);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE
                && child.getNodeName().equals(tagname)) {
                return (Element) child;
            }
        }
        return null;
    }

    /**
     * Simple tree walker that will clone recursively a node. This is to
     * avoid using parser-specific API such as Sun's <code>changeNodeOwner</code>
     * when we are dealing with DOM L1 implementations since <code>cloneNode(boolean)</code>
     * will not change the owner document.
     * <code>changeNodeOwner</code> is much faster and avoid the costly cloning process.
     * <code>importNode</code> is in the DOM L2 interface.
     * @param   parent  the node parent to which we should do the import to.
     * @param   child   the node to clone recursively. Its clone will be
     *              appended to <code>parent</code>.
     * @return  the cloned node that is appended to <code>parent</code>
     */
    public static Node importNode(Node parent, Node child) {
        final Document doc = parent.getOwnerDocument();

        Node copy;
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
            final Element elem = doc.createElement(((Element) child).getTagName());
            copy = elem;
            final NamedNodeMap attributes = child.getAttributes();
            if (attributes != null) {
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
            final ProcessingInstruction pi = (ProcessingInstruction) child;
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
            // Ignore
        }

        // bingo append it. (this should normally not be done here)
        parent.appendChild(copy);
        return copy;
    }
}
