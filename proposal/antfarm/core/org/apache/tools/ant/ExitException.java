/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

/**
 *  Thrown by the AntSecurityManager whenever a task tries
 *  to call System.exit().
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class ExitException extends RuntimeException {
    private int status;

    public ExitException(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}