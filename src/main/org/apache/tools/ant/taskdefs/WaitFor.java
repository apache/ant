/*
 * Copyright  2001-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Wait for an external event to occur.
 *
 * Wait for an external process to start or to complete some
 * task. This is useful with the <code>parallel</code> task to
 * synchronize the execution of tests with server startup.
 *
 * The following attributes can be specified on a waitfor task:
 * <ul>
 * <li>maxwait - maximum length of time to wait before giving up</li>
 * <li>maxwaitunit - The unit to be used to interpret maxwait attribute</li>
 * <li>checkevery - amount of time to sleep between each check</li>
 * <li>checkeveryunit - The unit to be used to interpret checkevery attribute</li>
 * <li>timeoutproperty - name of a property to set if maxwait has been exceeded.</li>
 * </ul>
 *
 * The maxwaitunit and checkeveryunit are allowed to have the following values:
 * millisecond, second, minute, hour, day and week. The default is millisecond.
 *
 * @author <a href="mailto:denis@network365.com">Denis Hennessy</a>
 * @author Magesh Umasankar
 *
 * @since Ant 1.5
 *
 * @ant.task category="control"
 */

public class WaitFor extends ConditionBase {
    /** default max wait time */
    private long maxWaitMillis = 1000L * 60L * 3L;
    private long maxWaitMultiplier = 1L;
    private long checkEveryMillis = 500L;
    private long checkEveryMultiplier = 1L;
    private String timeoutProperty;

    /**
     * Set the maximum length of time to wait
     */
    public void setMaxWait(long time) {
        maxWaitMillis = time;
    }

    /**
     * Set the max wait time unit
     */
    public void setMaxWaitUnit(Unit unit) {
        maxWaitMultiplier = unit.getMultiplier();
    }

    /**
     * Set the time between each check
     */
    public void setCheckEvery(long time) {
        checkEveryMillis = time;
    }

    /**
     * Set the check every time unit
     */
    public void setCheckEveryUnit(Unit unit) {
        checkEveryMultiplier = unit.getMultiplier();
    }

    /**
     * Name the property to set after a timeout.
     */
    public void setTimeoutProperty(String p) {
        timeoutProperty = p;
    }

    /**
     * Check repeatedly for the specified conditions until they become
     * true or the timeout expires.
     */
    public void execute() throws BuildException {
        if (countConditions() > 1) {
            throw new BuildException("You must not nest more than one "
                                     + "condition into <waitfor>");
        }
        if (countConditions() < 1) {
            throw new BuildException("You must nest a condition into "
                                     + "<waitfor>");
        }
        Condition c = (Condition) getConditions().nextElement();

        long savedMaxWaitMillis = maxWaitMillis;
        long savedCheckEveryMillis = checkEveryMillis;
        try {
            maxWaitMillis *= maxWaitMultiplier;
            checkEveryMillis *= checkEveryMultiplier;
            long start = System.currentTimeMillis();
            long end = start + maxWaitMillis;

            while (System.currentTimeMillis() < end) {
                if (c.eval()) {
                    return;
                }
                try {
                    Thread.sleep(checkEveryMillis);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            if (timeoutProperty != null) {
                getProject().setNewProperty(timeoutProperty, "true");
            }
        } finally {
            maxWaitMillis = savedMaxWaitMillis;
            checkEveryMillis = savedCheckEveryMillis;
        }
    }

    /**
     * The enumeration of units:
     * millisecond, second, minute, hour, day, week
     * @todo we use timestamps in many places, why not factor this out
     */
    public static class Unit extends EnumeratedAttribute {

        private static final String MILLISECOND = "millisecond";
        private static final String SECOND = "second";
        private static final String MINUTE = "minute";
        private static final String HOUR = "hour";
        private static final String DAY = "day";
        private static final String WEEK = "week";

        private static final String[] units = {
            MILLISECOND, SECOND, MINUTE, HOUR, DAY, WEEK
        };

        private Hashtable timeTable = new Hashtable();

        public Unit() {
            timeTable.put(MILLISECOND, new Long(1L));
            timeTable.put(SECOND,      new Long(1000L));
            timeTable.put(MINUTE,      new Long(1000L * 60L));
            timeTable.put(HOUR,        new Long(1000L * 60L * 60L));
            timeTable.put(DAY,         new Long(1000L * 60L * 60L * 24L));
            timeTable.put(WEEK,        new Long(1000L * 60L * 60L * 24L * 7L));
        }

        public long getMultiplier() {
            String key = getValue().toLowerCase();
            Long l = (Long) timeTable.get(key);
            return l.longValue();
        }

        public String[] getValues() {
            return units;
        }
    }
}
