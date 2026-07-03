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
import jakarta.inject.Inject;

@ApplicationScoped
public class HourglassService {

    // AI Service registrata da langchain4j-cdi (@RegisterAIService): incapsula prompt,
    // chiamata all'LLM (con il tool degli standard) e mapping in HourglassOutput.
    @Inject
    private HourglassModeler modeler;

    public HourglassOutput process(HourglassInput input) {
        return modeler.analyze(input.getDomain(), input.getUseCase());
    }
}
