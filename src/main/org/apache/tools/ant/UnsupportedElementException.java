/*
 * Copyright  2004 The Apache Software Foundation
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
package org.apache.tools.ant;

/**
 * Used to report attempts to set an unsupported element
 * When the attempt to set the element is made,
 * the code does not not know the name of the task/type
 * based on a mapping from the classname to the task/type.
 * However one class may be used by a lot of task/types.
 * This exception may be caught by code that does know
 * the task/type and it will reset the message to the
 * correct message.
 * This will be done once (in the case of a recursive
 * call to handlechildren).
 *
 * @since Ant 1.6.3 or Ant 1.7 ?
 */
public class UnsupportedElementException extends BuildException {

    private String  myMessage = null;
    private String  element;

    /**
     * Constructs an unsupport element exception
     * @param msg The string containing the message
     * @param element The name of the incorrect element
     */
    public UnsupportedElementException(String msg, String element) {
        super(msg);
        this.element = element;
    }

    /**
     * The element that is wrong
     *
     * @return the element name
     */
    public String getElement() {
        return element;
    }

    /**
     * Override throwable#getMessage
     * @return the message
     */
    public String getMessage() {
        if (myMessage == null) {
            return super.getMessage();
        } else {
            return myMessage;
        }
    }

    /**
     * Set the message (If not set already)
     * @param message a new message
     */
    public void setMessage(String message) {
        if (this.myMessage == null) {
            this.myMessage = message;
        }
    }
}
