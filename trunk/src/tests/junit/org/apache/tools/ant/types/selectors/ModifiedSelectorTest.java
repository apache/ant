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

package org.apache.tools.ant.types.selectors;


// Java
import java.io.File;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.selectors.modifiedselector.Algorithm;
import org.apache.tools.ant.types.selectors.modifiedselector.Cache;
import org.apache.tools.ant.types.selectors.modifiedselector.ChecksumAlgorithm;
import org.apache.tools.ant.types.selectors.modifiedselector.DigestAlgorithm;
import org.apache.tools.ant.types.selectors.modifiedselector.EqualComparator;
import org.apache.tools.ant.types.selectors.modifiedselector.HashvalueAlgorithm;
import org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector;
import org.apache.tools.ant.types.selectors.modifiedselector.PropertiesfileCache;
import org.apache.tools.ant.util.FileUtils;


/**
 * Unit tests for ModifiedSelector.
 *
 * @since  Ant 1.6
 */
public class ModifiedSelectorTest extends BaseSelectorTest {

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    //  =====================  attributes  =====================


    /** Package of the CacheSelector classes. */
    private static String pkg = "org.apache.tools.ant.types.selectors.modifiedselector";

    /** Path where the testclasses are. */
    private Path testclasses = null;


    //  =====================  constructors, factories  =====================


    public ModifiedSelectorTest(String name) {
        super(name);
    }


    /**
     * Factory method from base class. This should be overriden in child
     * classes to return a specific Selector class (like here).
     */
    public BaseSelector getInstance() {
        return new ModifiedSelector();
    }


    //  =====================  JUnit stuff  =====================


    public void setUp() {
        // project reference is set in super.setUp()
        super.setUp();
        // init the testclasses path object
        Project prj = getProject();
        if (prj != null) {
            testclasses = new Path(prj, prj.getProperty("build.tests.value"));
        }
    }


    /* * /
    // for test only - ignore tests where we arent work at the moment
    public static junit.framework.Test suite() {
        junit.framework.TestSuite suite= new junit.framework.TestSuite();
        suite.addTest(new ModifiedSelectorTest("testValidateWrongCache"));
        return suite;
    }
    /* */


    // =======  testcases for the attributes and nested elements of the selector  =====


    /** Test right use of cache names. */
    public void testValidateWrongCache() {
        String name = "this-is-not-a-valid-cache-name";
        try {
            ModifiedSelector.CacheName cacheName = new ModifiedSelector.CacheName();
            cacheName.setValue(name);
            fail("CacheSelector.CacheName accepted invalid value.");
        } catch (BuildException be) {
            assertEquals(name + " is not a legal value for this attribute",
                         be.getMessage());
        }
    }


    /** Test right use of cache names. */
    public void testValidateWrongAlgorithm() {
        String name = "this-is-not-a-valid-algorithm-name";
        try {
            ModifiedSelector.AlgorithmName algoName
                = new ModifiedSelector.AlgorithmName();
            algoName.setValue(name);
            fail("CacheSelector.AlgorithmName accepted invalid value.");
        } catch (BuildException be) {
            assertEquals(name + " is not a legal value for this attribute",
                         be.getMessage());
        }
    }


    /** Test right use of comparator names. */
    public void testValidateWrongComparator() {
        String name = "this-is-not-a-valid-comparator-name";
        try {
            ModifiedSelector.ComparatorName compName
                = new ModifiedSelector.ComparatorName();
            compName.setValue(name);
            fail("ModifiedSelector.ComparatorName accepted invalid value.");
        } catch (BuildException be) {
            assertEquals(name + " is not a legal value for this attribute",
                         be.getMessage());
        }
    }


    public void testIllegalCustomAlgorithm() {
        try {
            String algo = getAlgoName("java.lang.Object");
            fail("Illegal classname used.");
        } catch (Exception e) {
            assertTrue("Wrong exception type: " + e.getClass().getName(), e instanceof BuildException);
            assertEquals("Wrong exception message.",
                         "Specified class (java.lang.Object) is not an Algorithm.",
                         e.getMessage());

        }
    }


