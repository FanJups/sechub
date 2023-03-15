// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.adapter;

import java.util.Map;

public interface AdapterConfig extends TrustAllConfig, TraceIdProvider {

    int getTimeOutInMilliseconds();

    /**
     *
     * @return time to wait , usable for {@link WaitForStateSupport}. Normally this
     *         describes time to wait for next operation
     */
    int getTimeToWaitForNextCheckOperationInMilliseconds();

    /**
     *
     * @return base url as string, never <code>null</code>
     */
    String getProductBaseURL();

    /**
     *
     * @return a base 64 encoded token containing "USERID:APITOKEN" inside
     */
    String getCredentialsBase64Encoded();

    String getUser();

    String getPolicyId();

    String getPasswordOrAPIToken();

    /**
     * Returns a map for options. Can be used to provide special behaviours which
     * are not default. E.g. wire mock extensions etc.
     *
     * @return map with options
     */
    public Map<AdapterOptionKey, String> getOptions();

    /**
     * @return the project id or <code>null</code> if none set
     */
    String getProjectId();

    /**
     * Returns a target string.
     *
     * @return target string or <code>null</code> if none defined (e.g. for code
     *         scans)
     */
    String getTargetAsString();

    /**
     * If the adapter configuration is for a mock, and a special mock behavior shall
     * happen,an identifier for the mock data to use will be returned. In all other
     * cases this method returns <code>null</code>.
     *
     * @return a mock data identifier or <code>null</code>
     */
    String getMockDataIdentifier();

}