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
import java.text.*;

/**
 * Sets TSTAMP, DSTAMP and TODAY
 *
 * @author costin@dnt.ro
 * @author stefano@apache.org
 * @author roxspring@yahoo.com
 * @author conor@cognet.com.au
 */
public class Tstamp extends Task {
    
    private Vector customFormats = new Vector();

    public void execute() throws BuildException {
        try {
            Date d = new Date();

            SimpleDateFormat dstamp = new SimpleDateFormat ("yyyyMMdd");
            project.setProperty("DSTAMP", dstamp.format(d));

            SimpleDateFormat tstamp = new SimpleDateFormat ("HHmm");
            project.setProperty("TSTAMP", tstamp.format(d));

            SimpleDateFormat today  = new SimpleDateFormat ("MMMM d yyyy", Locale.US);
            project.setProperty("TODAY", today.format(d));
            
            Enumeration i = customFormats.elements();
            while(i.hasMoreElements())
            {
                CustomFormat cts = (CustomFormat)i.nextElement();
                cts.execute(project,d, location);
            }
            
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
    
    public CustomFormat createFormat()
    {
        CustomFormat cts = new CustomFormat();
        customFormats.addElement(cts);
        return cts;
    }
    
    public class CustomFormat
    {
        private String propertyName;
        private String pattern;
        private int offset = 0;
        private int field = Calendar.DATE;
        
        public CustomFormat()
        {
        }
        
        public void setProperty(String propertyName)
        {
            this.propertyName = propertyName;
        }
        
        public void setPattern(String pattern)
        {
            this.pattern = pattern;
        }
        
        public void setOffset(int offset) {
            this.offset = offset;
        }
        
        public void setUnit(String unit) {
            if (unit.equalsIgnoreCase("millisecond")) {
                field = Calendar.MILLISECOND;
            }
            else if (unit.equalsIgnoreCase("second")) {
                field = Calendar.SECOND;
            }
            else if (unit.equalsIgnoreCase("minute")) {
                field = Calendar.MINUTE;
            }
            else if (unit.equalsIgnoreCase("hour")) {
                field = Calendar.HOUR_OF_DAY;
            }
            else if (unit.equalsIgnoreCase("day")) {
                field = Calendar.DATE;
            }
            else if (unit.equalsIgnoreCase("week")) {
                field = Calendar.WEEK_OF_YEAR;
            }
            else if (unit.equalsIgnoreCase("month")) {
                field = Calendar.MONTH;
            }
            else if (unit.equalsIgnoreCase("year")) {
                field = Calendar.YEAR;
            }
            else {
                throw new BuildException(unit + " is not a unit supported by the tstamp task");
            }
        }            
        
        public void execute(Project project, Date date, Location location)
        {
            if (propertyName == null) {
                throw new BuildException("property attribute must be provided", location);
            }
            
            if (pattern == null) {
                throw new BuildException("pattern attribute must be provided", location);
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat (pattern);
            if (offset != 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(field, offset);
                date = calendar.getTime();
            }

            project.setProperty(propertyName, sdf.format(date));
        }
    }
}
