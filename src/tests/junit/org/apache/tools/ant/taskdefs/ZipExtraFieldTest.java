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

package org.apache.tools.ant.taskdefs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.ZipResource;
import org.apache.tools.zip.JarMarker;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipExtraField;
import org.apache.tools.zip.ZipFile;

public class ZipExtraFieldTest extends TestCase {

    public void testPreservesExtraFields() throws IOException {
        File f = File.createTempFile("ziptest", ".zip");
        f.delete();
        ZipFile zf = null;
        try {
            Zip testInstance = new Zip();
            testInstance.setDestFile(f);
            final ZipResource r = new ZipResource() {
                    public String getName() {
                        return "x";
                    }
                    public boolean isExists() {
                        return true;
                    }
                    public boolean isDirectory() {
                        return false;
                    }
                    public long getLastModified() {
                        return 1;
                    }
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(new byte[0]);
                    }
                    public ZipExtraField[] getExtraFields() {
                        return new ZipExtraField[] {
                            new JarMarker()
                        };
                    }
                };
            testInstance.add(new ResourceCollection() {
                    public boolean isFilesystemOnly() { return false; }
                    public int size() { return 1; }
                    public Iterator iterator() {
                        ArrayList l = new ArrayList();
                        l.add(r);
                        return l.iterator();
                    }
                });
            testInstance.execute();

            zf = new ZipFile(f);
            ZipEntry ze = zf.getEntry("x");
            assertNotNull(ze);
            assertEquals(1, ze.getExtraFields().length);
            assertTrue(ze.getExtraFields()[0] instanceof JarMarker);
        } finally {
            ZipFile.closeQuietly(zf);
            if (f.exists()) {
                f.delete();
            }
        }
    }

}
