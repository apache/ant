// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant;

/**
 * Signals a problem.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class AntException extends Exception {

    public AntException() {
        super();
    }
    
    public AntException(String msg) {
        super(msg);
    }
}