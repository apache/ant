/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types.selectors.modifiedselector;


import java.io.File;


/**
 * Computes a 'hashvalue' for the content of file using String.hashValue().
 * Use of this algorithm doesn´t require any additional nested <param>s and
 * doesn´t support any.
 *
 * @author Jan Matèrne
 * @version 2003-09-13
 * @since  Ant 1.6
 */
public class HashvalueAlgorithm implements Algorithm {

    /**
     * This algorithm doesn´t need any configuration.
     * Therefore it´s always valid.
     * @return always true
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Computes a 'hashvalue' for a file content.
     * It reads the content of a file, convert that to String and use the
     * String.hashCode() method.
     * @param file  The file for which the value should be computed
     * @return the hashvalue or <i>null</i> if the file couldn´t be read
     */
     // Because the content is only read the file will not be damaged. I tested
     // with JPG, ZIP and PDF as binary files.
    public String getValue(File file) {
        try {
            if (!file.canRead()) {
                return null;
            }
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] content = new byte[fis.available()];
            fis.read(content);
            fis.close();
            String s = new String(content);
            int hash = s.hashCode();
            return Integer.toString(hash);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Override Object.toString().
     * @return information about this comparator
     */
    public String toString() {
        return "HashvalueAlgorithm";
    }

}