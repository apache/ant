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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

package org.apache.ant.contrib;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

/**
 * Will set one of the given properties depending on the result of testing
 * the value of another property.
 *
 * <!ELEMENT case (when*, else) >
 * <!ATTLIST case property CDATA #REQUIRED > The name of the property to test
 * <!ELEMENT when EMPTY >
 * <!ATTLIST when value CDATA #REQUIRED > The value to compare and set prop.
 * <!ATTLIST when property CDATA #REQUIRED > The name of the property to set
 * <!ELEMENT else EMPTY >
 * <!ATTLIST else property CDATA #REQUIRED > The name of the property to set otherwise
 * <!ATTLIST else value CDATA #IMPLIED > The value to set; default "true".
 *
 * @author Jose Alberto Fernandez <a href="mailto:jfernandez@viquity.com">jfernandez@viquity.com</a>
 */

public class Case extends Task {

    public class When {
      private String property;
      private String value;

      public void setProperty(String name) {
        property = name;
      }

      public String getProperty() {
        return property;
      }

      public void setValue(String val) {
        value = val;
      }

      public String getValue() {
        return value;
      }

      public boolean tryCase(String caseValue) throws BuildException {
        if (property == null)
          throw new BuildException("Property attribute is mandatory");

        if (value == null)
          throw new BuildException("Value attribute is mandatory");

        if (!value.equals(caseValue)) return false;

        if (getProject().getProperty(property) == null) {
          getProject().setProperty(property, value);
        } else {
          log("Override ignored for " + property, Project.MSG_VERBOSE);
        }
        return true;
      }       

      public void doElse() throws BuildException {
        if (property == null)
          throw new BuildException("Property attribute is mandatory");

        String elseValue = (value == null) ? "true" : value;

        if (getProject().getProperty(property) == null) {
          getProject().setProperty(property, elseValue);
        } else {
          log("Override ignored for " + property, Project.MSG_VERBOSE);
        }
      }
    }
      
    private String caseProperty;
    private Vector whenList = new Vector();
    private When elseCase = null;

    public When createWhen() throws BuildException {
      When w = new When();
      whenList.addElement(w);
      return w;
    }

    public When createElse() throws BuildException {
      if (elseCase != null) 
        throw new BuildException("Only one else element allowed per case");

      return (elseCase = new When());
    }

    public void setProperty(String property) {
        this.caseProperty = property;
    }

    public void execute() throws BuildException {
        if (caseProperty == null) {
            throw new BuildException("property attribute is required", 
                                     location);
        }
        
        String caseValue = getProject().getProperty(caseProperty);

        for (Enumeration e = whenList.elements(); e.hasMoreElements(); ) {
          When w = (When)e.nextElement();
          if (w.tryCase(caseValue)) return;
        }
        
        if (elseCase != null)
          elseCase.doElse();
    }

}
