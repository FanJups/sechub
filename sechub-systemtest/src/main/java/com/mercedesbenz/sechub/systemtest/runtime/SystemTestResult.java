package com.mercedesbenz.sechub.systemtest.runtime;

import java.util.LinkedHashSet;
import java.util.Set;

public class SystemTestResult {

    private Set<SystemTestRunResult> runs = new LinkedHashSet<>();

    public Set<SystemTestRunResult> getRuns() {
        return runs;
    }

    public boolean hasFailedTests() {
        boolean hasErrors = false;
        for (SystemTestRunResult runResult : runs) {
            hasErrors = runResult.isFailed();
            if (hasErrors) {
                break;
            }
        }
        return hasErrors;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (SystemTestRunResult result : getRuns()) {
            sb.append(result.toString());
            sb.append("\n");
        }

        return sb.toString();
    }
}
