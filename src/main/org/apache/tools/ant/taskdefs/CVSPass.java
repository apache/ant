/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights 
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

/**
 * CVSLogin
 *
 * Adds an new entry to a CVS password file
 *
 * @author <a href="jeff@custommonkey.org">Jeff Martin</a>
 * @version $Revision$
 */
public class CVSPass extends Task {
    /** CVS Root */
    private String cvsRoot = null; 
    /** Password file to add password to */
    private File passFile = null;
    /** Password to add to file */
    private String password = null;
    /** End of line character */
    private final String EOL = System.getProperty("line.separator");

    /** Array contain char conversion data */
    char[] c=new char[128];

    public CVSPass(){
        passFile = new File(System.getProperty("user.home")+"/.cvspass");
        // Create lookup for password mangling
        c[32]='r'; c[33]='x'; c[34]='5'; c[35]='O'; c[37]='m';
        c[38]='H'; c[39]='l'; c[40]='F'; c[42]='L'; c[43]='C';
        c[44]='t'; c[45]='J'; c[46]='D'; c[47]='W'; c[48]='o';
        c[49]='4'; c[50]='K'; c[51]='w'; c[52]='1'; c[53]='"';
        c[54]='R'; c[55]='Q'; c[56]='_'; c[57]='A'; c[58]='p';
        c[59]='V'; c[60]='v'; c[61]='n'; c[62]='z'; c[63]='i';
        c[64]=')'; c[65]='9'; c[66]='S'; c[67]='+'; c[68]='.';
        c[69]='f'; c[70]='('; c[71]='Y'; c[72]='&'; c[73]='g';
        c[74]='-'; c[75]='2'; c[76]='*'; c[81]='7'; c[82]='6';
        c[83]='B'; c[86]=';'; c[87]='/'; c[89]='G'; c[90]='s';
        c[91]='N'; c[92]='X'; c[93]='k'; c[94]='j'; c[95]='%';
        c[97]='y'; c[98]='u'; c[99]='h'; c[100]='e'; c[101]='d';
        c[102]='E'; c[103]='I'; c[104]='c'; c[105]='?'; c[108]='\'';
        c[109]='%'; c[110]='='; c[111]='0'; c[112]=':'; c[113]='q';
        c[115]='Z'; c[116]=','; c[117]='b'; c[118]='<'; c[119]='3';
        c[120]='!'; c[121]='a'; c[122]='>'; c[123]='M'; c[124]='T';
        c[125]='P'; c[126]='U';
    }

    /**
     * Does the work.
     *
     * @exception BuildException if someting goes wrong with the build
     */
    public final void execute() throws BuildException {
        if(cvsRoot==null)throw new BuildException("cvsroot is required");
        if(password==null)throw new BuildException("password is required");

        log("cvsRoot: " + cvsRoot, project.MSG_DEBUG);
        log("password: " + password, project.MSG_DEBUG);
        log("passFile: " + passFile, project.MSG_DEBUG);

        try{
            StringBuffer buf = new StringBuffer();

            if(passFile.exists()){
                BufferedReader reader =
                    new BufferedReader(new FileReader(passFile));

                String line = null;

                while((line=reader.readLine())!=null){
                    if(!line.startsWith(cvsRoot)){
                        buf.append(line+EOL);
                    }
                }

            reader.close();
            }

            PrintWriter writer = new PrintWriter(new FileWriter(passFile));


            writer.print(buf.toString());
            writer.print(cvsRoot);
            writer.print(" A");
            writer.println(mangle(password));

            log("Writing -> " + buf.toString() + cvsRoot + " A" + mangle(password), project.MSG_DEBUG);

            writer.close();
        }catch(IOException e){
            throw new BuildException(e);
        }

    }

    private final String mangle(String password){
        StringBuffer buf = new StringBuffer();
        for(int i=0;i<password.length();i++){
            buf.append(c[password.charAt(i)]);
        }
        return buf.toString();
    }

    /**
     * Sets cvs root to be added to the password file
     */
    public void setCvsroot(String cvsRoot) {
        this.cvsRoot = cvsRoot;
    }

    /**
     * Sets the password file attribute.
     */
    public void setPassfile(File passFile) {
        this.passFile = passFile;
    }

    /**
     * Sets the password attribute.
     */
    public void setPassword(String password) {
        this.password = password;
    }

}
