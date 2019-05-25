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

package org.apache.tools.ant.taskdefs.optional.testing;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.taskdefs.Parallel;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.taskdefs.WaitFor;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.util.WorkerAnt;

/**
 * Task to provide functional testing under Ant, with a fairly complex workflow of:
 *
 * <ul>
 * <li>Conditional execution</li>
 * <li>Application to start</li>
 * <li>A probe to "waitfor" before running tests</li>
 * <li>A tests sequence</li>
 * <li>A reporting sequence that runs after the tests have finished</li>
 * <li>A "teardown" clause that runs after the rest.</li>
 * <li>Automated termination of the program it executes, if a timeout is not met</li>
 * <li>Checking of a failure property and automatic raising of a fault
 *     (with the text in failureText)
 * if test shutdown and reporting succeeded</li>
 *  </ul>
 *
 * The task is designed to be framework neutral; it will work with JUnit,
 *  TestNG and other test frameworks That can be
 * executed from Ant. It bears a resemblance to the FunctionalTest task from
 * SmartFrog, as the attribute names were
 * chosen to make migration easier. However, this task benefits from the
 * ability to tweak Ant's internals, and so
 * simplify the workflow, and from the experience of using the SmartFrog task.
 * No code has been shared.
 *
 * @since Ant 1.8
 */

public class Funtest extends Task {
    /** {@value} */
    public static final String WARN_OVERRIDING = "Overriding previous definition of ";
    /** {@value} */
    public static final String APPLICATION_FORCIBLY_SHUT_DOWN = "Application forcibly shut down";
    /** {@value} */
    public static final String SHUTDOWN_INTERRUPTED = "Shutdown interrupted";
    /** {@value} */
    public static final String SKIPPING_TESTS
        = "Condition failed -skipping tests";
    /** Application exception : {@value} */
    public static final String APPLICATION_EXCEPTION = "Application Exception";
    /** Teardown exception : {@value} */
    public static final String TEARDOWN_EXCEPTION = "Teardown Exception";

    /**
     * A condition that must be true before the tests are run. This makes it
     * easier to define complex tests that only
     * run if certain conditions are met, such as OS or network state.
     */

    private NestedCondition condition;


    /**
     * Used internally to set the workflow up
     */
    private Parallel timedTests;

    /**
     * Setup runs if the condition is met. Once setup is complete, teardown
     * will be run when the task finishes
     */
    private Sequential setup;

    /**
     * The application to run
     */
    private Sequential application;

    /**
     * A block that halts the tests until met.
     */
    private BlockFor block;

    /**
     * Tests to run
     */
    private Sequential tests;

    /**
     * Reporting only runs if the tests were executed. If the block stopped
     * them, reporting is skipped.
     */
    private Sequential reporting;

    /**
     * Any teardown operations.
     */
    private Sequential teardown;

    /**
     * time for the tests to time out
     */
    private long timeout;

    private long timeoutUnitMultiplier = WaitFor.ONE_MILLISECOND;

    /**
     * time for the execution to time out.
     */
    private long shutdownTime = 10 * WaitFor.ONE_SECOND;

    private long shutdownUnitMultiplier = WaitFor.ONE_MILLISECOND;

    /**
     * Name of a property to look for
     */
    private String failureProperty;

    /**
     * Message to send when tests failed
     */
    private String failureMessage = "Tests failed";

    /**
     * Flag to set to true if you don't care about any shutdown errors.
     * <p/>
     * In that situation, errors raised during teardown are logged but not
     * turned into BuildFault events. Similar to catching and ignoring
     * <code>finally {}</code> clauses in Java/
     */
    private boolean failOnTeardownErrors = true;


    /**
     * What was thrown in the test run (including reporting)
     */
    private BuildException testException;
    /**
     * What got thrown during teardown
     */
    private BuildException teardownException;

    /**
     * Did the application throw an exception
     */
    private BuildException applicationException;

    /**
     * Did the task throw an exception
     */
    private BuildException taskException;

    /**
     * Log if the definition is overriding something
     *
     * @param name       what is being defined
     * @param definition what should be null if you don't want a warning
     */
    private void logOverride(String name, Object definition) {
        if (definition != null) {
            log(WARN_OVERRIDING + '<' + name + '>', Project.MSG_INFO);
        }
    }

    /**
      * Add a condition element.
      * @return <code>ConditionBase</code>.
      * @since Ant 1.6.2
      */
     public ConditionBase createCondition() {
        logOverride("condition", condition);
        condition = new NestedCondition();
        return condition;
    }

    /**
     * Add an application.
     * @param sequence the application to add.
     */
    public void addApplication(Sequential sequence) {
        logOverride("application", application);
        application = sequence;
    }

