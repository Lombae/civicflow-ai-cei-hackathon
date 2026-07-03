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

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * LangChain4j tool: a curated catalog of REAL interoperability standards for urban
 * mobility (MaaS) and the energy transition. It grounds the "waist" of the hourglass
 * in verified specifications, preventing the model from inventing them.
 */
@ApplicationScoped
public class MobilityStandardsCatalog {

    private static final Logger LOG = Logger.getLogger(MobilityStandardsCatalog.class.getName());

    private static final Map<String, List<String>> CATALOG = new LinkedHashMap<>();
    static {
        CATALOG.put("maas", List.of(
                "TOMP-API — Transport Operator / MaaS Provider API (MaaS interoperability)",
                "NeTEx (CEN/TC 278) — exchange of public transport network, stop and timetable data",
                "GTFS & GTFS-Realtime — static and real-time public transport data",
                "SIRI (CEN/TC 278) — real-time service information",
                "ISO/TC 204 — Intelligent Transport Systems (ITS)"));
        CATALOG.put("micromobility", List.of(
                "MDS — Mobility Data Specification (Open Mobility Foundation)",
                "GBFS — General Bikeshare Feed Specification (shared vehicles)"));
        CATALOG.put("evcharging", List.of(
                "OCPP — Open Charge Point Protocol (Open Charge Alliance): charge point <-> backend",
                "OCPI — Open Charge Point Interface: roaming between CPO and eMSP",
                "ISO 15118 — Vehicle-to-Grid Communication / Plug & Charge"));
        CATALOG.put("energy", List.of(
                "OpenADR (OpenADR Alliance) — Automated Demand Response",
                "IEC 61850 — substation automation and smart grid",
                "IEEE 2030.5 (SEP 2.0) — Smart Energy Profile for Distributed Energy Resources"));
        CATALOG.put("traffic", List.of(
                "DATEX II (CEN/TC 278) — exchange of road traffic and conditions data"));
        CATALOG.put("payment", List.of(
                "EMV & PCI DSS — contactless payments and payment data security",
                "OpenID Connect / OAuth 2.0 — digital identity and authorization"));
    }

    @Tool("Returns the real interoperability standards and the bodies that maintain them for a "
            + "given area of urban mobility or energy. Useful values for 'area': 'maas', "
            + "'micromobility', 'evcharging', 'energy', 'traffic', 'payment'. With an empty or "
            + "unknown area it returns the whole catalog.")
    public List<String> standardsFor(String area) {
        LOG.info("Standards tool invoked by the agent for area: " + area);
        String a = area == null ? "" : area.toLowerCase();
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : CATALOG.entrySet()) {
            if (matches(a, entry.getKey())) {
                result.addAll(entry.getValue());
            }
        }
        if (result.isEmpty()) {
            CATALOG.values().forEach(result::addAll);
        }
        return result;
    }

    private boolean matches(String area, String key) {
        if (area.isBlank()) {
            return false;
        }
        if (area.contains(key) || key.contains(area)) {
            return true;
        }
        return switch (key) {
            case "maas" -> area.contains("transit") || area.contains("transport")
                    || area.contains("ticket") || area.contains("mobility") || area.contains("public");
            case "micromobility" -> area.contains("scooter") || area.contains("bike")
                    || area.contains("sharing") || area.contains("micro");
            case "evcharging" -> area.contains("ev") || area.contains("charg")
                    || area.contains("v2g");
            case "energy" -> area.contains("energ") || area.contains("grid")
                    || area.contains("demand") || area.contains("dso") || area.contains("utility");
            case "traffic" -> area.contains("traffic") || area.contains("road");
            case "payment" -> area.contains("pay") || area.contains("ident");
            default -> false;
        };
    }
}
