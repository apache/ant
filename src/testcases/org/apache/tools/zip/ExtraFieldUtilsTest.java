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

package org.apache.tools.zip;

import junit.framework.TestCase;

/**
 * JUnit 3 testcases for org.apache.tools.zip.ExtraFieldUtils.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class ExtraFieldUtilsTest extends TestCase implements UnixStat {
    public ExtraFieldUtilsTest(String name) {
        super(name);
    }

    private AsiExtraField a;
    private UnrecognizedExtraField dummy;
    private byte[] data;
    private byte[] aLocal;

    public void setUp() {
        a = new AsiExtraField();
        a.setMode(0755);
        a.setDirectory(true);
        dummy = new UnrecognizedExtraField();
        dummy.setHeaderId(new ZipShort(1));
        dummy.setLocalFileDataData(new byte[0]);
        dummy.setCentralDirectoryData(new byte[] {0});

        aLocal = a.getLocalFileDataData();
        byte[] dummyLocal = dummy.getLocalFileDataData();
        data = new byte[4 + aLocal.length + 4 + dummyLocal.length];
        System.arraycopy(a.getHeaderId().getBytes(), 0, data, 0, 2);
        System.arraycopy(a.getLocalFileDataLength().getBytes(), 0, data, 2, 2);
        System.arraycopy(aLocal, 0, data, 4, aLocal.length);
        System.arraycopy(dummy.getHeaderId().getBytes(), 0, data, 
                         4+aLocal.length, 2);
        System.arraycopy(dummy.getLocalFileDataLength().getBytes(), 0, data, 
                         4+aLocal.length+2, 2);
        System.arraycopy(dummyLocal, 0, data, 
                         4+aLocal.length+4, dummyLocal.length);

    }

    /**
     * test parser.
     */
    public void testParse() throws Exception {
        ZipExtraField[] ze = ExtraFieldUtils.parse(data);
        assertEquals("number of fields", 2, ze.length);
        assertTrue("type field 1", ze[0] instanceof AsiExtraField);
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertTrue("type field 2", ze[1] instanceof UnrecognizedExtraField);
        assertEquals("data length field 2", 0, 
                     ze[1].getLocalFileDataLength().getValue());

        byte[] data2 = new byte[data.length-1];
        System.arraycopy(data, 0, data2, 0, data2.length);
        try {
            ExtraFieldUtils.parse(data2);
            fail("data should be invalid");
        } catch (Exception e) {
            assertEquals("message", 
                         "data starting at "+(4+aLocal.length)+" is in unknown format",
                         e.getMessage());
        }
    }

    /**
     * Test merge methods
     */
    public void testMerge() {
        byte[] local = 
            ExtraFieldUtils.mergeLocalFileDataData(new ZipExtraField[] {a, dummy});
        assertEquals("local length", data.length, local.length);
        for (int i=0; i<local.length; i++) {
            assertEquals("local byte "+i, data[i], local[i]);
        }
        
        byte[] dummyCentral = dummy.getCentralDirectoryData();
        byte[] data2 = new byte[4 + aLocal.length + 4 + dummyCentral.length];
        System.arraycopy(data, 0, data2, 0, 4 + aLocal.length + 2);
        System.arraycopy(dummy.getCentralDirectoryLength().getBytes(), 0, 
                         data2, 4+aLocal.length+2, 2);
        System.arraycopy(dummyCentral, 0, data2, 
                         4+aLocal.length+4, dummyCentral.length);


        byte[] central = 
            ExtraFieldUtils.mergeCentralDirectoryData(new ZipExtraField[] {a, dummy});
        assertEquals("central length", data2.length, central.length);
        for (int i=0; i<central.length; i++) {
            assertEquals("central byte "+i, data2[i], central[i]);
        }
        
    }
}