    /**
     * Add a setup sequence.
     * @param sequence the setup sequence to add.
     */
    public void addSetup(Sequential sequence) {
        logOverride("setup", setup);
        setup = sequence;
    }

    /**
     * Add a block.
     * @param sequence the block for to add.
     */
    public void addBlock(BlockFor sequence) {
        logOverride("block", block);
        block = sequence;
    }

    /**
     * add tests.
     * @param sequence a sequence to add.
     */
    public void addTests(Sequential sequence) {
        logOverride("tests", tests);
        tests = sequence;
    }

    /**
     * set reporting sequence of tasks.
     * @param sequence a reporting sequence to use.
     */
    public void addReporting(Sequential sequence) {
        logOverride("reporting", reporting);
        reporting = sequence;
    }

    /**
     * set teardown sequence of tasks.
     * @param sequence a teardown sequence to use.
     */
    public void addTeardown(Sequential sequence) {
        logOverride("teardown", teardown);
        teardown = sequence;
    }

    /**
     * Set the failOnTeardownErrors attribute.
     * @param failOnTeardownErrors the value to use.
     */
    public void setFailOnTeardownErrors(boolean failOnTeardownErrors) {
        this.failOnTeardownErrors = failOnTeardownErrors;
    }

    /**
     * Set the failureMessage attribute.
     * @param failureMessage the value to use.
     */
    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    /**
     * Set the failureProperty attribute.
     * @param failureProperty the value to use.
     */
    public void setFailureProperty(String failureProperty) {
        this.failureProperty = failureProperty;
    }

    /**
     * Set the shutdownTime attribute.
     * @param shutdownTime the value to use.
     */
    public void setShutdownTime(long shutdownTime) {
        this.shutdownTime = shutdownTime;
    }

    /**
     * Set the timeout attribute.
     * @param timeout the value to use.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Set the timeoutunit attribute.
     * @param unit the value to use.
     */
    public void setTimeoutUnit(WaitFor.Unit unit) {
        timeoutUnitMultiplier = unit.getMultiplier();
    }

    /**
     * Set the shutdownunit attribute.
     * @param unit the value to use.
     */
    public void setShutdownUnit(WaitFor.Unit unit) {
        shutdownUnitMultiplier = unit.getMultiplier();
    }


    /**
     * Get the application exception.
     * @return the application exception.
     */
    public BuildException getApplicationException() {
        return applicationException;
    }

    /**
     * Get the teardown exception.
     * @return the teardown exception.
     */
    public BuildException getTeardownException() {
        return teardownException;
    }

    /**
     * Get the test exception.
     * @return the test exception.
     */
    public BuildException getTestException() {
        return testException;
    }

    /**
     * Get the task exception.
     * @return the task exception.
     */
    public BuildException getTaskException() {
        return taskException;
    }

    /**
     * Bind and initialise a task
     * @param task task to bind
     */
    private void bind(Task task) {
        task.bindToOwner(this);
        task.init();
    }

    /**
     * Create a newly bound parallel instance
     * @param parallelTimeout timeout
     * @return a bound and initialised parallel instance.
     */
    private Parallel newParallel(long parallelTimeout) {
        Parallel par = new Parallel();
        bind(par);
        par.setFailOnAny(true);
        par.setTimeout(parallelTimeout);
        return par;
    }

    /**
     * Create a newly bound parallel instance with one child
     * @param parallelTimeout timeout
     * @param child task
     * @return a bound and initialised parallel instance.
     */
    private Parallel newParallel(long parallelTimeout, Task child) {
        Parallel par = newParallel(parallelTimeout);
        par.addTask(child);
        return par;
    }

    /**
     * Add any task validation needed to ensure internal code quality
     * @param task task
     * @param role role of the task
     */
    private void validateTask(Task task, String role) {
        if (task != null && task.getProject() == null) {
            throw new BuildException("%s task is not bound to the project %s",
                role, task);
        }
    }

