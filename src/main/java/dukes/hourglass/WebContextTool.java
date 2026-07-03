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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * LangChain4j tool: fetches encyclopedic definitions from the web (Wikipedia REST/Action API).
 * It grounds the KEY CONCEPTS and capabilities of the use case — e.g. Mobility-as-a-Service,
 * demand response, vehicle-to-grid — so the model frames the capabilities precisely. Real transit
 * operators are grounded separately by {@link TransitDataTool} and chargers by {@link EvChargingTool};
 * this tool deliberately covers the conceptual layer instead. The agent chooses whether to call it.
 *
 * Fail-safe by design: any network/API/parse error returns a short "no data" string instead of
 * throwing, so a flaky venue network never breaks the analyze flow. No API key, no extra dependency
 * (java.net.http + Jakarta JSON-P, both already available).
 */
@ApplicationScoped
public class WebContextTool {

    private static final Logger LOG = Logger.getLogger(WebContextTool.class.getName());

    @Tool("Fetch a concise factual definition from the web (Wikipedia) about a concept, technology, "
            + "topic or named initiative — for example what 'Mobility as a service', 'demand response' "
            + "or 'vehicle-to-grid' mean. Use it to ground and frame the capabilities. Do NOT use it to "
            + "list a city's transit operators (another tool covers those). Pass a short query like "
            + "'demand response' or 'Mobility as a service'.")
    public String searchWeb(String query) {
        if (query == null || query.isBlank()) {
            return "No query provided.";
        }
        LOG.info("Web tool invoked by the agent with query: " + query);
        try {
            String url = "https://en.wikipedia.org/w/api.php?action=query&format=json"
                    + "&prop=extracts&exintro=1&explaintext=1&exchars=1200&redirects=1"
                    + "&generator=search&gsrlimit=1&gsrsearch="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpResponse<String> res = HttpJson.get(url);
            if (res.statusCode() != 200) {
                return "Web lookup unavailable (HTTP " + res.statusCode() + ").";
            }
            return parseExtract(res.body(), query);
        } catch (Exception e) {
            // Never break the analyze flow: degrade gracefully if the network/API fails.
            LOG.warning("searchWeb failed for '" + query + "': " + e.getMessage());
            return "Web lookup failed; proceed without external context.";
        }
    }

    private String parseExtract(String body, String query) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject root = reader.readObject();
            JsonObject queryObj = root.getJsonObject("query");
            if (queryObj == null) {
                return "No web result for: " + query;
            }
            JsonObject pages = queryObj.getJsonObject("pages");
            if (pages == null || pages.isEmpty()) {
                return "No web result for: " + query;
            }
            JsonObject page = pages.values().iterator().next().asJsonObject();
            String title = page.getString("title", query);
            String extract = page.getString("extract", "");
            if (extract.isBlank()) {
                return "No web result for: " + query;
            }
            return title + " — " + extract.strip();
        } catch (RuntimeException e) {
            return "Could not parse web result for: " + query;
        }
    }
}
