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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Destroys all registered <code>Process</code>es when the VM exits.
 *
 * @author <a href="mailto:mnewcomb@tacintel.com">Michael Newcomb</a>
 * @author <a href="mailto:jallers@advancedreality.com">Jim Allers</a>
 * @since Ant 1.5
 */
class ProcessDestroyer implements Runnable {

    private Vector processes = new Vector();
    // methods to register and unregister shutdown hooks
    private Method addShutdownHookMethod;
    private Method removeShutdownHookMethod;
    private ProcessDestroyerImpl destroyProcessThread = null;

    // whether or not this ProcessDestroyer has been registered as a
    // shutdown hook
    private boolean added = false;

    private class ProcessDestroyerImpl extends Thread {
        private boolean shouldDestroy = true;

        public ProcessDestroyerImpl() {
            super("ProcessDestroyer Shutdown Hook");
        }
        public void run() {
            if (shouldDestroy) {
                ProcessDestroyer.this.run();
            }
        }

        public void setShouldDestroy(boolean shouldDestroy) {
            this.shouldDestroy = shouldDestroy;
        }
    }

    /**
     * Constructs a <code>ProcessDestroyer</code> and obtains
     * <code>Runtime.addShutdownHook()</code> and
     * <code>Runtime.removeShutdownHook()</code> through reflection. The
     * ProcessDestroyer manages a list of processes to be destroyed when the
     * VM exits. If a process is added when the list is empty,
     * this <code>ProcessDestroyer</code> is registered as a shutdown hook. If
     * removing a process results in an empty list, the
     * <code>ProcessDestroyer</code> is removed as a shutdown hook.
     */
    public ProcessDestroyer() {
        try {
            // check to see if the shutdown hook methods exists
            // (support pre-JDK 1.3 VMs)
            Class[] paramTypes = {Thread.class};
            addShutdownHookMethod =
                Runtime.class.getMethod("addShutdownHook", paramTypes);

            removeShutdownHookMethod =
                Runtime.class.getMethod("removeShutdownHook", paramTypes);
            // wait to add shutdown hook as needed
        } catch (NoSuchMethodException e) {
            // it just won't be added as a shutdown hook... :(
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers this <code>ProcessDestroyer</code> as a shutdown hook,
     * uses reflection to ensure pre-JDK 1.3 compatibility.
     */
    private void addShutdownHook() {
        if (addShutdownHookMethod != null) {
            destroyProcessThread = new ProcessDestroyerImpl();
            Object[] args = {destroyProcessThread};
            try {
                addShutdownHookMethod.invoke(Runtime.getRuntime(), args);
                added = true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Removes this <code>ProcessDestroyer</code> as a shutdown hook,
     * uses reflection to ensure pre-JDK 1.3 compatibility
     */
    private void removeShutdownHook() {
        if (removeShutdownHookMethod != null && destroyProcessThread != null) {
            Object[] args = {destroyProcessThread};
            try {
                Boolean removed =
                    (Boolean) removeShutdownHookMethod.invoke(
                        Runtime.getRuntime(),
                        args);
                if (!removed.booleanValue()) {
                    System.err.println("Could not remove shutdown hook");
                }
                // start the hook thread, a unstarted thread may not be
                // eligible for garbage collection
                // Cf.: http://developer.java.sun.com/developer/bugParade/bugs/4533087.html
                destroyProcessThread.setShouldDestroy(false);
                destroyProcessThread.start();
                // this should return quickly, since Process.destroy()
                try {
                    destroyProcessThread.join(20000);
                } catch (InterruptedException ie) {
                    // the thread didn't die in time
                    // it should not kill any processes unexpectedly
                }
                destroyProcessThread = null;
                added = false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns whether or not the ProcessDestroyer is registered as
     * as shutdown hook
     * @return true if this is currently added as shutdown hook
     */
    public boolean isAddedAsShutdownHook() {
        return added;
    }

    /**
     * Returns <code>true</code> if the specified <code>Process</code> was
     * successfully added to the list of processes to destroy upon VM exit.
     *
     * @param   process the process to add
     * @return  <code>true</code> if the specified <code>Process</code> was
     *          successfully added
     */
    public boolean add(Process process) {
        synchronized (processes) {
            // if this list is empty, register the shutdown hook
            if (processes.size() == 0) {
                addShutdownHook();
            }
            processes.addElement(process);
            return processes.contains(process);
        }
    }

    /**
     * Returns <code>true</code> if the specified <code>Process</code> was
     * successfully removed from the list of processes to destroy upon VM exit.
     *
     * @param   process the process to remove
     * @return  <code>true</code> if the specified <code>Process</code> was
     *          successfully removed
     */
    public boolean remove(Process process) {
        synchronized (processes) {
            boolean processRemoved = processes.removeElement(process);
            if (processes.size() == 0) {
                processes.notifyAll();
                removeShutdownHook();
            }
            return processRemoved;
        }
    }

    /**
     * Invoked by the VM when it is exiting.
     */
    public void run() {
        synchronized (processes) {
            Enumeration e = processes.elements();
            while (e.hasMoreElements()) {
                ((Process) e.nextElement()).destroy();
            }

            try {
                // wait for all processes to finish
                processes.wait();
            } catch (InterruptedException interrupt) {
                // ignore
            }
        }
    }
}