    /**
     * Run the functional test sequence.
     * <p>
     * This is a fairly complex workflow -what is going on is that we try to clean up
     * no matter how the run ended, and to retain the innermost exception that got thrown
     * during cleanup. That is, if teardown fails after the tests themselves failed, it is the
     * test failing that is more important.
     * @throws BuildException if something was caught during the run or teardown.
     */
    @Override
    public void execute() throws BuildException {

        //validation
        validateTask(setup, "setup");
        validateTask(application, "application");
        validateTask(tests, "tests");
        validateTask(reporting, "reporting");
        validateTask(teardown, "teardown");

        //check the condition
        //and bail out if it is defined but not true
        if (condition != null && !condition.eval()) {
            //we are skipping the test
            log(SKIPPING_TESTS);
            return;
        }

        long timeoutMillis = timeout * timeoutUnitMultiplier;

        //set up the application to run in a separate thread
        Parallel applicationRun = newParallel(timeoutMillis);
        //with a worker which we can use to manage it
        WorkerAnt worker = new WorkerAnt(applicationRun, null);
        if (application != null) {
            applicationRun.addTask(application);
        }

        //The test run consists of the block followed by the tests.
        long testRunTimeout = 0;
        Sequential testRun = new Sequential();
        bind(testRun);
        if (block != null) {
            //waitfor is not a task, it needs to be adapted
            TaskAdapter ta = new TaskAdapter(block);
            ta.bindToOwner(this);
            validateTask(ta, "block");
            testRun.addTask(ta);
            //add the block time to the total test run timeout
            testRunTimeout = block.calculateMaxWaitMillis();
        }

        //add the tests and more delay
        if (tests != null) {
            testRun.addTask(tests);
            testRunTimeout += timeoutMillis;
        }
        //add the reporting and more delay
        if (reporting != null) {
            testRun.addTask(reporting);
            testRunTimeout += timeoutMillis;
        }

        //wrap this in a parallel purely to set up timeouts for the
        //test run
        timedTests = newParallel(testRunTimeout, testRun);

        try {
            //run any setup task
            if (setup != null) {
                Parallel setupRun = newParallel(timeoutMillis, setup);
                setupRun.execute();
            }
            //start the worker thread and leave it running
            worker.start();
            //start the probe+test sequence
            timedTests.execute();
        } catch (BuildException e) {
            //Record the exception and continue
            testException = e;
        } finally {
            //teardown always runs; its faults are filed away
            if (teardown != null) {
                try {
                    Parallel teardownRun = newParallel(timeoutMillis, teardown);
                    teardownRun.execute();
                } catch (BuildException e) {
                    teardownException = e;
                }
            }
        }

        //we get here whether or not the tests/teardown have thrown a BuildException.
        //do a forced shutdown of the running application, before processing the faults

        try {
            //wait for the worker to have finished
            long shutdownTimeMillis = shutdownTime * shutdownUnitMultiplier;
            worker.waitUntilFinished(shutdownTimeMillis);
            if (worker.isAlive()) {
                //then, if it is still running, interrupt it a second time.
                log(APPLICATION_FORCIBLY_SHUT_DOWN, Project.MSG_WARN);
                worker.interrupt();
                worker.waitUntilFinished(shutdownTimeMillis);
            }
        } catch (InterruptedException e) {
            //success, something interrupted the shutdown. There may be a leaked
            //worker;
            log(SHUTDOWN_INTERRUPTED, e, Project.MSG_VERBOSE);
        }
        applicationException = worker.getBuildException();

        //Now faults are analysed

        processExceptions();
    }

    /**
     * Now faults are analysed.
     * <p>The priority is</p>
     * <ol>
     * <li>testexceptions, except those indicating a build timeout when the application itself
     * failed. (Because often it is the application fault that is more interesting than the probe
     * failure, which is usually triggered by the application not starting.)</li>
     * <li>Application exceptions (above test timeout exceptions)</li>
     * <li>Teardown exceptions -except when they are being ignored</li>
     * <li>Test failures as indicated by the failure property</li>
     * </ol>
     */
    protected void processExceptions() {
        taskException = testException;

        //look for an application fault
        if (applicationException != null) {
            if (taskException == null || taskException instanceof BuildTimeoutException) {
                taskException = applicationException;
            } else {
                ignoringThrowable(APPLICATION_EXCEPTION, applicationException);
            }
        }

        //now look for teardown faults, which may be ignored
        if (teardownException != null) {
            if (taskException == null && failOnTeardownErrors) {
                taskException = teardownException;
            } else {
                //don't let the cleanup exception get in the way of any other failure
                ignoringThrowable(TEARDOWN_EXCEPTION, teardownException);
            }
        }

        //now, analyse the tests
        if (failureProperty != null
             && getProject().getProperty(failureProperty) != null) {
            //we've failed
            log(failureMessage);
            if (taskException == null) {
                taskException = new BuildException(failureMessage);
            }
        }

        //at this point taskException is null or not.
        //if not, throw the exception
        if (taskException != null) {
            throw taskException;
        }
    }

    /**
     * log that we are ignoring something rather than rethrowing it.
     * @param type name of exception
     * @param thrown what was thrown
     */
    protected void ignoringThrowable(String type, Throwable thrown) {
        log(type + ": " + thrown.toString(),
                thrown,
                Project.MSG_WARN);
    }

    private static class NestedCondition extends ConditionBase implements Condition {
        @Override
        public boolean eval() {
            if (countConditions() != 1) {
                throw new BuildException(
                    "A single nested condition is required.");
            }
            return getConditions().nextElement().eval();
        }
    }
}