    public void testNonExistentCustomAlgorithm() {
        boolean noExcThrown = false;
        try {
            String algo = getAlgoName("non.existent.custom.Algorithm");
            noExcThrown = true;
        } catch (Exception e) {
            if (noExcThrown) {
                fail("does 'non.existent.custom.Algorithm' really exist?");
            }
            assertTrue("Wrong exception type: " + e.getClass().getName(), e instanceof BuildException);
            assertEquals("Wrong exception message.",
                         "Specified class (non.existent.custom.Algorithm) not found.",
                         e.getMessage());

        }
    }


    public void testCustomAlgorithm() {
        String algo = getAlgoName("org.apache.tools.ant.types.selectors.modifiedselector.HashvalueAlgorithm");
        assertTrue("Wrong algorithm used: "+algo, algo.startsWith("HashvalueAlgorithm"));
    }


    public void testCustomAlgorithm2() {
        String algo = getAlgoName("org.apache.tools.ant.types.selectors.MockAlgorithm");
        assertTrue("Wrong algorithm used: "+algo, algo.startsWith("MockAlgorithm"));
    }


    public void testCustomClasses() {
        BFT bft = new BFT();
        bft.setUp();
        try {
            // do the actions
            bft.doTarget("modifiedselectortest-customClasses");
            // do the checks - the buildfile stores the fileset as property
            String fsFullValue = bft.getProperty("fs.full.value");
            String fsModValue  = bft.getProperty("fs.mod.value");

            assertNotNull("'fs.full.value' must be set.", fsFullValue);
            assertTrue("'fs.full.value' must not be null.", !"".equals(fsFullValue));
            assertTrue("'fs.full.value' must contain ant.bat.", fsFullValue.indexOf("ant.bat")>-1);

            assertNotNull("'fs.mod.value' must be set.", fsModValue);
            // must be empty according to the Mock* implementations
            assertTrue("'fs.mod.value' must be empty.", "".equals(fsModValue));
        // don't catch the JUnit exceptions
        } finally {
            bft.doTarget("modifiedselectortest-scenario-clean");
            bft.deletePropertiesfile();
            bft.tearDown();
        }
    }


    public void testDelayUpdateTaskFinished() {
        doDelayUpdateTest(1);
    }


    public void testDelayUpdateTargetFinished() {
        doDelayUpdateTest(2);
    }


    public void testDelayUpdateBuildFinished() {
        doDelayUpdateTest(3);
    }


    public void doDelayUpdateTest(int kind) {
        // no check for 1<=kind<=3 - only internal use therefore check it
        // while development

        // readable form of parameter kind
        String[] kinds = {"task", "target", "build"};

        // setup the "Ant project"
        MockProject project = new MockProject();
        File base  = new File("base");
        File file1 = new File("file1");
        File file2 = new File("file2");

        // setup the selector
        ModifiedSelector sel = new ModifiedSelector();
        sel.setProject(project);
        sel.setUpdate(true);
        sel.setDelayUpdate(true);
        // sorry - otherwise we will get a ClassCastException because the MockCache
        // is loaded by two different classloader ...
        sel.setClassLoader(this.getClass().getClassLoader());
        sel.addClasspath(testclasses);

        sel.setAlgorithmClass("org.apache.tools.ant.types.selectors.MockAlgorithm");
        sel.setCacheClass("org.apache.tools.ant.types.selectors.MockCache");
        sel.configure();

        // get the cache, so we can check our things
        MockCache cache = (MockCache)sel.getCache();

        // the test
        assertFalse("Cache must not be saved before 1st selection.", cache.saved);
        sel.isSelected(base, "file1", file1);
        assertFalse("Cache must not be saved after 1st selection.", cache.saved);
        sel.isSelected(base, "file2", file2);
        assertFalse("Cache must not be saved after 2nd selection.", cache.saved);
        switch (kind) {
            case 1 : project.fireTaskFinished();   break;
            case 2 : project.fireTargetFinished(); break;
            case 3 : project.fireBuildFinished();  break;
        }
        assertTrue("Cache must be saved after " + kinds[kind-1] + "Finished-Event.", cache.saved);

        // MockCache doesnt create a file - therefore no cleanup needed
    }


