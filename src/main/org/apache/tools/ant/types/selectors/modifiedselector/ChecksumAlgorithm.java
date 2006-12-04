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


import java.util.zip.Checksum;
import java.util.zip.CRC32;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.security.NoSuchAlgorithmException;
import org.apache.tools.ant.BuildException;


/**
 * Computes a 'checksum' for the content of file using
 * java.util.zip.CRC32 and java.util.zip.Adler32.
 * Use of this algorithm doesn't require any additional nested <param>s.
 * Supported <param>s are:
 * <table>
 * <tr>
 *   <th>name</th><th>values</th><th>description</th><th>required</th>
 * </tr>
 * <tr>
 *   <td> algorithm.algorithm </td>
 *   <td> ADLER | CRC ( default ) </td>
 *   <td> name of the algorithm the checksum should use </td>
 *   <td> no, defaults to CRC </td>
 * </tr>
 * </table>
 *
 * @version 2004-06-17
 * @since  Ant 1.7
 */
public class ChecksumAlgorithm implements Algorithm {


    // -----  member variables  -----


    /**
     * Checksum algorithm to be used.
     */
    private String algorithm = "CRC";

    /**
     * Checksum interface instance
     */
    private Checksum checksum = null;


    // -----  Algorithm-Configuration  -----


    /**
     * Specifies the algorithm to be used to compute the checksum.
     * Defaults to "CRC". Other popular algorithms like "ADLER" may be used as well.
     * @param algorithm the digest algorithm to use
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }


    /** Initialize the checksum interface. */
    public void initChecksum() {
        if (checksum != null) {
            return;
        }
        if ("CRC".equalsIgnoreCase(algorithm)) {
            checksum = new CRC32();
        } else if ("ADLER".equalsIgnoreCase(algorithm)) {
            checksum = new Adler32();
        } else {
            throw new BuildException(new NoSuchAlgorithmException());
        }
    }


    // -----  Logic  -----


    /**
     * This algorithm supports only CRC and Adler.
     * @return <i>true</i> if all is ok, otherwise <i>false</i>.
     */
    public boolean isValid() {
        return "CRC".equalsIgnoreCase(algorithm) || "ADLER".equalsIgnoreCase(algorithm);
    }


    /**
     * Computes a value for a file content with the specified checksum algorithm.
     * @param file    File object for which the value should be evaluated.
     * @return        The value for that file
     */
    public String getValue(File file) {
        initChecksum();
        String rval = null;

        try {
            if (file.canRead()) {
                 checksum.reset();
                 FileInputStream fis = new FileInputStream(file);
                 CheckedInputStream check = new CheckedInputStream(fis, checksum);
                 BufferedInputStream in = new BufferedInputStream(check);
                 while (in.read() != -1) {
                     // Read the file
                 }
                 rval = Long.toString(check.getChecksum().getValue());
                 in.close();
            }
        } catch (Exception e) {
            rval = null;
        }
        return rval;
    }


    /**
     * Override Object.toString().
     * @return some information about this algorithm.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("<ChecksumAlgorithm:");
        buf.append("algorithm=").append(algorithm);
        buf.append(">");
        return buf.toString();
    }
}
