/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.test;

import java.io.File;
import org.apache.myrmidon.AbstractProjectTest;
import org.apache.myrmidon.LogMessageTracker;

/**
 * Simple tests for the Ant1 Compatibility layer.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class Ant1CompatTestCase
    extends AbstractProjectTest
{
    public Ant1CompatTestCase( final String name )
    {
        super( name );
    }

    public void testBasic() throws Exception
    {
        final File projectFile = getTestResource( "basic-test.xml" );

        // <echo> test
        LogMessageTracker tracker = new LogMessageTracker();
        tracker.addExpectedMessage( "echo-test", "Hello, hello, hello" );
        executeTarget( projectFile, "echo-test", tracker );

        // Property resolution tests
        tracker = new LogMessageTracker();
        tracker.addExpectedMessage( "property-test", "prop-1 = [value-1]" );
        tracker.addExpectedMessage( "property-test", "prop-2 = [value-2]" );
        tracker.addExpectedMessage( "property-test", "prop-undefined = [${prop-undefined}]" );
        tracker.addExpectedMessage( "property-test", "Omit, replace$, but keep ${} and $" );
        executeTarget( projectFile, "property-test", tracker );
    }

    public void testIfUnless() throws Exception
    {
        final File projectFile = getTestResource( "if-unless-test.xml" );

        // if/unless tests.
        LogMessageTracker tracker = new LogMessageTracker();
        // Should pass if for "set", "true" and "false"
        tracker.addExpectedMessage( "if-set-test", "Ran target: if-set-test" );
        tracker.addExpectedMessage( "if-true-test", "Ran target: if-true-test" );
        tracker.addExpectedMessage( "if-false-test", "Ran target: if-false-test" );

        // Should only pass unless, when not defined.
        tracker.addExpectedMessage( "unless-unset-test",
                                    "Ran target: unless-unset-test" );

        // If combined with unless on a single target.
        tracker.addExpectedMessage( "if-with-unless-test-1",
                                    "Ran target: if-with-unless-test-1" );

        executeTarget( projectFile, "if-unless-tests", tracker );
    }

    public void testAntTask() throws Exception
    {
        final File projectFile = getTestResource( "ant-task-test.xml" );

        // TODO - Get the <ant> project listeners working, so we can test log messages.

        LogMessageTracker tracker = new LogMessageTracker();
        tracker.addExpectedMessage( "default-target", "In default target." );
        tracker.addExpectedMessage( "echo-test", "Hello, hello, hello" );
        //        executeTarget( projectFile, "ant-samefile-test", tracker );
        executeTarget( projectFile, "ant-samefile-test" );

        tracker = new LogMessageTracker();
        tracker.addExpectedMessage( "main",
                                    "Executed subdir/build.xml (default target)" );
        tracker.addExpectedMessage( "main",
                                    "Executed subdir/build.xml (default target)" );
        tracker.addExpectedMessage( "main",
                                    "Executed subdir/build.xml (default target)" );
        tracker.addExpectedMessage( "echo",
                                    "Executed subdir/build.xml (echo target)" );
        //        executeTarget( projectFile, "ant-otherfile-test", tracker );
        executeTarget( projectFile, "ant-otherfile-test" );

        tracker = new LogMessageTracker();
        tracker.addExpectedMessage( "property-test",
                                    "test-prop = [test-value]" );
        tracker.addExpectedMessage( "property-test",
                                    "test-prop = [set in calling task]" );
        tracker.addExpectedMessage( "property-test",
                                    "test-prop = [set in calling target]" );
        tracker.addExpectedMessage( "property-test",
                                       "test-prop = [test-value]" );
           //        executeTarget( projectFile, "ant-setprops-test", tracker );
        executeTarget( projectFile, "ant-setprops-test" );
    }

    public void testAntcallTask() throws Exception
    {
        final File projectFile = getTestResource( "antcall-task-test.xml" );

        // TODO - Get the <ant> project listeners working, so we can test log messages.

        LogMessageTracker tracker = new LogMessageTracker();
        tracker.addExpectedMessage( "default-target",
                                    "In default target." );
        tracker.addExpectedMessage( "antcall-target",
                                    "In antcall-target:  test-prop = [test-value]" );
        tracker.addExpectedMessage( "antcall-target",
                                    "In antcall-target:  test-prop = [set in calling task]" );
        tracker.addExpectedMessage( "antcall-target",
                                    "In antcall-target:  test-prop = [set in calling target]" );
        tracker.addExpectedMessage( "antcall-target",
                                    "In antcall-target:  test-prop = [test-value]" );
        //        executeTarget( projectFile, "ant-samefile-test", tracker );
        executeTarget( projectFile, "antcall-test" );
    }

}
