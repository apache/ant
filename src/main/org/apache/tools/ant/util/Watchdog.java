/*
 * Copyright  2002-2004 The Apache Software Foundation
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

package org.apache.tools.ant.util;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Generalization of <code>ExecuteWatchdog</code>
 *
 * @since Ant 1.5
 *
 * @see org.apache.tools.ant.taskdefs.ExecuteWatchdog
 *
 * @author Stefan Bodewig
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class Watchdog implements Runnable {

    private Vector observers = new Vector(1);
    private long timeout = -1;
    private boolean stopped = false;

    public Watchdog(long timeout) {
        if (timeout < 1) {
            throw new IllegalArgumentException("timeout lesser than 1.");
        }
        this.timeout = timeout;
    }

    public void addTimeoutObserver(TimeoutObserver to) {
        observers.addElement(to);
    }

    public void removeTimeoutObserver(TimeoutObserver to) {
        observers.removeElement(to);
    }

    protected final void fireTimeoutOccured() {
        Enumeration e = observers.elements();
        while (e.hasMoreElements()) {
            ((TimeoutObserver) e.nextElement()).timeoutOccured(this);
        }
    }

    public synchronized void start() {
        stopped = false;
        Thread t = new Thread(this, "WATCHDOG");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stop() {
        stopped = true;
        notifyAll();
    }

    public synchronized void run() {
        final long until = System.currentTimeMillis() + timeout;
        long now;
        while (!stopped && until > (now = System.currentTimeMillis())) {
            try {
                wait(until - now);
            } catch (InterruptedException e) {
            }
        }
        if (!stopped) {
            fireTimeoutOccured();
        }
    }

}
