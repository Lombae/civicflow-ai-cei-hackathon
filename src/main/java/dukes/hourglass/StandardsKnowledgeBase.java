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

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import java.util.List;
import java.util.logging.Logger;

/**
 * RAG: in-memory knowledge base of curated summaries about real standards and regulations
 * for urban mobility and energy. It produces a CDI {@link ContentRetriever} (@Named
 * "standards-retriever") that the {@link HourglassModeler} skill uses to ground the answer
 * in precise, citable documental passages.
 *
 * Fully LangChain4j and fully in-memory: embeddings via OpenAI (text-embedding-3-small,
 * same OPENAI_API_KEY), no external vector database, no extra Maven dependency.
 */
@ApplicationScoped
public class StandardsKnowledgeBase {

    private static final Logger LOG = Logger.getLogger(StandardsKnowledgeBase.class.getName());

    @Produces
    @ApplicationScoped
    @Named("standards-retriever")
    public ContentRetriever standardsRetriever() {
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();

        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        try {
            List<TextSegment> segments = DOCUMENTS.stream().map(TextSegment::from).toList();
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            for (int i = 0; i < segments.size(); i++) {
                store.add(embeddings.get(i), segments.get(i));
            }
            LOG.info("RAG knowledge base: ingested " + segments.size() + " documents on standards/regulations.");
        } catch (RuntimeException e) {
            // Soft degradation: if embedding fails (e.g. missing key) the retriever stays empty
            // and the application keeps working without documental augmentation.
            LOG.warning("RAG ingestion failed (" + e.getMessage() + "); empty retriever, the app continues without RAG.");
        }

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(4)
                .minScore(0.3)
                .build();
    }

    /** Curated factual summaries (not verbatim official texts). One entry = one retrievable segment. */
    private static final List<String> DOCUMENTS = List.of(
        "TOMP-API (Transport Operator to MaaS Provider API) is the open standard that defines the "
            + "interface between transport operators and MaaS providers. It covers the full cycle: "
            + "planning, booking, payment, trip execution and support. It is maintained by the TOMP "
            + "Working Group and is central to MaaS interoperability in Europe.",
        "NeTEx (Network Timetable Exchange, CEN/TC 278, EN 12896) is the European XML format for "
            + "exchanging static public transport data: network topology, stops, planned timetables and "
            + "fare information. It is among the formats required by National Access Points.",
        "GTFS (General Transit Feed Specification) is the de-facto standard for static public transport "
            + "data (lines, stops, timetables). GTFS-Realtime extends it with real-time updates: trip "
            + "updates (delays), vehicle positions and service alerts.",
        "SIRI (Service Interface for Real-time Information, CEN/TC 278) is the European standard for "
            + "real-time exchange of public transport information, such as predicted arrivals and vehicle "
            + "positions. It is often used together with NeTEx.",
        "MDS (Mobility Data Specification), managed by the Open Mobility Foundation, is the set of APIs "
            + "that lets cities regulate and monitor shared micromobility services (scooters, bikes). It "
            + "includes the Provider, Agency and Policy APIs.",
        "GBFS (General Bikeshare Feed Specification), managed by MobilityData, is the open format for "
            + "publishing real-time availability of shared vehicles: stations, free vehicles and system "
            + "status.",
        "OCPP (Open Charge Point Protocol), by the Open Charge Alliance, standardizes communication "
            + "between the EV charge point and the central management system (CSMS). The most widespread "
            + "versions are 1.6 and 2.0.1; 2.1 adds smart charging and V2X functions.",
        "OCPI (Open Charge Point Interface) enables roaming and data exchange between charge point "
            + "operators (CPO) and e-mobility service providers (eMSP): station locations, tariffs, "
            + "charging sessions and CDRs (Charge Detail Records).",
        "ISO 15118 defines the communication between the electric vehicle and the charging station, "
            + "enabling Plug & Charge (automatic authentication and payment) and, with part 15118-20, "
            + "bidirectional Vehicle-to-Grid (V2G) charging.",
        "OpenADR (OpenADR Alliance) is the standard for Automated Demand Response: it lets utilities and "
            + "aggregators send automatic demand-management signals to distributed energy resources. "
            + "Reference versions: 2.0b and the more recent 3.0.",
        "IEC 61850 is the international standard for electrical substation automation and smart grid "
            + "communication, essential to integrate distributed resources and grid management.",
        "IEEE 2030.5 (Smart Energy Profile 2.0) is the standard for communicating with distributed "
            + "energy resources (DER) such as photovoltaics and electric vehicles. It is adopted, for "
            + "example, in the Common Smart Inverter Profile (CSIP) for grid interconnection.",
        "DATEX II (CEN/TC 278) is the European standard for exchanging road traffic and conditions data "
            + "between control centers, road operators and information services.",
        "ISO/TC 204 is the ISO technical committee for Intelligent Transport Systems (ITS), developing "
            + "standards for integrating information, communication and control in transport.",
        "AFIR (Alternative Fuels Infrastructure Regulation, EU Regulation 2023/1804) sets binding "
            + "targets for deploying charging infrastructure in the EU and imposes requirements on ad-hoc "
            + "payments, price transparency and availability of station data.",
        "EU Delegated Regulation 2017/1926 (MMTIS, Multimodal Travel Information Services), under the ITS "
            + "Directive, requires Member States to make multimodal travel data available through "
            + "National Access Points, using formats such as NeTEx, SIRI and DATEX II.",
        "The Data Act (EU Regulation 2023/2854) governs access to and sharing of data generated by "
            + "connected products and services, relevant for connected mobility and for vehicle and "
            + "charging infrastructure data.",
        "In the hourglass model, many actors and use cases (the wide top) converge on a narrow 'waist' "
            + "of a few common standards and capabilities that ensure interoperability, from which many "
            + "concrete implementations branch out (the wide bottom). In MaaS the waist consists of "
            + "shared APIs and data standards."
    );
}
