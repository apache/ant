package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

/**
 * A {@link TestResultFormatter} which prints a brief statistic for tests that have
 * failed, aborted or skipped
 */
class LegacyBriefResultFormatter extends LegacyPlainResultFormatter implements TestResultFormatter {

    @Override
    protected boolean shouldReportExecutionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult) {
        final TestExecutionResult.Status resultStatus = testExecutionResult.getStatus();
        return resultStatus == TestExecutionResult.Status.ABORTED || resultStatus == TestExecutionResult.Status.FAILED;
    }
}