    /**
     * Extracts the real used algorithm name from the ModifiedSelector using
     * its toString() method.
     * @param classname  the classname from the algorithm to use
     * @return  the algorithm part from the toString() (without brackets)
     */
    private String getAlgoName(String classname) {
        ModifiedSelector sel = new ModifiedSelector();
        // add the test classes to its classpath
        sel.addClasspath(testclasses);
        sel.setAlgorithmClass(classname);
        // let the selector do its checks
        sel.validate();
        // extract the algorithm name (and config) from the selectors output
        String s1 = sel.toString();
        int posStart = s1.indexOf("algorithm=") + 10;
        int posEnd   = s1.indexOf(" comparator=");
        String algo  = s1.substring(posStart, posEnd);
        // '<' and '>' are only used if the algorithm has properties
        if (algo.startsWith("<")) algo = algo.substring(1);
        if (algo.endsWith(">"))   algo = algo.substring(0, algo.length()-1);
        // return the clean value
        return algo;
    }


    // ================  testcases for the cache implementations  ================


    /**
     * Propertycache must have a set 'cachefile' attribute.
     * The default in ModifiedSelector "cache.properties" is set by the selector.
     */
    public void testPropcacheInvalid() {
        Cache cache = new PropertiesfileCache();
        if (cache.isValid())
            fail("PropertyfilesCache does not check its configuration.");
    }


    public void testPropertyfileCache() {
        PropertiesfileCache cache = new PropertiesfileCache();
        File cachefile = new File("cache.properties");
        cache.setCachefile(cachefile);
        doTest(cache);
        assertFalse("Cache file not deleted.", cachefile.exists());
    }


    /** Checks whether a cache file is created. */
    public void testCreatePropertiesCacheDirect() {
        File cachefile = new File(basedir, "cachefile.properties");

        PropertiesfileCache cache = new PropertiesfileCache();
        cache.setCachefile(cachefile);

        cache.put("key", "value");
        cache.save();

        assertTrue("Cachefile not created.", cachefile.exists());

        cache.delete();
        assertFalse("Cachefile not deleted.", cachefile.exists());
    }


    /** Checks whether a cache file is created. */
    public void testCreatePropertiesCacheViaModifiedSelector() {
        File cachefile = new File(basedir, "cachefile.properties");
        try {

            // initialize test environment (called "bed")
            makeBed();

            // Configure the selector
            ModifiedSelector s = (ModifiedSelector)getSelector();
            s.setDelayUpdate(false);
            s.addParam("cache.cachefile", cachefile);

            ModifiedSelector.CacheName cacheName = new ModifiedSelector.CacheName();
            cacheName.setValue("propertyfile");
            s.setCache(cacheName);

            s.setUpdate(true);

            // does the selection
            String results = selectionString(s);

            // evaluate correctness
            assertTrue("Cache file is not created.", cachefile.exists());
        } finally {
            cleanupBed();
            if (cachefile!=null) cachefile.delete();
        }
    }


    /**
     * In earlier implementations there were problems with the <i>order</i>
     * of the <param>s. The scenario was <pre>
     *   <custom class="ModifiedSelector">
     *       <param name="cache.cachefile" value="mycache.properties" />
     *       <param name="cache" value="propertyfiles" />
     *   </custom>
     * </pre> It was important first to set the cache and then to set
     * the cache's configuration parameters. That results in the reorganized
     * configure() method of ModifiedSelector. This testcase tests that.
     */
    public void testCreatePropertiesCacheViaCustomSelector() {
        File cachefile = FILE_UTILS.createTempFile("tmp-cache-", ".properties", null);
        try {
            // initialize test environment (called "bed")
            makeBed();

            // Configure the selector

            ExtendSelector s = new ExtendSelector();
            s.setClassname("org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector");
            s.addParam(createParam("update", "true"));
            s.addParam(createParam("cache.cachefile", cachefile.getAbsolutePath()));
            s.addParam(createParam("cache", "propertyfile"));

            // does the selection
            String results = selectionString(s);

            // evaluate correctness
            assertTrue("Cache file is not created.", cachefile.exists());
        } finally {
            cleanupBed();
            if (cachefile!=null) cachefile.delete();
        }
    }


    public void _testCustomCache() {
        // same logic as on algorithm, no testcases created
    }


