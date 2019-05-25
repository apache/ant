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

package org.apache.tools.ant.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generalization of <code>ExecuteWatchdog</code>
 *
 * @since Ant 1.5
 *
 * @see org.apache.tools.ant.taskdefs.ExecuteWatchdog
 *
 */
public class Watchdog implements Runnable {

    /**
     * Error string.
     * {@value}
     */
    public static final String ERROR_INVALID_TIMEOUT = "timeout less than 1.";

    private List<TimeoutObserver> observers =
        Collections.synchronizedList(new ArrayList<>(1));
    private long timeout = -1;

    /**
     * marked as volatile to stop the compiler caching values or (in java1.5+,
     * reordering access)
     */
    private volatile boolean stopped = false;

    /**
     * Constructor for Watchdog.
     * @param timeout the timeout to use in milliseconds (must be &gt;= 1).
     */
    public Watchdog(long timeout) {
        if (timeout < 1) {
            throw new IllegalArgumentException(ERROR_INVALID_TIMEOUT);
        }
        this.timeout = timeout;
    }

    /**
     * Add a timeout observer.
     * @param to the timeout observer to add.
     */
    public void addTimeoutObserver(TimeoutObserver to) {
        observers.add(to);
    }

    /**
     * Remove a timeout observer.
     * @param to the timeout observer to remove.
     */
    public void removeTimeoutObserver(TimeoutObserver to) {
        observers.remove(to);
    }

    /**
     * Inform the observers that a timeout has occurred.
     * This happens in the watchdog thread.
     */
    protected final void fireTimeoutOccured() {
        observers.forEach(o -> o.timeoutOccured(this));
    }

    /**
     * Start the watch dog.
     */
    public synchronized void start() {
        stopped = false;
        Thread t = new Thread(this, "WATCHDOG");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Stop the watch dog.
     */
    public synchronized void stop() {
        stopped = true;
        notifyAll();
    }

    /**
     * The run method of the watch dog thread.
     * This simply does a wait for the timeout time, and
     * if the stop flag has not been set when the wait has returned or
     * has been interrupted, the watch dog listeners are informed.
     */
    @Override
    public synchronized void run() {
        long now = System.currentTimeMillis();
        final long until = now + timeout;

        try {
            while (!stopped && until > now) {
                wait(until - now);
                now = System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            // Ignore exception
        }
        if (!stopped) {
            fireTimeoutOccured();
        }
    }

}
