/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package dukes.hourglass;

import java.util.List;

public class HourglassOutput {
    private List<String> stakeholders;
    private List<String> capabilities;
    private List<String> standardsGroups;

    public List<String> getStakeholders() {
        return stakeholders;
    }

    public void setStakeholders(List<String> stakeholders) {
        this.stakeholders = stakeholders;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public List<String> getStandardsGroups() {
        return standardsGroups;
    }

    public void setStandardsGroups(List<String> standardsGroups) {
        this.standardsGroups = standardsGroups;
    }
}