    /**
     * Test the interface semantic of Caches.
     * This method does some common test for cache implementations.
     * A cache must return a stored value and a valid iterator.
     * After calling the delete() the cache must be empty.
     *
     * @param algo   configured test object
     */
    protected void doTest(Cache cache) {
        assertTrue("Cache not proper configured.", cache.isValid());

        String key1   = "key1";
        String value1 = "value1";
        String key2   = "key2";
        String value2 = "value2";

        // given cache must be empty
        Iterator it1 = cache.iterator();
        assertFalse("Cache is not empty", it1.hasNext());

        // cache must return a stored value
        cache.put(key1, value1);
        cache.put(key2, value2);
        assertEquals("cache returned wrong value", value1, cache.get(key1));
        assertEquals("cache returned wrong value", value2, cache.get(key2));

        // test the iterator
        Iterator it2 = cache.iterator();
        Object   returned = it2.next();
        boolean ok = (key1.equals(returned) || key2.equals(returned));
        String msg = "Iterator returned unexpected value."
                   + "  key1.equals(returned)="+key1.equals(returned)
                   + "  key2.equals(returned)="+key2.equals(returned)
                   + "  returned="+returned
                   + "  ok="+ok;
        assertTrue(msg, ok);

        // clear the cache
        cache.delete();
        Iterator it3 = cache.iterator();
        assertFalse("Cache is not empty", it1.hasNext());
    }


    // ==============  testcases for the algorithm implementations  ==============


    public void testHashvalueAlgorithm() {
        HashvalueAlgorithm algo = new HashvalueAlgorithm();
        doTest(algo);
    }


    public void testDigestAlgorithmMD5() {
        DigestAlgorithm algo = new DigestAlgorithm();
        algo.setAlgorithm("MD5");
        doTest(algo);
    }


    public void testDigestAlgorithmSHA() {
        DigestAlgorithm algo = new DigestAlgorithm();
        algo.setAlgorithm("SHA");
        doTest(algo);
    }


    public void testChecksumAlgorithm() {
        ChecksumAlgorithm algo = new ChecksumAlgorithm();
        doTest(algo);
    }


    public void testChecksumAlgorithmCRC() {
        ChecksumAlgorithm algo = new ChecksumAlgorithm();
        algo.setAlgorithm("CRC");
        doTest(algo);
    }


    public void testChecksumAlgorithmAdler() {
        ChecksumAlgorithm algo = new ChecksumAlgorithm();
        algo.setAlgorithm("Adler");
        doTest(algo);
    }


