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
package org.apache.tools.ant.taskdefs;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Generates a key in a keystore.
 *
 * @since Ant 1.2
 *
 * @ant.task name="genkey" category="java"
 */
public class GenerateKey extends Task {

    /**
     * A DistinguishedName parameter.
     * This is a nested element in a dname nested element.
     */
    public static class DnameParam {
        private String name;
        private String value;

        /**
         * Set the name attribute.
         * @param name a <code>String</code> value
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Get the name attribute.
         * @return the name.
         */
        public String getName() {
            return name;
        }

        /**
         * Set the value attribute.
         * @param value a <code>String</code> value
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * Get the value attribute.
         * @return the value.
         */
        public String getValue() {
            return value;
        }

        public boolean isComplete() {
            return name != null && value != null;
        }
    }

    /**
     * A class corresponding to the dname nested element.
     */
    public static class DistinguishedName {
        private List<DnameParam> params = new Vector<>();

        /**
         * Create a param nested element.
         * @return a DnameParam object to be configured.
         */
        public Object createParam() {
            DnameParam param = new DnameParam();
            params.add(param);
            return param;
        }

        /**
         * Get the nested parameters.
         * @return an enumeration of the nested parameters.
         */
        public Enumeration<DnameParam> getParams() {
            return Collections.enumeration(params);
        }

        /**
         * Generate a string rep of this distinguished name.
         * The format is each of the parameters (name = value)
         * separated by ','.
         * This is used on the command line.
         * @return a string rep of this name
         */
        @Override
        public String toString() {
            return params.stream().map(p -> encode(p.getName()) + "=" + encode(p.getValue()))
                .collect(Collectors.joining(", "));
        }

        /**
         * Encode a name or value.
         * The encoded result is the same as the input string
         * except that each ',' is replaced by a '\,'.
         * @param string the value to be encoded
         * @return the encoded value.
         */
        public String encode(final String string) {
            return String.join("\\,", string.split(","));
        }
    }

    // CheckStyle:VisibilityModifier OFF - bc

    /**
     * The alias of signer.
     */
    protected String alias;

    /**
     * The name of keystore file.
     */
    protected String keystore;
    protected String storepass;
    protected String storetype;
    protected String keypass;

    protected String sigalg;
    protected String keyalg;
    protected String saname;
    protected String dname;
    protected DistinguishedName expandedDname;
    protected int keysize;
    protected int validity;
    protected boolean verbose;
    // CheckStyle:VisibilityModifier ON

    /**
     * Distinguished name list.
     *
     * @return Distinguished name container.
     * @throws BuildException If specified more than once or dname
     *                        attribute is used.
     */
    public DistinguishedName createDname() throws BuildException {
        if (null != expandedDname) {
            throw new BuildException("DName sub-element can only be specified once.");
        }
        if (null != dname) {
            throw new BuildException(
                "It is not possible to specify dname  both as attribute and element.");
        }
        expandedDname = new DistinguishedName();
        return expandedDname;
    }

    /**
     * The distinguished name for entity.
     *
     * @param dname distinguished name
     */
    public void setDname(final String dname) {
        if (null != expandedDname) {
            throw new BuildException(
                "It is not possible to specify dname  both as attribute and element.");
        }
        this.dname = dname;
    }

    /**
     * The subject alternative name for entity.
     *
     * @param saname subject alternative name
     * @since Ant 1.9.14
     */
    public void setSaname(final String saname) {
        this.saname = saname;
    }

    /**
     * The alias to add under.
     *
     * @param alias alias to add under
     */
    public void setAlias(final String alias) {
        this.alias = alias;
    }

    /**
     * Keystore location.
     *
     * @param keystore location
     */
    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }

    /**
     * Password for keystore integrity.
     * Must be at least 6 characters long.
     * @param storepass password
     */
    public void setStorepass(final String storepass) {
        this.storepass = storepass;
    }

    /**
     * Keystore type.
     *
     * @param storetype type
     */
    public void setStoretype(final String storetype) {
        this.storetype = storetype;
    }

    /**
     * Password for private key (if different).
     *
     * @param keypass password
     */
    public void setKeypass(final String keypass) {
        this.keypass = keypass;
    }

    /**
     * The algorithm to use in signing.
     *
     * @param sigalg algorithm
     */
    public void setSigalg(final String sigalg) {
        this.sigalg = sigalg;
    }

    /**
     * The method to use when generating name-value pair.
     * @param keyalg algorithm
     */
    public void setKeyalg(final String keyalg) {
        this.keyalg = keyalg;
    }

    /**
     * Indicates the size of key generated.
     *
     * @param keysize size of key
     * @throws BuildException If not an Integer
     * @todo Could convert this to a plain Integer setter.
     */
    public void setKeysize(final String keysize) throws BuildException {
        try {
            this.keysize = Integer.parseInt(keysize);
        } catch (final NumberFormatException nfe) {
            throw new BuildException("KeySize attribute should be a integer");
        }
    }

    /**
     * Indicates how many days certificate is valid.
     *
     * @param validity days valid
     * @throws BuildException If not an Integer
     */
    public void setValidity(final String validity) throws BuildException {
        try {
            this.validity = Integer.parseInt(validity);
        } catch (final NumberFormatException nfe) {
            throw new BuildException("Validity attribute should be a integer");
        }
    }

    /**
     * If true, verbose output when signing.
     * @param verbose verbose or not
     */
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Execute the task.
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {

        if (null == alias) {
            throw new BuildException("alias attribute must be set");
        }

        if (null == storepass) {
            throw new BuildException("storepass attribute must be set");
        }

        if (null == dname && null == expandedDname) {
            throw new BuildException("dname must be set");
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("-genkey ");

        if (verbose) {
            sb.append("-v ");
        }

        sb.append("-alias \"");
        sb.append(alias);
        sb.append("\" ");

        if (null != dname) {
            sb.append("-dname \"");
            sb.append(dname);
            sb.append("\" ");
        }

        if (null != expandedDname) {
            sb.append("-dname \"");
            sb.append(expandedDname);
            sb.append("\" ");
        }

        if (null != keystore) {
            sb.append("-keystore \"");
            sb.append(keystore);
            sb.append("\" ");
        }

        if (null != storepass) {
            sb.append("-storepass \"");
            sb.append(storepass);
            sb.append("\" ");
        }

        if (null != storetype) {
            sb.append("-storetype \"");
            sb.append(storetype);
            sb.append("\" ");
        }

        sb.append("-keypass \"");
        if (null != keypass) {
            sb.append(keypass);
        } else {
            sb.append(storepass);
        }
        sb.append("\" ");

        if (null != sigalg) {
            sb.append("-sigalg \"");
            sb.append(sigalg);
            sb.append("\" ");
        }

        if (null != keyalg) {
            sb.append("-keyalg \"");
            sb.append(keyalg);
            sb.append("\" ");
        }

        if (0 < keysize) {
            sb.append("-keysize \"");
            sb.append(keysize);
            sb.append("\" ");
        }

        if (0 < validity) {
            sb.append("-validity \"");
            sb.append(validity);
            sb.append("\" ");
        }

        if (null != saname) {
            sb.append("-ext ");
            sb.append("\"san=");
            sb.append(saname);
            sb.append("\" ");
        }

        log("Generating Key for " + alias);
        final ExecTask cmd = new ExecTask(this);
        cmd.setExecutable(JavaEnvUtils.getJdkExecutable("keytool"));
        Commandline.Argument arg = cmd.createArg();
        arg.setLine(sb.toString());
        cmd.setFailonerror(true);
        cmd.setTaskName(getTaskName());
        cmd.execute();
    }
}
