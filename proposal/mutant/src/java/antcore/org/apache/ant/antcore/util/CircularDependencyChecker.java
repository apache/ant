/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.ant.antcore.util;
import java.util.Stack;

/**
 * Checks for circular dependencies when visiting nodes of an object
 * hierarchy
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 14 January 2002
 */
public class CircularDependencyChecker {
    /**
     * The activity being undertaken which checking for circular
     * redundancies. This is used for reporting exceptions
     */
    private String activity;

    /** The nodes which we are currently visiting */
    private Stack nodes = new Stack();

    /**
     * Constructor for the CircularDependencyChecker object
     *
     * @param activity the activity being undertaken
     */
    public CircularDependencyChecker(String activity) {
        this.activity = activity;
    }

    /**
     * Visit a Node to check its relationships to other nodes
     *
     * @param node an object which is being visited and analyzed
     * @exception CircularDependencyException if this node is alreay being
     *      visited.
     */
    public void visitNode(Object node) throws CircularDependencyException {
        if (nodes.contains(node)) {
            throw new CircularDependencyException(getDescription(node));
        }
        nodes.push(node);
    }

    /**
     * Complete the examination of the node and leave.
     *
     * @param node an object for which the examination of relationships has
     *      been completed
     * @exception CircularDependencyException if the given node was not
     *      expected.
     */
    public void leaveNode(Object node) throws CircularDependencyException {
        if (!nodes.pop().equals(node)) {
            throw new CircularDependencyException("Internal error: popped " +
                "element was unexpected");
        }
    }

    /**
     * Gets the description of the circular dependency
     *
     * @param endNode the node which was revisited and where the circular
     *      dependency was detected
     * @return the description of the circular dependency
     */
    private String getDescription(Object endNode) {
        StringBuffer sb = new StringBuffer("Circular dependency while "
             + activity + ": ");
        sb.append(endNode);
        Object o = null;
        do {
            o = nodes.pop();
            sb.append(" <- ");
            sb.append(o.toString());
        } while (!(o.equals(endNode)));

        return new String(sb);
    }
}