    /**
     * Test the interface semantic of Algorithms.
     * This method does some common test for algorithm implementations.
     * An algorithm must return always the same value for the same file and
     * it must not return <i>null</i>.
     *
     * @param algo   configured test object
     */
    protected void doTest(Algorithm algo) {
        assertTrue("Algorithm not proper configured.", algo.isValid());
        try {
            makeBed();

            for (int i=0; i<files.length; i++) {
                File file = files[i];  // must not be a directory
                if (file.isFile()) {
                    // get the Hashvalues
                    String hash1 = algo.getValue(file);
                    String hash2 = algo.getValue(file);
                    String hash3 = algo.getValue(file);
                    String hash4 = algo.getValue(file);
                    String hash5 = algo.getValue(new File(file.getAbsolutePath()));

                    // Assert !=null and equality
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash1);
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash2);
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash3);
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash4);
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash5);
                    assertEquals("getHashvalue() returned different value for "+file.getAbsolutePath(), hash1, hash2);
                    assertEquals("getHashvalue() returned different value for "+file.getAbsolutePath(), hash1, hash3);
                    assertEquals("getHashvalue() returned different value for "+file.getAbsolutePath(), hash1, hash4);
                    assertEquals("getHashvalue() returned different value for "+file.getAbsolutePath(), hash1, hash5);
                }//if-isFile
            }//for
        } finally {
            cleanupBed();
        }
    }



    // ==============  testcases for the comparator implementations  ==============


    public void testEqualComparator() {
        EqualComparator comp = new EqualComparator();
        doTest(comp);
    }


    public void testRuleComparator() {
        RuleBasedCollator comp = (RuleBasedCollator)RuleBasedCollator.getInstance();
        doTest(comp);
    }


    public void testEqualComparatorViaSelector() {
        ModifiedSelector s = (ModifiedSelector)getSelector();
        ModifiedSelector.ComparatorName compName = new ModifiedSelector.ComparatorName();
        compName.setValue("equal");
        s.setComparator(compName);
        try {
            performTests(s, "TTTTTTTTTTTT");
        } finally {
            s.getCache().delete();
        }
    }


    public void _testRuleComparatorViaSelector() { //not yet supported see note in selector
        ModifiedSelector s = (ModifiedSelector)getSelector();
        ModifiedSelector.ComparatorName compName = new ModifiedSelector.ComparatorName();
        compName.setValue("rule");
        s.setComparator(compName);
        try {
            performTests(s, "TTTTTTTTTTTT");
        } finally {
            s.getCache().delete();
        }
    }


    public void _testCustomComparator() {
        // same logic as on algorithm, no testcases created
    }


    public void testResourceSelectorSimple() {
        BFT bft = new BFT("modifiedselector");
        bft.doTarget("modifiedselectortest-ResourceSimple");
        bft.deleteCachefile();
        //new File("src/etc/testcases/types/resources/selectors/cache.properties").delete();
    }
    public void testResourceSelectorSelresTrue() {
        BFT bft = new BFT("modifiedselector");
        bft.doTarget("modifiedselectortest-ResourceSelresTrue");
        bft.assertLogContaining("does not provide an InputStream");
        bft.deleteCachefile();
    }
    public void testResourceSelectorSelresFalse() {
        BFT bft = new BFT("modifiedselector");
        bft.doTarget("modifiedselectortest-ResourceSelresFalse");
        bft.deleteCachefile();
    }
    public void testResourceSelectorScenarioSimple() {
        BFT bft = new BFT("modifiedselector");
        bft.doTarget("modifiedselectortest-scenario-resourceSimple");
        bft.doTarget("modifiedselectortest-scenario-clean");
        bft.deleteCachefile();
    }
    /**
     * Test the interface semantic of Comparators.
     * This method does some common test for comparator implementations.
     *
     * @param algo   configured test object
     */
    protected void doTest(Comparator comp) {
        Object o1 = new String("string1");
        Object o2 = new String("string2");
        Object o3 = new String("string2"); // really "2"

        assertTrue("Comparator gave wrong value.", comp.compare(o1, o2) != 0);
        assertTrue("Comparator gave wrong value.", comp.compare(o1, o3) != 0);
        assertTrue("Comparator gave wrong value.", comp.compare(o2, o3) == 0);
    }


    // =====================  scenario tests  =====================


    /**
     * Tests whether the seldirs attribute is used.
     */
    public void testSeldirs() {
        ModifiedSelector s = (ModifiedSelector)getSelector();
        try {
            makeBed();

            StringBuffer sbTrue  = new StringBuffer();
            StringBuffer sbFalse = new StringBuffer();
            for (int i=0; i<filenames.length; i++) {
                if (files[i].isDirectory()) {
                    sbTrue.append("T");
                    sbFalse.append("F");
                } else {
                    sbTrue.append("T");
                    sbFalse.append("T");
                }
            }

            s.setSeldirs(true);
            performTests(s, sbTrue.toString());
            s.getCache().delete();

            s.setSeldirs(false);
            performTests(s, sbFalse.toString());
            s.getCache().delete();

        } finally {
            cleanupBed();
            if (s!=null) s.getCache().delete();
        }
    }


    /**
     * Complex test scenario using default values (DigestAlgorithm with MD5,
     * PropertiesfileCache with file=cache.properties, EqualComparator
     * and update=true). <ol>
     * <li> try fist time --> should select all </li>
     * <li> try second time --> should select no files (only directories) </li>
     * <li> modify timestamp of one file and content of a nother one </li>
     * <li> try third time --> should select only the file with modified
     *      content </li>
     */
    public void testScenario1() {
        BFT bft = null;
        ModifiedSelector s = null;
        try {
            //
            // *****  initialize test environment (called "bed")  *****
            //
            makeBed();
            String results = null;

            // Configure the selector - only defaults are used
            s = (ModifiedSelector)getSelector();

            //
            // *****  First Run  *****
            // the first call should get all files, because nothing is in
            // the cache
            //
            performTests(s, "TTTTTTTTTTTT");

            //
            // *****  Second Run  *****
            // the second call should get no files, because no content
            // has changed
            //
            performTests(s, "TFFFFFFFFFFT");

            //
            // *****  make some files dirty  *****
            //

            // these files are made dirty --> 3+4 with different content
            String f2name = "tar/bz2/asf-logo-huge.tar.bz2";
            String f3name = "asf-logo.gif.md5";
            String f4name = "copy.filterset.filtered";

            // AccessObject to the test-Ant-environment
            bft = new BFT();
            // give some values (via property file) to that environment
            bft.writeProperties("f2name="+f2name);
            bft.writeProperties("f3name="+f3name);
            bft.writeProperties("f4name="+f4name);
            // call the target for making the files dirty
            bft.doTarget("modifiedselectortest-makeDirty");

            //
            // *****  Third Run  *****
            // third call should get only those files, which CONTENT changed
            // (no timestamp changes required!)
            results = selectionString(s);

            //
            // *****  Check the result  *****
            //

            // Mark all files which should be selected as (T)rue and all others
            // as (F)alse. Directories are always selected so they always are
            // (T)rue.
            StringBuffer expected = new StringBuffer();
            for (int i=0; i<filenames.length; i++) {
                String ch = "F";
                if (files[i].isDirectory()) ch = "T";
                // f2name shouldn't be selected: only timestamp has changed!
                if (filenames[i].equalsIgnoreCase(f3name)) ch = "T";
                if (filenames[i].equalsIgnoreCase(f4name)) ch = "T";
                expected.append(ch);
            }

            assertEquals(
                "Wrong files selected. Differing files: "       // info text
                + resolve(diff(expected.toString(), results)),  // list of files
                expected.toString(),                            // expected result
                results                                         // result
            );

        } finally {
            // cleanup the environment
            cleanupBed();
            if (s!=null) s.getCache().delete();
            if (bft!=null) bft.deletePropertiesfile();
        }
    }


    /**
     * This scenario is based on scenario 1, but does not use any
     * default value and its based on <custom> selector. Used values are:<ul>
     * <li><b>Cache: </b> Propertyfile,
     *                    cachefile={java.io.tmpdir}/mycache.txt </li>
     * <li><b>Algorithm: </b> Digest
     *                    algorithm=SHA, Provider=null </li>
     * <li><b>Comparator: </b> java.text.RuleBasedCollator
     * <li><b>Update: </b> true </li>
     */
    public void _testScenario2() { // RuleBasedCollator not yet supported - see Selector:375 note
        ExtendSelector s = new ExtendSelector();
        BFT bft = new BFT();
        String cachefile = System.getProperty("java.io.tmpdir")+"/mycache.txt";
        try {
            makeBed();

            s.setClassname("org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector");

            s.addParam(createParam("cache.cachefile", cachefile));
            //s.addParam(createParam("algorithm.provider","---")); // i don't know any valid
            s.addParam(createParam("cache","propertyfile"));
            s.addParam(createParam("update","true"));
            s.addParam(createParam("comparator","rule"));
            s.addParam(createParam("algorithm.name","sha"));
            s.addParam(createParam("algorithm","digest"));

            // first and second run
            performTests(s, "TTTTTTTTTTTT");
            performTests(s, "TFFFFFFFFFFT");
            // make dirty
            String f2name = "tar/bz2/asf-logo-huge.tar.bz2";
            String f3name = "asf-logo.gif.md5";
            String f4name = "copy.filterset.filtered";
            bft.writeProperties("f2name="+f2name);
            bft.writeProperties("f3name="+f3name);
            bft.writeProperties("f4name="+f4name);
            bft.doTarget("modifiedselectortest-makeDirty");
            // third run
            String results = selectionString(s);
            StringBuffer expected = new StringBuffer();
            for (int i=0; i<filenames.length; i++) {
                String ch = "F";
                if (files[i].isDirectory()) ch = "T";
                if (filenames[i].equalsIgnoreCase(f3name)) ch = "T";
                if (filenames[i].equalsIgnoreCase(f4name)) ch = "T";
                expected.append(ch);
            }
            assertEquals(
                "Wrong files selected. Differing files: "       // info text
                + resolve(diff(expected.toString(), results)),  // list of files
                expected.toString(),                            // expected result
                results                                         // result
            );
        } finally {
            // cleanup the environment
            cleanupBed();
            (new java.io.File(cachefile)).delete();
            if (bft!=null) bft.deletePropertiesfile();
        }
    }


    public void testScenarioCoreSelectorDefaults() {
        doScenarioTest("modifiedselectortest-scenario-coreselector-defaults", "cache.properties");
    }


    public void testScenarioCoreSelectorSettings() {
        doScenarioTest("modifiedselectortest-scenario-coreselector-settings", "core.cache.properties");
    }


    public void testScenarioCustomSelectorSettings() {
        doScenarioTest("modifiedselectortest-scenario-customselector-settings", "core.cache.properties");
    }


    public void doScenarioTest(String target, String cachefilename) {
        BFT bft = new BFT();
        bft.setUp();
        File cachefile = new File(basedir, cachefilename);
        try {
            // do the actions
            bft.doTarget("modifiedselectortest-scenario-clean");
            bft.doTarget(target);

            // the directories to check
            File to1 = new File(basedir, "selectortest/to-1");
            File to2 = new File(basedir, "selectortest/to-2");
            File to3 = new File(basedir, "selectortest/to-3");

            // do the checks
            assertTrue("Cache file not created.", cachefile.exists());
            assertTrue("Not enough files copied on first time.", to1.list().length>5);
            assertTrue("Too much files copied on second time.", to2.list().length==0);
            assertTrue("Too much files copied on third time.", to3.list().length==2);
        // don't catch the JUnit exceptions
        } finally {
            bft.doTarget("modifiedselectortest-scenario-clean");
            bft.deletePropertiesfile();
            bft.tearDown();
            cachefile.delete();
        }
    }


    //  =====================  helper methods and classes  ====================


    /**
     * Creates a configured parameter object.
     * @param name   name of the parameter
     * @param value  value of the parameter
     * @return the parameter object
     */
    private Parameter createParam(String name, String value) {
        Parameter p = new Parameter();
        p.setName(name);
        p.setValue(value);
        return p;
    }


    /**
     * The BFT class wrapps the selector test-builfile inside an
     * ant project (BuildFileTest). It supports target execution
     * and property transfer to that project.
     */
    private class BFT extends org.apache.tools.ant.BuildFileTest {
        String buildfile = "src/etc/testcases/types/selectors.xml";

        BFT() { super("nothing"); }
        BFT(String name) {
            super(name);
        }

        String propfile = "ModifiedSelectorTest.properties";

        boolean isConfigured = false;

        public void setUp() {
            configureProject(buildfile);
            isConfigured = true;
        }


        /**
         * This stub teardown is here because the outer class needs to call the
         * tearDown method, and in the superclass it is protected.
         */
        public void tearDown() {
            try {
                super.tearDown();
            } catch (Exception e) {
                // ignore
            }
        }

        public void doTarget(String target) {
            if (!isConfigured) setUp();
            executeTarget(target);
        }

        public String getProperty(String property) {
            return project.getProperty(property);
        }

        public void writeProperties(String line) {
            if (!isConfigured) setUp();
            File dir = getProject().getBaseDir();
            File file = new File(dir, propfile);
            try {
                java.io.FileWriter out =
                    new java.io.FileWriter(file.getAbsolutePath(), true);
                out.write(line);
                out.write(System.getProperty("line.separator"));
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void deletePropertiesfile() {
            if (!isConfigured) setUp();
            new File(getProject().getBaseDir(), propfile).delete();
        }

        public void deleteCachefile() {
            File basedir = new File(buildfile).getParentFile();
            File cacheFile = new File(basedir, "cache.properties");
            cacheFile.delete();
        }

        public String getBuildfile() {
            return buildfile;
        }
        public void setBuildfile(String buildfile) {
            this.buildfile = buildfile;
        }
    }//class-BFT


    /**
     * MockProject wrappes a very small ant project (one target, one task)
     * but provides public methods to fire the build events.
     */
    private class MockProject extends Project {
        private Task   task;
        private Target target;

        public MockProject() {
            task = new Task(){
                public void execute() {
                }
            };
            task.setTaskName("testTask");
            target = new Target();
            target.setName("testTarget");
            target.setProject(this);
            target.addTask(task);
            task.setOwningTarget(target);
        }

        public void fireBuildFinished() {
            super.fireBuildFinished(null);
        }
        public void fireSubBuildFinished() {
            super.fireSubBuildFinished(null);
        }
        public void fireTargetStarted() {
            super.fireTargetStarted(target);
        }
        public void fireTargetFinished() {
            super.fireTargetFinished(target, null);
        }
        public void fireTaskStarted() {
            super.fireTaskStarted(task);
        }
        public void fireTaskFinished() {
            super.fireTaskFinished(task, null);
        }
    }//class-MockProject


}//class-ModifiedSelectorTest
