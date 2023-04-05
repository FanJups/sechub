package com.mercedesbenz.sechub.systemtest;

import static com.mercedesbenz.sechub.systemtest.SystemTestAPI.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mercedesbenz.sechub.commons.model.JSONConverter;
import com.mercedesbenz.sechub.systemtest.config.SystemTestConfiguration;
import com.mercedesbenz.sechub.systemtest.runtime.SystemTestResult;
import com.mercedesbenz.sechub.systemtest.runtime.SystemTestRuntimeException;
import com.mercedesbenz.sechub.test.TestFileReader;
import com.mercedesbenz.sechub.test.TestUtil;

/**
 * An integration test if the system test API and the involved runtime +
 * configuration builder can work together and execute real (but simple fake)
 * bash scripts.
 *
 * @author Albert Tregnaghi
 *
 */
class SystemTestIntegrationTest {

    private static final String TEST_PDS_SOLUTIONS_PATH = "./src/test/resources/fake-root/sechub-pds-solutions";
    private static final Logger LOG = LoggerFactory.getLogger(SystemTestIntegrationTest.class);

    @BeforeEach
    void beforeEach(TestInfo info) {
        LOG.info("--------------------------------------------------------------------------------------------------------------------------------");
        LOG.info("System API tests: {}", info.getDisplayName());
        LOG.info("--------------------------------------------------------------------------------------------------------------------------------");
    }

    @Test
    void faked_gosec_can_be_executed_without_errors() throws IOException {
        /* @formatter:off */

        Path secHubStartOutputFile = TestUtil.createTempFileInBuildFolder("faked_gosec_sechub_start_output_file.txt");
        Path goSecStartOutputFile = TestUtil.createTempFileInBuildFolder("faked_gosec_pds_start_output_file.txt");
        Path goSecStopOutputFile = TestUtil.createTempFileInBuildFolder("faked_gosec_pds_stop_output_file.txt");
        Path secHubStopOutputFile = TestUtil.createTempFileInBuildFolder("faked_gosec_sechub_stop_output_file.txt");

        String var_Text = "nano_"+System.nanoTime();

        /* prepare */
        SystemTestConfiguration configuration = configure().

                addVariable("var_text",var_Text).
                addVariable("var_number","2").
                addVariable("test_var_number","${variables.var_number}_should_be_2").
                addVariable("test_env_path","${env.PATH}").
                addVariable("a-secret-example","${secretEnv.PATH}").

                localSetup().
                    secHub().
                        addStartStep().
                            script().
                                envVariable("TEST_NUMBER_LIST", "${variables.test_var_number}").
                                path("./01-start-single-docker-compose.sh").
                                arguments(secHubStartOutputFile.toString()).
                            endScript().
                        endStep().

                        addStopStep().
                            script().
                                envVariable("Y_TEST", "testy").
                                path("./01-stop-single-docker-compose.sh").
                                arguments(secHubStopOutputFile.toString(),"second","third-as:${variables.var_text}").
                            endScript().
                        endStep().

                        configure().
                            addExecutor().
                                pdsProductId("PDS_GOSEC").
                            endExecutor().
                        endConfigure().

                    endSecHub().

                    addSolution("faked-gosec").
                        addStartStep().
                            script().
                                envVariable("A_TEST1", "value1").
                                envVariable("B_TEST2", "value2").
                                envVariable("C_test_var_number_added", "${variables.test_var_number}").
                                envVariable("D_RESOLVED_SECRET","${variables.a-secret-example}").
                                path("./05-start-single-sechub-network-docker-compose.sh").
                                arguments(goSecStartOutputFile.toString(),"secondCallIsForPDS","third-as:${secretEnv.PATH}_may_not_be_resolved_because_only_script_env_can_contain_this").

                            endScript().
                        endStep().
                        addStopStep().
                            script().
                                envVariable("X_TEST", "testx").
                                path("./05-stop-single-sechub-network-docker-compose.sh").
                                arguments(goSecStopOutputFile.toString(),"second","third-as:${variables.var_text}").
                                workingDir("./").
                            endScript().
                        endStep().

                    endSolution().

                endLocalSetup().

                build();

        LOG.info("config=\n{}", JSONConverter.get().toJSON(configuration,true));

        /* execute */
        SystemTestResult result = runSystemTestsLocal(configuration, TEST_PDS_SOLUTIONS_PATH);

        /* test */
        if (result.hasFailedTests()) {
            fail("The execution failed?!?!");
        }

        String sechubStartOutputData = TestFileReader.loadTextFile(secHubStartOutputFile);
        assertEquals("sechub-started and TEST_NUMBER_LIST=2_should_be_2", sechubStartOutputData);

        String gosecStartOutputData = TestFileReader.loadTextFile(goSecStartOutputFile);
        assertEquals("gosec-started with param2=secondCallIsForPDS and C_test_var_number_added=2_should_be_2, B_TEST2=value2, D_RESOLVED_SECRET is like path=true, parameter3 is still a secret=true", gosecStartOutputData);

        String sechubStopOutputData = TestFileReader.loadTextFile(secHubStopOutputFile);
        assertEquals("sechub-stopped with param2=second and parm3=third-as:"+var_Text+" and Y_TEST=testy", sechubStopOutputData);

        String gosecStopOutputData = TestFileReader.loadTextFile(goSecStopOutputFile);
        assertEquals("gosec-stopped with param2=second and parm3=third-as:"+var_Text+" and X_TEST=testx", gosecStopOutputData);

        /* @formatter:on */
    }

