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

package org.apache.tools.ant.types.selectors.modifiedselector;


import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import org.apache.tools.ant.BuildException;


/**
 * Computes a 'hashvalue' for the content of file using
 * java.security.MessageDigest.
 * Use of this algorithm doesn't require any additional nested <param>s.
 * Supported <param>s are:
 * <table>
 * <tr>
 *   <th>name</th><th>values</th><th>description</th><th>required</th>
 * </tr>
 * <tr>
 *   <td> algorithm.algorithm </td>
 *   <td> MD5 | SHA (default provider) </td>
 *   <td> name of the algorithm the provider should use </td>
 *   <td> no, defaults to MD5 </td>
 * </tr>
 * <tr>
 *   <td> algorithm.provider </td>
 *   <td> </td>
 *   <td> name of the provider to use </td>
 *   <td> no, defaults to <i>null</i> </td>
 * </tr>
 * </table>
 *
 * @version 2004-07-08
 * @since  Ant 1.6
 */
public class DigestAlgorithm implements Algorithm {


    // -----  member variables  -----


    /**
     * MessageDigest algorithm to be used.
     */
    private String algorithm = "MD5";

    /**
     * MessageDigest Algorithm provider
     */
    private String provider = null;

    /**
     * Message Digest instance
     */
    private MessageDigest messageDigest = null;

    /**
     * Size of the read buffer to use.
     */
    private int readBufferSize = 8 * 1024;


    // -----  Algorithm-Configuration  -----


    /**
     * Specifies the algorithm to be used to compute the checksum.
     * Defaults to "MD5". Other popular algorithms like "SHA" may be used as well.
     * @param algorithm the digest algorithm to use
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }


    /**
     * Sets the MessageDigest algorithm provider to be used
     * to calculate the checksum.
     * @param provider provider to use
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }


    /** Initialize the security message digest. */
    public void initMessageDigest() {
        if (messageDigest != null) {
            return;
        }

        if ((provider != null) && !"".equals(provider) && !"null".equals(provider)) {
            try {
                messageDigest = MessageDigest.getInstance(algorithm, provider);
            } catch (NoSuchAlgorithmException noalgo) {
                throw new BuildException(noalgo);
            } catch (NoSuchProviderException noprovider) {
                throw new BuildException(noprovider);
            }
        } else {
            try {
                messageDigest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException noalgo) {
                throw new BuildException(noalgo);
            }
        }
    }


    // -----  Logic  -----


    /**
     * This algorithm supports only MD5 and SHA.
     * @return <i>true</i> if all is ok, otherwise <i>false</i>.
     */
    public boolean isValid() {
        return "SHA".equalsIgnoreCase(algorithm) || "MD5".equalsIgnoreCase(algorithm);
    }


    /**
     * Computes a value for a file content with the specified digest algorithm.
     * @param file    File object for which the value should be evaluated.
     * @return        The value for that file
     */
    // implementation adapted from ...taskdefs.Checksum, thanks to Magesh for hint
    public String getValue(File file) {
        initMessageDigest();
        String checksum = null;
        try {
            if (!file.canRead()) {
                return null;
            }
            FileInputStream fis = null;

            byte[] buf = new byte[readBufferSize];
            try {
                messageDigest.reset();
                fis = new FileInputStream(file);
                DigestInputStream dis = new DigestInputStream(fis,
                                                              messageDigest);
                while (dis.read(buf, 0, readBufferSize) != -1) {
                    // do nothing
                }
                dis.close();
                fis.close();
                fis = null;
                byte[] fileDigest = messageDigest.digest();
                StringBuffer checksumSb = new StringBuffer();
                for (int i = 0; i < fileDigest.length; i++) {
                    String hexStr = Integer.toHexString(0x00ff & fileDigest[i]);
                    if (hexStr.length() < 2) {
                        checksumSb.append("0");
                    }
                    checksumSb.append(hexStr);
                }
                checksum = checksumSb.toString();
            } catch (Exception e) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return checksum;
    }


    /**
     * Override Object.toString().
     * @return some information about this algorithm.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("<DigestAlgorithm:");
        buf.append("algorithm=").append(algorithm);
        buf.append(";provider=").append(provider);
        buf.append(">");
        return buf.toString();
    }
}
