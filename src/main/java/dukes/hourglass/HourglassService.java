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

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HourglassService {

    // Inject the AI Service/Client configured via langchain4j-cdi or similar

    public HourglassOutput process(HourglassInput input) {
        // 1. Construct the prompt
        // 2. Call the augmented LLM
        // 3. Parse the result into HourglassOutput
        return new HourglassOutput();
    }
}
