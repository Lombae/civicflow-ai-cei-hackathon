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

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Skill (LangChain4j AI Service) that applies the "hourglass model" to the domain of
 * urban Mobility-as-a-Service in the context of the energy transition.
 *
 * Registered as a CDI bean by the langchain4j-cdi portable extension. It uses the model
 * configured in microprofile-config.properties (bean "chat-model"), has access to the
 * {@link MobilityStandardsCatalog} tool to ground standards in real specs, and to a
 * documental knowledge base via the "standards-retriever" content retriever. The return
 * value {@link HourglassOutput} is mapped via structured output.
 */
@RegisterAIService(
        chatModelName = "chat-model",
        contentRetrieverName = "standards-retriever",
        tools = { MobilityStandardsCatalog.class, WebContextTool.class, EvChargingTool.class,
                TransitDataTool.class },
        outputGuardrails = HourglassOutputGuardrail.class,
        scope = ApplicationScoped.class)
public interface HourglassModeler {

    @SystemMessage({
        "You are an expert in enterprise architecture for urban mobility (Mobility-as-a-Service)",
        "in the context of the energy transition. You model domains with the 'hourglass model':",
        "- TOP (wide): the many stakeholders and use cases;",
        "- WAIST (narrow): the common capabilities and STANDARDS that make everything interoperable;",
        "- BOTTOM (wide): the many concrete implementations.",
        "The WAIST is NARROW BY DEFINITION — this is the entire point of the hourglass: a MINIMAL",
        "set of shared standards on which the many stakeholders above and implementations below all",
        "converge (like IP in the Internet's hourglass). A wide waist is WRONG. Do NOT list every",
        "standard of every area. From all the real standards you gather, SELECT ONLY the few (aim for",
        "4-6) most CROSS-CUTTING ones — those shared by the largest number of stakeholders and",
        "capabilities and bridging mobility AND energy. Deduplicate ruthlessly.",
        "Among the stakeholders always distinguish the energy actors relevant to the transition:",
        "besides energy suppliers, include the electricity grid operators (DSO/TSO) when the",
        "use case involves energy demand management or grid integration.",
        "To ground the waist you MUST use the 'standardsFor' tool (and the knowledge base) as your",
        "SOURCE of real standards — cite ONLY standards they return, never invented ones — but treat",
        "their output as a CANDIDATE POOL to distil from, not a list to copy wholesale.",
        "You also have access to a documental knowledge base on standards and EU regulations",
        "(e.g. AFIR, MMTIS, Data Act): use the retrieved passages to add precision and, where",
        "relevant, reference the applicable regulations.",
        "You also have a 'searchWeb' tool that fetches a concise factual definition from the web",
        "(Wikipedia). Use it to ground the KEY CONCEPTS and capabilities of the use case (e.g. what",
        "'Mobility as a service', 'demand response' or 'vehicle-to-grid' mean) so the capabilities are",
        "precise and correctly framed. Do NOT use 'searchWeb' for the city's transit operators — the",
        "'transitInfoFor' tool already covers those with real data.",
        "Reply in English, concise and concrete."
    })
    @UserMessage({
        "Domain: {{domain}}",
        "Use case: {{useCase}}",
        "Step 1 — You MUST call 'searchWeb' to ground the one or two CENTRAL concepts of the use case",
        "(e.g. 'Mobility as a service', 'demand response', 'vehicle-to-grid') and use the definitions to",
        "make the capabilities precise. Do NOT use it for the city's transit operators (transitInfoFor",
        "covers those); if the use case names a specific initiative, you MAY look it up too.",
        "Step 2 — If a city is named, you MUST ALSO call 'chargingInfoFor' with the city name to get",
        "the real EV charging operators and station counts. Name those real operators among the",
        "stakeholders, each tagged '(live data)'. Ignore any operator clearly not local to the city",
        "(obvious data artifacts).",
        "Step 3 — If a city is named, you MUST ALSO call 'transitInfoFor' with the city name to get the",
        "real public-transport agencies serving it; name those agencies among the stakeholders, each",
        "tagged '(live data)', and treat their published open feeds as evidence that GTFS/NeTEx belong",
        "in the waist.",
        "Step 4 — Call the 'standardsFor' tool for each relevant area (maas, micromobility, evcharging,",
        "energy, traffic, payment) to gather the CANDIDATE real standards. This is your sourcing step,",
        "NOT the final answer.",
        "Step 5 — DISTIL: from all candidates keep ONLY the few cross-cutting standards that the most",
        "stakeholders and capabilities share (mobility AND energy). This distilled set is the narrow waist.",
        "Step 6 — Generate the hourglass model. Populate:",
        "- stakeholders: the key actors (TOP level); list the tool-grounded real operators/agencies",
        "  first (keep their '(live data)' tag), then the inferred roles (DSO/TSO, MaaS providers, users);",
        "- capabilities: the common capabilities/services of the WAIST;",
        "- standardsGroups: a SHORT flat list of 4-6 entries, each ONE core standard with a brief note of",
        "  WHICH actors it binds and WHICH capability it enables (the convergence), e.g. 'TOMP-API -",
        "  binds transit, sharing & MaaS providers -> integrated booking and payment'. No per-area dumps,",
        "  no duplicates: this short list IS the narrow neck of the hourglass."
    })
    HourglassOutput analyze(@V("domain") String domain, @V("useCase") String useCase);
}
