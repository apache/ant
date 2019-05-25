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

package org.apache.tools.ant.taskdefs.condition;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
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
 * @since Ant 1.4
 */
public abstract class ConditionBase extends ProjectComponent {

    /**
     * name of the component
     */
    private String taskName = "condition";

    /**
     *
     */
    private List<Condition> conditions = new Vector<>();

    /**
     * Simple constructor.
     */
    protected ConditionBase() {
        taskName = "component";
    }

    /**
     * Constructor that takes the name of the task in the task name.
     * @param taskName the name of the task.
     * @since Ant 1.7
     */
    protected ConditionBase(String taskName) {
        this.taskName = taskName;
    }

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
    protected final Enumeration<Condition> getConditions() {
        return Collections.enumeration(conditions);
    }

    /**
     * Sets the name to use in logging messages.
     *
     * @param name The name to use in logging messages.
     *             Should not be <code>null</code>.
     * @since Ant 1.7
     */
    public void setTaskName(String name) {
        this.taskName = name;
    }

    /**
     * Returns the name to use in logging messages.
     *
     * @return the name to use in logging messages.
     * @since Ant 1.7
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Add an &lt;available&gt; condition.
     * @param a an available condition
     * @since 1.1
     */
    public void addAvailable(Available a) {
        conditions.add(a);
    }

    /**
     * Add an &lt;checksum&gt; condition.
     *
     * @param c a Checksum condition
     * @since 1.4, Ant 1.5
     */
    public void addChecksum(Checksum c) {
        conditions.add(c);
    }

    /**
     * Add an &lt;uptodate&gt; condition.
     *
     * @param u an UpToDate condition
     * @since 1.1
     */
    public void addUptodate(UpToDate u) {
        conditions.add(u);
    }

    /**
     * Add an &lt;not&gt; condition "container".
     *
     * @param n a Not condition
     * @since 1.1
     */
    public void addNot(Not n) {
        conditions.add(n);
    }

    /**
     * Add an &lt;and&gt; condition "container".
     *
     * @param a an And condition
     * @since 1.1
     */
    public void addAnd(And a) {
        conditions.add(a);
    }

    /**
     * Add an &lt;or&gt; condition "container".
     *
     * @param o an Or condition
     * @since 1.1
     */
    public void addOr(Or o) {
        conditions.add(o);
    }

    /**
     * Add an &lt;equals&gt; condition.
     *
     * @param e an Equals condition
     * @since 1.1
     */
    public void addEquals(Equals e) {
        conditions.add(e);
    }

    /**
     * Add an &lt;os&gt; condition.
     *
     * @param o an Os condition
     * @since 1.1
     */
    public void addOs(Os o) {
        conditions.add(o);
    }

    /**
     * Add an &lt;isset&gt; condition.
     *
     * @param i an IsSet condition
     * @since Ant 1.5
     */
    public void addIsSet(IsSet i) {
        conditions.add(i);
    }

    /**
     * Add an &lt;http&gt; condition.
     *
     * @param h an Http condition
     * @since Ant 1.5
     */
    public void addHttp(Http h) {
        conditions.add(h);
    }

    /**
     * Add a &lt;socket&gt; condition.
     *
     * @param s a Socket condition
     * @since Ant 1.5
     */
    public void addSocket(Socket s) {
        conditions.add(s);
    }

    /**
     * Add a &lt;filesmatch&gt; condition.
     *
     * @param test a FilesMatch condition
     * @since Ant 1.5
     */
    public void addFilesMatch(FilesMatch test) {
        conditions.add(test);
    }

    /**
     * Add a &lt;contains&gt; condition.
     *
     * @param test a Contains condition
     * @since Ant 1.5
     */
    public void addContains(Contains test) {
        conditions.add(test);
    }

    /**
     * Add a &lt;istrue&gt; condition.
     *
     * @param test an IsTrue condition
     * @since Ant 1.5
     */
    public void addIsTrue(IsTrue test) {
        conditions.add(test);
    }

    /**
     * Add a &lt;isfalse&gt; condition.
     *
     * @param test an IsFalse condition
     * @since Ant 1.5
     */
    public void addIsFalse(IsFalse test) {
        conditions.add(test);
    }

    /**
     * Add an &lt;isreference&gt; condition.
     *
     * @param i an IsReference condition
     * @since Ant 1.6
     */
    public void addIsReference(IsReference i) {
        conditions.add(i);
    }

    /**
     * Add an &lt;isfileselected&gt; condition.
     * @param test the condition
     */
    public void addIsFileSelected(IsFileSelected test) {
        conditions.add(test);
    }

    /**
     * Add an arbitrary condition
     * @param c a condition
     * @since Ant 1.6
     */
    public void add(Condition c) {
        conditions.add(c);
    }

}
