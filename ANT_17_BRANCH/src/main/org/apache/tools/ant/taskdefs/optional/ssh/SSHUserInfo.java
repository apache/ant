/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.optional.ssh;

import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.UIKeyboardInteractive;

/**
 * Class containing information on an SSH user.
 */
public class SSHUserInfo implements UserInfo, UIKeyboardInteractive {

    private String name;
    private String password = null;
    private String keyfile;
    private String passphrase = null;
    private boolean trustAllCertificates;

    /** Constructor for SSHUserInfo. */
    public SSHUserInfo() {
        super();
        this.trustAllCertificates = false;
    }

    /**
     * Constructor for SSHUserInfo.
     * @param password the user's password
     * @param trustAllCertificates if true trust hosts whose identity is unknown
     */
    public SSHUserInfo(String password, boolean trustAllCertificates) {
        super();
        this.password = password;
        this.trustAllCertificates = trustAllCertificates;
    }

    /**
     * Gets the user name.
     * @return the user name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the pass phrase of the user.
     * @param message a message
     * @return the passphrase
     */
    public String getPassphrase(String message) {
        return passphrase;
    }

    /**
     * Gets the user's password.
     * @return the user's password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Prompts a string.
     * @param str the string
     * @return whether the string was prompted
     */
    public boolean prompt(String str) {
        return false;
    }

    /**
     * Indicates whether a retry was done.
     * @return whether a retry was done
     */
    public boolean retry() {
        return false;
    }

    /**
     * Sets the name.
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the passphrase.
     * @param passphrase The passphrase to set
     */
    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    /**
     * Sets the password.
     * @param password The password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the trust.
     * @param trust whether to trust or not.
     */
    public void setTrust(boolean trust) {
        this.trustAllCertificates = trust;
    }

    /**
     * @return whether to trust or not.
     */
    public boolean getTrust() {
        return this.trustAllCertificates;
    }

    /**
     * Returns the passphrase.
     * @return String
     */
    public String getPassphrase() {
        return passphrase;
    }

    /**
     * Returns the keyfile.
     * @return String
     */
    public String getKeyfile() {
        return keyfile;
    }

    /**
     * Sets the keyfile.
     * @param keyfile The keyfile to set
     */
    public void setKeyfile(String keyfile) {
        this.keyfile = keyfile;
    }

    /**
     * Implement the UserInfo interface.
     * @param message ignored
     * @return true always
     */
    public boolean promptPassphrase(String message) {
        return true;
    }

    /**
     * Implement the UserInfo interface.
     * @param passwordPrompt ignored
     * @return true the first time this is called, false otherwise
     */
    public boolean promptPassword(String passwordPrompt) {
        return true;
    }

    /**
     * Implement the UserInfo interface.
     * @param message ignored
     * @return the value of trustAllCertificates
     */
    public boolean promptYesNo(String message) {
        return trustAllCertificates;
    }

//why do we do nothing?
    /**
     * Implement the UserInfo interface (noop).
     * @param message ignored
     */
    public void showMessage(String message) {
        //log(message, Project.MSG_DEBUG);
    }

    /**
     * Implementation of UIKeyboardInteractive#promptKeyboardInteractive.
     * @param destination not used.
     * @param name        not used.
     * @param instruction not used.
     * @param prompt      the method checks if this is one in length.
     * @param echo        the method checks if the first element is false.
     * @return the password in an size one array if there is a password
     *         and if the prompt and echo checks pass.
     */
    public String[] promptKeyboardInteractive(String destination,
                                              String name,
                                              String instruction,
                                              String[] prompt,
                                              boolean[] echo) {
        if (prompt.length != 1 || echo[0] || this.password == null) {
            return null;
        }
        String[] response = new String[1];
        response[0] = this.password;
        return response;
    }

}
