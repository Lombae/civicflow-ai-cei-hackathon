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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * OutputGuardrail: validates the model's hourglass response and forces a self-correcting
 * reprompt if it is invalid. Turns the "no hallucination" promise from a prompt instruction
 * into a runtime guarantee:
 *   1. the three lists must be non-empty;
 *   2. every standard in standardsGroups must reference a REAL standard/regulation from the
 *      verified catalog / knowledge base — invented standards trigger a reprompt.
 *
 * Wired via {@code @RegisterAIService(outputGuardrails = ...)}. On reject it reprompts (the
 * framework retries up to its max); logs each rejection so it is visible in a live demo.
 */
@ApplicationScoped
public class HourglassOutputGuardrail implements OutputGuardrail {

    private static final Logger LOG = Logger.getLogger(HourglassOutputGuardrail.class.getName());

    /** Real standards the model may cite as the WAIST (from the catalog + RAG knowledge base). */
    private static final List<String> KNOWN_STANDARDS = List.of(
            "TOMP-API", "NeTEx", "GTFS", "SIRI", "ISO/TC 204", "MDS", "GBFS",
            "OCPP", "OCPI", "ISO 15118", "OpenADR", "IEC 61850", "IEEE 2030.5",
            "DATEX II", "EMV", "PCI DSS", "OpenID Connect", "OAuth");

    /** Real EU regulations allowed as context alongside the standards (not counted as waist width). */
    private static final List<String> KNOWN_REGULATIONS = List.of(
            "AFIR", "MMTIS", "Data Act", "2017/1926", "2023/1804", "2023/2854");

    /** The hourglass waist must stay NARROW: at most this many distinct core standards.
     *  Prompt targets 4-6; the cap keeps one slot of headroom so a legitimate 7th cross-cutting
     *  standard does not trigger a brittle reprompt loop, while still rejecting a truly wide waist. */
    private static final int MAX_WAIST_STANDARDS = 7;

    private static final List<String> REQUIRED_LISTS = List.of("stakeholders", "capabilities", "standardsGroups");

    @Override
    public OutputGuardrailResult validate(AiMessage response) {
        String text = response == null ? null : response.text();
        if (text == null || text.isBlank()) {
            return reject("empty response",
                    "Return a valid JSON hourglass model with non-empty stakeholders, capabilities and standardsGroups.");
        }

        JsonObject obj;
        try (JsonReader reader = Json.createReader(new StringReader(stripCodeFences(text)))) {
            obj = reader.readObject();
        } catch (RuntimeException e) {
            return reject("output is not valid JSON",
                    "Return ONLY a valid JSON object with the arrays stakeholders, capabilities and standardsGroups.");
        }

        for (String field : REQUIRED_LISTS) {
            if (!obj.containsKey(field)
                    || obj.get(field).getValueType() != JsonValue.ValueType.ARRAY
                    || obj.getJsonArray(field).isEmpty()) {
                return reject("the '" + field + "' list is missing or empty",
                        "Populate all three lists (stakeholders, capabilities, standardsGroups); none may be empty.");
            }
        }

        JsonArray standards = obj.getJsonArray("standardsGroups");
        Set<String> waistStandards = new LinkedHashSet<>();
        for (JsonValue v : standards) {
            if (v.getValueType() != JsonValue.ValueType.STRING) {
                return reject("standardsGroups must contain only strings",
                        "Return standardsGroups as a flat JSON array of strings, each naming a real standard.");
            }
            String entry = ((JsonString) v).getString();
            if (!mentionsKnownStandard(entry)) {
                return reject("standard not in the verified catalog: \"" + entry + "\"",
                        "In standardsGroups cite ONLY real standards obtained from the standardsFor tool "
                                + "(e.g. TOMP-API, NeTEx, OCPI, OpenADR, ISO 15118). Remove any invented standard.");
            }
            for (String known : KNOWN_STANDARDS) {
                if (containsAsToken(entry, known)) {
                    waistStandards.add(known);
                }
            }
        }
        if (waistStandards.size() > MAX_WAIST_STANDARDS) {
            return reject("the waist cites " + waistStandards.size() + " standards — too wide for an hourglass",
                    "Keep the waist NARROW: cite at most " + MAX_WAIST_STANDARDS + " core cross-cutting "
                            + "standards in standardsGroups (aim for 4-6) — the few shared by the most "
                            + "stakeholders and capabilities. Remove the rest; regulations may remain as context.");
        }
        return success();
    }

    private OutputGuardrailResult reject(String reason, String repromptText) {
        LOG.info("Output guardrail REJECTED (" + reason + ") -> reprompting the model to self-correct.");
        return reprompt("Hourglass output invalid: " + reason, repromptText);
    }

    private boolean mentionsKnownStandard(String entry) {
        return containsAny(entry, KNOWN_STANDARDS) || containsAny(entry, KNOWN_REGULATIONS);
    }

    private static boolean containsAny(String entry, List<String> tokens) {
        for (String token : tokens) {
            if (containsAsToken(entry, token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if {@code token} occurs in {@code haystack} not embedded inside a larger word — so
     * "GTFS & GTFS-Realtime" matches "GTFS" but an invented "SIRIUS-Pay" does NOT match "SIRI".
     */
    private static boolean containsAsToken(String haystack, String token) {
        int from = 0;
        int idx;
        while ((idx = haystack.indexOf(token, from)) >= 0) {
            boolean leftOk = idx == 0 || !Character.isLetter(haystack.charAt(idx - 1));
            int end = idx + token.length();
            boolean rightOk = end >= haystack.length() || !Character.isLetter(haystack.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            from = idx + 1;
        }
        return false;
    }

    /** Removes a surrounding markdown code fence (```json ... ```), if present. */
    private static String stripCodeFences(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.strip();
    }
}

