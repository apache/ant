/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.condition;

import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.Available;
import org.apache.tools.ant.taskdefs.Checksum;
import org.apache.tools.ant.taskdefs.UpToDate;

/**
 * Baseclass for the &lt;condition&gt; task as well as several
 * conditions - ensures that the types of conditions inside the task
 * and the "container" conditions are in sync.
 *
 * @author Stefan Bodewig
 * @since Ant 1.4
 * @version $Revision$
 */
public abstract class ConditionBase extends ProjectComponent {
    private Vector conditions = new Vector();

    /**
     * Count the conditions.
     *
     * @return the number of conditions in the container
     * @since 1.1
     */
    protected int countConditions() {
        return conditions.size();
    }

    /**
     * Iterate through all conditions.
     *
     * @return an enumeration to use for iteration
     * @since 1.1
     */
    protected final Enumeration getConditions() {
        return conditions.elements();
    }

    /**
     * Add an &lt;available&gt; condition.
     * @param a an available condition
     * @since 1.1
     */
    public void addAvailable(Available a) {
        conditions.addElement(a);
    }

    /**
     * Add an &lt;checksum&gt; condition.
     *
     * @param c a Checksum condition
     * @since 1.4, Ant 1.5
     */
    public void addChecksum(Checksum c) {
        conditions.addElement(c);
    }

    /**
     * Add an &lt;uptodate&gt; condition.
     *
     * @param u an UpToDate condition
     * @since 1.1
     */
    public void addUptodate(UpToDate u) {
        conditions.addElement(u);
    }

    /**
     * Add an &lt;not&gt; condition "container".
     *
     * @param n a Not condition
     * @since 1.1
     */
    public void addNot(Not n) {
        conditions.addElement(n);
    }

    /**
     * Add an &lt;and&gt; condition "container".
     *
     * @param a an And condition
     * @since 1.1
     */
    public void addAnd(And a) {
        conditions.addElement(a);
    }

    /**
     * Add an &lt;or&gt; condition "container".
     *
     * @param o an Or condition
     * @since 1.1
     */
    public void addOr(Or o) {
        conditions.addElement(o);
    }

    /**
     * Add an &lt;equals&gt; condition.
     *
     * @param e an Equals condition
     * @since 1.1
     */
    public void addEquals(Equals e) {
        conditions.addElement(e);
    }

    /**
     * Add an &lt;os&gt; condition.
     *
     * @param o an Os condition
     * @since 1.1
     */
    public void addOs(Os o) {
        conditions.addElement(o);
    }

    /**
     * Add an &lt;isset&gt; condition.
     *
     * @param i an IsSet condition
     * @since Ant 1.5
     */
    public void addIsSet(IsSet i) {
        conditions.addElement(i);
    }

    /**
     * Add an &lt;http&gt; condition.
     *
     * @param h an Http condition
     * @since Ant 1.5
     */
    public void addHttp(Http h) {
        conditions.addElement(h);
    }

    /**
     * Add a &lt;socket&gt; condition.
     *
     * @param s a Socket condition
     * @since Ant 1.5
     */
    public void addSocket(Socket s) {
        conditions.addElement(s);
    }

    /**
     * Add a &lt;filesmatch&gt; condition.
     *
     * @param test a FilesMatch condition
     * @since Ant 1.5
     */
    public void addFilesMatch(FilesMatch test) {
        conditions.addElement(test);
    }

    /**
     * Add a &lt;contains&gt; condition.
     *
     * @param test a Contains condition
     * @since Ant 1.5
     */
    public void addContains(Contains test) {
        conditions.addElement(test);
    }

    /**
     * Add a &lt;istrue&gt; condition.
     *
     * @param test an IsTrue condition
     * @since Ant 1.5
     */
    public void addIsTrue(IsTrue test) {
        conditions.addElement(test);
    }

    /**
     * Add a &lt;isfalse&gt; condition.
     *
     * @param test an IsFalse condition
     * @since Ant 1.5
     */
    public void addIsFalse(IsFalse test) {
        conditions.addElement(test);
    }

    /**
     * Add an &lt;isreference&gt; condition.
     *
     * @param i an IsReference condition
     * @since Ant 1.6
     */
    public void addIsReference(IsReference i) {
        conditions.addElement(i);
    }

    /**
     * Add an arbitary condition
     * @param c a  condition
     * @since Ant 1.6
     */
    public void add(Condition c) {
        conditions.addElement(c);
    }
}
