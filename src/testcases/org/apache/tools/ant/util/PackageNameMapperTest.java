package org.apache.tools.ant.util;

import java.io.File;
import junit.framework.TestCase;

public class PackageNameMapperTest extends TestCase {
    public PackageNameMapperTest(String name) { super(name); }
    
    public void testMapping() {
        PackageNameMapper mapper = new PackageNameMapper();
        mapper.setFrom("*.java");
        mapper.setTo("TEST-*.xml");
        String file = fixupPath("org/apache/tools/ant/util/PackageNameMapperTest.java");
        String result = mapper.mapFileName(file)[0];
        
        assertEquals("TEST-org.apache.tools.ant.util.PackageNameMapperTest.xml",
          result);
    }
    
    private String fixupPath(String file) {
        return file.replace('/', File.separatorChar);
    }
}
