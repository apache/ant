/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import java.io.*;
import java.util.*;

/**
 * Keyword substitution. Input file is written to output file.
 * Do not make input file same as output file.
 * Keywords in input files look like this: @foo@. See the docs for the 
 * setKeys method to understand how to do the substitutions.
 *
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 *
 * @deprecated KeySubst is deprecated. Use Filter + CopyDir instead.
 */
public class KeySubst extends Task {
    private File source = null;
    private File dest = null;
    private String sep = "*";
    private Hashtable replacements = new Hashtable();
    
    /**
        Do the execution.
    */
    public void execute() throws BuildException {
        log("!! KeySubst is deprecated. Use Filter + CopyDir instead. !!");
        log("Performing Substitions");
        if ( source == null || dest == null ) {
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
            int length;
            line = br.readLine();
            while (line != null) {
                if ( line.length() == 0 ) {
                    bw.newLine();
                } else {
                    newline = KeySubst.replace ( line, replacements );
                    bw.write ( newline );
                    bw.newLine();
                }
                line = br.readLine();
            }
            bw.flush();
            bw.close();
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }       
    }
    /**
        Set the source file.
    */
    public void setSrc(String s) {
        this.source=project.resolveFile(s);
    }

    /**
        Set the destination file.
    */
    public void setDest(String dest) {
        this.dest = project.resolveFile(dest);
    }

    /**
        Sets the seperator between name=value arguments
        in setKeys(). By default it is "*".
    */
    public void setSep(String sep) {
        this.sep = sep;
    }
    /**
        Format string is like this:
        <p>
        name=value*name2=value
        <p>
        Names are case sensitive.
        <p>
        Use the setSep() method to change the * to something else
        if you need to use * as a name or value.
    */
    public void setKeys(String keys) {
        if (keys != null && keys.length() > 0) {
            StringTokenizer tok =
            new StringTokenizer(keys, this.sep, false);
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken().trim();
                StringTokenizer itok =
                new StringTokenizer(token, "=", false);
                
                String name = itok.nextToken();
                String value = itok.nextToken();
//                log ( "Name: " + name );
//                log ( "Value: " + value );
                replacements.put ( name, value );
            }
        }
    }
        

    public static void main(String[] args)
    {
        try{
        Hashtable hash = new Hashtable();
        hash.put ( "VERSION", "1.0.3" );
        hash.put ( "b", "ffff" );
        System.out.println ( KeySubst.replace ( "$f ${VERSION} f ${b} jj $", hash ) );
        }catch ( Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
        Does replacement on text using the hashtable of keys.
        
        @returns the string with the replacements in it.
    */
    public static String replace ( String origString, Hashtable keys )
        throws BuildException
    {
        StringBuffer finalString=new StringBuffer();
        int index=0;
        int i = 0;
        String key = null;
        while ((index = origString.indexOf("${", i)) > -1) {
            key = origString.substring(index + 2, origString.indexOf("}", index+3));
            finalString.append (origString.substring(i, index));
            if ( keys.containsKey ( key ) ) {
                finalString.append (keys.get(key));
            } else {
                finalString.append ( "${" );
                finalString.append ( key );
                finalString.append ( "}" );
            }
            i = index + 3 + key.length();
        }
        finalString.append (origString.substring(i));
        return finalString.toString();
    }
}
