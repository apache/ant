package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InputTaskStubTest {

    private org.apache.tools.ant.Project project;
    private org.apache.tools.ant.taskdefs.Input inputTask;

    @Before
    public void setUp() {
        // Initialize Ant Project Environment and Input Task.
        project = new Project();
        project.init();
        inputTask = new Input();
        inputTask.setProject(project);
    }

    // ==========================================
    //  Stub (sub class)
    // ==========================================
    private class StubInputHandler implements InputHandler {
        private String hardcodedResponse;

        public StubInputHandler(String hardcodedResponse) {
            this.hardcodedResponse = hardcodedResponse;
        }

        @Override
        public void handleInput(InputRequest request) throws BuildException {
            // Instead of reading keyboard input, it directly stuffs a hard-coded fake answer into the request.
            request.setInput(hardcodedResponse);
        }
    }

    // ==========================================
    // Call New Tests for Stub
    // ==========================================
    @Test
    public void testInputTaskUsesStubbedHandler() {
        // 1. Set Input Task Variable
        inputTask.setMessage("Are you sure you want to deploy?");
        inputTask.setAddproperty("deploy.confirmation");

        // 2. Dependency Injection
        // Inject Stub into Project,replace Console InputHandler block
        project.setInputHandler(new StubInputHandler("yes"));

        // 3. Execution (call handleInput in Stub -> suppose not to block)
        inputTask.execute();

        // 4. Assertion
        // Check if Ant property Stub returns "yes"
        assertEquals("yes", project.getProperty("deploy.confirmation"));
    }
}