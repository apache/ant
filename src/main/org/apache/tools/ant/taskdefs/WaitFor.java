/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.taskdefs.condition.Condition;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Vector;

/**
 * Wait for an external event to occur.
 *
 * Wait for an external process to start or to complete some
 * task. This is useful with the <code>parallel</code> task to
 * syncronize the execution of tests with server startup.
 *
 * The following attributes can be specified on a waitfor task:
 * <ul>
 * <li>maxwait - maximum length of time to wait before giving up</li>
 * <li>checkevery - amount of time to sleep between each check</li>
 * </ul>
 *
 * The time value can include a suffix of "ms", "s", "m", "h" to
 * indicate that the value is in milliseconds, seconds, minutes or
 * hours. The default is milliseconds.
 *
 * @author <a href="mailto:denis@network365.com">Denis Hennessy</a>
 */

public class WaitFor extends ConditionBase {
    private long maxWaitMillis = 1000 * 60 * 3;     // default max wait time
    private long checkEveryMillis = 500;

    /**
     * Set the maximum length of time to wait
     */
    public void setMaxWait(String time) {
        maxWaitMillis = parseTime(time);
    }

    /**
     * Set the time between each check
     */
    public void setCheckEvery(String time) {
        checkEveryMillis = parseTime(time);
    }

    /**
     * Check repeatedly for the specified conditions until they become
     * true or the timeout expires.
     */
    public void execute() throws BuildException {
        if (countConditions() > 1) {
            throw new BuildException("You must not nest more than one condition into <waitfor>");
        }
        if (countConditions() < 1) {
            throw new BuildException("You must nest a condition into <waitfor>");
        }
        Condition c = (Condition) getConditions().nextElement();

        long start = System.currentTimeMillis();
        long end = start + maxWaitMillis;

        while (System.currentTimeMillis() < end) {
            if (c.eval()) {
                return;
            }
            try {
                Thread.sleep(checkEveryMillis);
            } catch (InterruptedException e) {
            }
        }

        throw new BuildException("Task did not complete in time");
    }

    /**
     * Parse a time in the format nnnnnxx where xx is a common time
     * multiplier suffix.
     */
    protected long parseTime(String value) {
        int i = 0;
        for (i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < '0' || ch > '9') {
                break;
            }
        }
        if (i == 0) {
            throw new NumberFormatException();
        }
        String digits = value.substring(0, i);
        return Long.parseLong(digits) * getMultiplier(value.substring(i));
    }

    /**
     * Look for and decipher a multiplier suffix in the string.
     * @param value - a string with a series of digits followed by the
     * scale suffix.
     */
    protected long getMultiplier(String value) {
        String lowercaseValue = value.toLowerCase();
        if (lowercaseValue.startsWith("ms")) {
            return 1;
        }
        if (lowercaseValue.startsWith("s")) {
            return 1000;
        }
        if (lowercaseValue.startsWith("m")) {
            return 1000 * 60;
        }
        if (lowercaseValue.startsWith("h")) {
            return 1000 * 60 * 60;
        }
        return 1;
    }
}
