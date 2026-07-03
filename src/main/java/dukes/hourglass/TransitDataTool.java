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
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * LangChain4j tool: the real public-transport agencies serving a city, from the Transitland
 * open transit-feed index. It grounds the MOBILITY side of the hourglass in real local data —
 * named transit operators actually serving the city — mirroring what {@link EvChargingTool}
 * does for the energy/charging side. Because Transitland is built from open GTFS/GBFS feeds,
 * an agency's presence is itself evidence that the city already adopts those interoperability
 * standards, which lets the model justify GTFS/NeTEx in the waist with real adoption.
 *
 * The agency serving-area is queried by city name (no geocoding needed). Fail-safe: any error,
 * a missing key or an unknown city returns a short "no data" string instead of throwing, so a
 * flaky network never breaks the analyze flow.
 */
@ApplicationScoped
public class TransitDataTool {

    private static final Logger LOG = Logger.getLogger(TransitDataTool.class.getName());

    /** Keep the grounded stakeholder list concise. */
    private static final int MAX_AGENCIES = 10;

    @Tool("Returns the real public-transport agencies serving a given city, from Transitland's "
            + "open transit-feed index (built from GTFS/GBFS). Use it to ground transport "
            + "stakeholders in real local operators and to evidence that the city already adopts "
            + "open mobility-data standards. Pass the city name, e.g. 'Amsterdam'.")
    public String transitInfoFor(String city) {
        if (city == null || city.isBlank()) {
            return "No city provided.";
        }
        LOG.info("Transit tool invoked by the agent for city: " + city);
        String apiKey = System.getenv("TRANSITLAND_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warning("TRANSITLAND_API_KEY is not set in the server environment — start with ./dev.sh.");
            return "Transit agency data not available (Transitland key not configured).";
        }
        try {
            String url = "https://transit.land/api/v2/rest/agencies?limit=50&city_name="
                    + URLEncoder.encode(city, StandardCharsets.UTF_8)
                    + "&apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            HttpResponse<String> res = HttpJson.get(url);
            if (res.statusCode() != 200) {
                return "Transit data unavailable (HTTP " + res.statusCode() + ").";
            }
            return parseAgencies(res.body(), city);
        } catch (Exception e) {
            LOG.warning("Transit tool failed for '" + city + "': " + e.getMessage());
            return "Transit lookup failed; proceed without it.";
        }
    }

    private String parseAgencies(String body, String city) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject root = reader.readObject();
            JsonArray agencies = root.getJsonArray("agencies");
            if (agencies == null || agencies.isEmpty()) {
                return "No transit agencies found in Transitland for: " + city;
            }
            Set<String> names = new LinkedHashSet<>();
            for (JsonValue v : agencies) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }
                JsonValue name = v.asJsonObject().get("agency_name");
                if (name != null && name.getValueType() == JsonValue.ValueType.STRING) {
                    String n = ((JsonString) name).getString().strip();
                    if (!n.isBlank()) {
                        names.add(n);
                    }
                }
            }
            if (names.isEmpty()) {
                return "No named transit agencies found in Transitland for: " + city;
            }
            String list = names.stream().limit(MAX_AGENCIES).collect(Collectors.joining(", "));
            return "Transitland — transit agencies serving " + city + " (from the open GTFS feed index): "
                    + list + ". These operators publish open transit data, evidence of real local adoption "
                    + "of the GTFS/NeTEx interoperability standards.";
        } catch (RuntimeException e) {
            return "Could not parse transit data for: " + city;
        }
    }
}
