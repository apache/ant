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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * Keyword substitution. Input file is written to output file.
 * Do not make input file same as output file.
 * Keywords in input files look like this: @foo@. See the docs for the
 * setKeys method to understand how to do the substitutions.
 *
 * @since Ant 1.1
 * @deprecated KeySubst is deprecated since Ant 1.1. Use Filter + Copy
 * instead.
 */
@Deprecated
public class KeySubst extends Task {
    private File source = null;
    private File dest = null;
    private String sep = "*";
    private Hashtable<String, String> replacements = new Hashtable<>();

    /**
     * Do the execution.
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        log("!! KeySubst is deprecated. Use Filter + Copy instead. !!");
        log("Performing Substitutions");
        if (source == null || dest == null) {
            log("Source and destinations must not be null");
            return;
        }
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new FileReader(source));
            dest.delete();
            bw = new BufferedWriter(new FileWriter(dest));

            String line = null;
            String newline = null;
            line = br.readLine();
            while (line != null) {
                if (!line.isEmpty()) {
                    newline = KeySubst.replace(line, replacements);
                    bw.write(newline);
                }
                bw.newLine();
                line = br.readLine();
            }
            bw.flush();
        } catch (IOException ioe) {
            log(StringUtils.getStackTrace(ioe), Project.MSG_ERR);
        } finally {
            FileUtils.close(bw);
            FileUtils.close(br);
        }
    }

    /**
     * Set the source file.
     * @param s the source file
     */
    public void setSrc(File s) {
        this.source = s;
    }

    /**
     * Set the destination file.
     * @param dest the destination file
     */
    public void setDest(File dest) {
        this.dest = dest;
    }

    /**
     * Sets the separator between name=value arguments
     * in setKeys(). By default it is "*".
     * @param sep the separator string
     */
    public void setSep(String sep) {
        this.sep = sep;
    }
    /**
     * Sets the keys.
     *
     * Format string is like this:
     *   <p>
     *   name=value*name2=value
     *   <p>
     *   Names are case sensitive.
     *   <p>
     *   Use the setSep() method to change the * to something else
     *   if you need to use * as a name or value.
     * @param keys a <code>String</code> value
     */
    public void setKeys(String keys) {
        if (keys != null && !keys.isEmpty()) {
            StringTokenizer tok =
            new StringTokenizer(keys, this.sep, false);
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken().trim();
                StringTokenizer itok =
                new StringTokenizer(token, "=", false);

                String name = itok.nextToken();
                String value = itok.nextToken();
                replacements.put(name, value);
            }
        }
    }


    /**
     * A test method.
     * @param args not used
     */
    public static void main(String[] args) {
        try {
            Hashtable<String, String> hash = new Hashtable<>();
            hash.put("VERSION", "1.0.3");
            hash.put("b", "ffff");
            System.out.println(KeySubst.replace("$f ${VERSION} f ${b} jj $",
                                                hash));
        } catch (Exception e) {
            e.printStackTrace(); //NOSONAR
        }
    }

    /**
     * Does replacement on text using the hashtable of keys.
     * @param origString an input string
     * @param keys       mapping of keys to values
     * @return the string with the replacements in it.
     * @throws BuildException on error
     */
    public static String replace(String origString, Hashtable<String, String> keys)
        throws BuildException {
        final StringBuilder finalString = new StringBuilder();
        int index = 0;
        int i = 0;
        String key = null;
        // CheckStyle:MagicNumber OFF
        while ((index = origString.indexOf("${", i)) > -1) {
            key = origString.substring(index + 2, origString.indexOf("}",
                                       index + 3));
            finalString.append(origString, i, index);
            if (keys.containsKey(key)) {
                finalString.append(keys.get(key));
            } else {
                finalString.append("${");
                finalString.append(key);
                finalString.append("}");
            }
            i = index + 3 + key.length();
        }
        // CheckStyle:MagicNumber ON
        finalString.append(origString.substring(i));
        return finalString.toString();
    }
}
