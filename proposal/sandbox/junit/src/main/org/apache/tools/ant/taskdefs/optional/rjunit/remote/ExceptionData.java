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
package org.apache.tools.ant.taskdefs.optional.rjunit.remote;

import java.io.Serializable;

import org.apache.tools.ant.util.StringUtils;

/**
 * A wrapper around an exception since an exception stacktrace is
 * not serializable.
 *
 */
public class ExceptionData implements Serializable {

    /** the stacktrace of the exception */
    private final String stacktrace;

    /** the classname of an exception */
    private final String type;

    /** the message associated to this exception */
    private final String message;

    /**
     * Create a new error.
     * @param exception the exception to run as
     */
    public ExceptionData(Throwable exception) {
        this(exception.getClass().getName(),
                exception.getMessage(),
                StringUtils.getStackTrace(exception));
    }

    /**
     * Create a new error.
     * @param type the type of the error (ie classname).
     * @param message the message associated to this error.
     * @param stacktrace the full stacktrace of this error.
     */
    public ExceptionData(String type, String message, String stacktrace) {
        this.stacktrace = stacktrace;
        this.type = type;
        this.message = message;
    }

    /**
     * @return the type of the error (ie classname)
     */
    public String getType() {
        return type;
    }

    /**
     * @return the message associated to this error.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the stacktrace for this error.
     */
    public String getStackTrace() {
        return stacktrace;
    }

    public boolean equals(Object o){
        if ( o instanceof ExceptionData ){
            ExceptionData other = (ExceptionData)o;
            return ( ( type == null ? other.type == null :  type.equals(other.type) ) &&
                    ( message == null ? other.message == null : message.equals(other.message) ) &&
                    ( stacktrace == null ? other.stacktrace == null : stacktrace.equals(other.stacktrace) ) );
        }
        return false;
    }

    public String toString() {
        return (message != null) ? (type + ": " + message) : type;
    }
}
