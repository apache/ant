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
package org.apache.ant.antlib.monitor;

import org.apache.ant.common.util.Location;
import java.io.PrintStream;

/**
 * A record of some performance values at a particular time
 *
 * @author Conor MacNeill
 */
public class MonitorRecord {
    /** The element name being monitored */
    private String elementName;

    /** The location in the build file */
    private Location location;

    /** System clock when task started */
    private long startMillis;

    /** Starting memory when task started */
    private long startMemory;

    /**
     * Get the current used memory from the runtime
     *
     * @return the amount of memory in use.
     */
    private long getMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    /**
     * Create a monitoring record.
     *
     * @param elementName the name of the element.
     * @param location the build file location of the element.
     */
    public MonitorRecord(String elementName, Location location) {
        this.elementName = elementName;
        this.location = location;
        startMillis = System.currentTimeMillis();
        startMemory = getMemoryUsage();
    }

    /**
     * Print a summary of the monitor
     *
     * @param stream the stream onto which the summary is printed.
     */
    public void print(PrintStream stream) {
        long timeDiff = System.currentTimeMillis() - startMillis;
        long memDiff = getMemoryUsage() - startMemory;
        stream.println(location + elementName + " took " + timeDiff
            + " milliseconds, memory used at start " + startMemory
            + " which changed by " + memDiff);
    }
}
