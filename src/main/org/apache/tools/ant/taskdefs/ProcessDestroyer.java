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

package org.apache.tools.ant.taskdefs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Destroys all registered <code>Process</code>es when the VM exits.
 *
 * @author <a href="mailto:mnewcomb@tacintel.com">Michael Newcomb</a>
 * @since Ant 1.5
 */
class ProcessDestroyer
    implements Runnable {

    private Vector processes = new Vector();
    // methods to register and unregister shutdown hooks
    private Method addShutdownHookMethod;
    private Method removeShutdownHookMethod;
    private ProcessDestroyerImpl destroyProcessThread = null;
    
    // whether or not this ProcessDestroyer has been registered as a
    // shutdown hook
    private boolean added = false;
    
    private class ProcessDestroyerImpl extends Thread{
        private boolean shouldDestroy = true;
        
        public ProcessDestroyerImpl(){
            super("ProcessDestroyer Shutdown Hook");
        }
        public void run(){
            if(shouldDestroy){
                ProcessDestroyer.this.run();
            }
        }
        
        public void setShouldDestroy(boolean shouldDestroy){
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
        } catch (Exception e) {
            // it just won't be added as a shutdown hook... :(
        }
    }
    
    /**
     * Registers this <code>ProcessDestroyer</code> as a shutdown hook, 
     * uses reflection to ensure pre-JDK 1.3 compatibility.
     */
    private void addShutdownHook(){
        if(addShutdownHookMethod != null){
            destroyProcessThread = new ProcessDestroyerImpl();
            Object[] args = {destroyProcessThread};
            try {
                addShutdownHookMethod.invoke(Runtime.getRuntime(),args);
                added = true;
            } catch (IllegalAccessException e) {
                // it just won't be added as a shutdown hook... :(
            } catch (InvocationTargetException e) {
                // it just won't be added as a shutdown hook... :(
            }
        }
    }
    
    /**
     * Registers this <code>ProcessDestroyer</code> as a shutdown hook,
     * uses reflection to ensure pre-JDK 1.3 compatibility
     */
    private void removeShutdownHook(){
        if(removeShutdownHookMethod != null && destroyProcessThread != null){
            Object[] args = {destroyProcessThread};
            try{
                Boolean removed =
                    (Boolean) removeShutdownHookMethod.invoke(
                        Runtime.getRuntime(),
                        args);
                if(!removed.booleanValue()){
                    System.err.println("Could not remove shutdown hook");
                }
                // start the hook thread, a unstarted thread may not be
                // eligible for garbage collection
                destroyProcessThread.setShouldDestroy(false);
                destroyProcessThread.start();
                // this should return quickly, since Process.destroy()
                try{ 
                    destroyProcessThread.join(20000);
                }catch(InterruptedException ie){
                    // the thread didn't die in time
                    // it should not kill any processes unexpectedly
                }
                destroyProcessThread = null;
                added = false;
            }catch(IllegalAccessException e){
            }catch(InvocationTargetException e){
            }
        }
    }
    
    /**
     * Returns whether or not the ProcessDestroyer is registered as 
     * as shutdown hook
     * @return true if this is currently added as shutdown hook
     */
    public boolean isAddedAsShutdownHook(){
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
        synchronized(processes){
            // if this list is empty, register the shutdown hook 
            if(processes.size() == 0){
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
        synchronized(processes){
            boolean processRemoved = processes.removeElement(process);
            if(processes.size() == 0){
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
        }
    }
}
