/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

/**
 *  This security manager is installed by the Workspace class
 *  while tasks are being invoked so that System.exit calls can
 *  be intercepted. Any tasks that tries to call System.exit
 *  will cause an ExitException to be thrown instead of terminating
 *  the VM.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class AntSecurityManager extends SecurityManager {
    /**
     *  Throws an ExitException which should be caught at the task level and handled.
     */
    public void checkExit(int status) {
        throw new ExitException(status);
    }

    /**
     *  Allows anything.
     */
    public void checkPermission(java.security.Permission p) {
    }
}