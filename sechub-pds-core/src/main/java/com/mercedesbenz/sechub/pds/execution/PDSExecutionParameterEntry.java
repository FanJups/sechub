// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.pds.execution;

public class PDSExecutionParameterEntry {

    public PDSExecutionParameterEntry() {
        /* used by Jackson */
    }

    public PDSExecutionParameterEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    private String key;

    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "PDSExecutionParameterEntry [" + (key != null ? "key=" + key + ", " : "") + "]";
    }
}