    @Test
    void fail_because_unknown_runtime_variable() {
        /* @formatter:off */

        /* prepare */
        SystemTestConfiguration configuration = configure().
                localSetup().
                    addSolution("faked-fail_on_start").
                        addStartStep().script().path("./05-start-single-sechub-network-docker-compose.sh").arguments("${runtime.unknown_must_fail}").endScript().endStep().
                        addStopStep().script().path("./05-stop-single-sechub-network-docker-compose.sh").endScript().endStep().
                    endSolution().
                endLocalSetup().
                build();

        LOG.info("loaded config=\n{}", JSONConverter.get().toJSON(configuration,true));

        /* execute */
        SystemTestRuntimeException exception = assertThrows(SystemTestRuntimeException.class, ()->runSystemTestsLocal(configuration, TEST_PDS_SOLUTIONS_PATH));

        /* test */
        String message = exception.getMessage();
        assertTrue(message.contains("'runtime.unknown_must_fail' is not defined!"));
        // test proposals are inside error message:
        assertTrue(message.contains("Allowed variables for type RUNTIME_VARIABLES are:"));
        assertTrue(message.contains("- runtime.workspaceRoot"));


        /* @formatter:on */
    }

    @Test
    void fail_on_start() {
        /* @formatter:off */

        /* prepare */
        SystemTestConfiguration configuration = configure().
                addVariable("a-env-variable","WILL_BE_REPLACED:${env.PATH}").
                addVariable("a-secret-variable","WILL_NOT_BE_REPLACED:${secretEnv.PATH}").
                localSetup().
                    addSolution("faked-fail_on_start").
                        addStartStep().script().path("./05-start-single-sechub-network-docker-compose.sh").endScript().endStep().
                        addStopStep().script().path("./05-stop-single-sechub-network-docker-compose.sh").endScript().endStep().
                    endSolution().
                endLocalSetup().
                build();

        LOG.info("loaded config=\n{}", JSONConverter.get().toJSON(configuration,true));

        /* execute */
        SystemTestRuntimeException exception = assertThrows(SystemTestRuntimeException.class, ()->runSystemTestsLocal(configuration, TEST_PDS_SOLUTIONS_PATH));

        /* test */
        String message = exception.getMessage();
        assertTrue(message.contains("Script ./05-start-single-sechub-network-docker-compose.sh failed with exit code:33"));

        /* @formatter:on */
    }

    @Test
    void fail_because_no_pds_config() {
        /* @formatter:off */

        /* prepare */
        SystemTestConfiguration configuration = configure().
                localSetup().
                    addSolution("faked-fail_because_no_pds_server_config_file").
                        addStartStep().script().path("./05-start-single-sechub-network-docker-compose.sh").endScript().endStep().
                        addStopStep().script().path("./05-stop-single-sechub-network-docker-compose.sh").endScript().endStep().
                    endSolution().
                endLocalSetup().
                build();

        LOG.info("loaded config=\n{}", JSONConverter.get().toJSON(configuration,true));

        /* execute */
        SystemTestRuntimeException exception = assertThrows(SystemTestRuntimeException.class, ()->
            runSystemTestsLocal(configuration, TEST_PDS_SOLUTIONS_PATH));

        String message = exception.getMessage();
        assertTrue(message.contains("PDS server config file does not exist"));


        /* @formatter:on */
    }

}
