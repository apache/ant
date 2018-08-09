package org.apache.tools.ant.taskdefs.optional.junitlauncher;

/**
 * Constants used within the junitlauncher task
 */
final class Constants {

    static final int FORK_EXIT_CODE_SUCCESS = 0;
    static final int FORK_EXIT_CODE_EXCEPTION = 1;
    static final int FORK_EXIT_CODE_TESTS_FAILED = 2;
    static final int FORK_EXIT_CODE_TIMED_OUT = 3;

    static final String ARG_PROPERTIES = "--properties";
    static final String ARG_LAUNCH_DEFINITION = "--launch-definition";


    static final String LD_XML_ELM_LAUNCH_DEF = "launch-definition";
    static final String LD_XML_ELM_TEST = "test";
    static final String LD_XML_ELM_TEST_CLASSES = "test-classes";
    static final String LD_XML_ATTR_HALT_ON_FAILURE = "haltOnFailure";
    static final String LD_XML_ATTR_OUTPUT_DIRECTORY = "outDir";
    static final String LD_XML_ATTR_INCLUDE_ENGINES = "includeEngines";
    static final String LD_XML_ATTR_EXCLUDE_ENGINES = "excludeEngines";
    static final String LD_XML_ATTR_CLASS_NAME = "classname";
    static final String LD_XML_ATTR_METHODS = "methods";
    static final String LD_XML_ATTR_PRINT_SUMMARY = "printSummary";
    static final String LD_XML_ELM_LISTENER = "listener";
    static final String LD_XML_ATTR_SEND_SYS_ERR = "sendSysErr";
    static final String LD_XML_ATTR_SEND_SYS_OUT = "sendSysOut";
    static final String LD_XML_ATTR_LISTENER_RESULT_FILE = "resultFile";


    private Constants() {

    }
}
