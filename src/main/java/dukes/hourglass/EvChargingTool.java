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
import jakarta.json.JsonNumber;
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

/**
 * LangChain4j tool: real EV charging infrastructure for a city, from the Open Charge Map
 * open database. Grounds the EV/energy side of the hourglass in real local data — named
 * charging operators (CPOs) actually present and station/point counts — which neither the
 * curated catalog nor Wikipedia provide. The agent chooses to call it (function calling).
 *
 * Two steps: geocode the city (Nominatim, no key) then query Open Charge Map (free key in
 * the OPENCHARGEMAP_API_KEY env var). Fail-safe: any error returns a short "no data" string
 * instead of throwing, so a flaky network or missing key never breaks the analyze flow.
 */
@ApplicationScoped
public class EvChargingTool {

    private static final Logger LOG = Logger.getLogger(EvChargingTool.class.getName());

    @Tool("Returns real EV charging infrastructure for a given city from Open Charge Map: the "
            + "number of charging stations/points and the named charging operators (CPOs) actually "
            + "present. Use it to ground EV-charging stakeholders and the energy side in real local "
            + "data. Pass the city name, e.g. 'Helsinki'.")
    public String chargingInfoFor(String city) {
        if (city == null || city.isBlank()) {
            return "No city provided.";
        }
        LOG.info("EV charging tool invoked by the agent for city: " + city);
        String apiKey = System.getenv("OPENCHARGEMAP_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warning("OPENCHARGEMAP_API_KEY is not set in the server environment — start with ./dev.sh.");
            return "EV charging data not available (Open Charge Map key not configured).";
        }
        try {
            double[] coords = geocode(city);
            if (coords == null) {
                return "Could not locate the city: " + city;
            }
            return queryCharging(city, coords[0], coords[1], apiKey);
        } catch (Exception e) {
            LOG.warning("EV charging tool failed for '" + city + "': " + e.getMessage());
            return "EV charging lookup failed; proceed without it.";
        }
    }

    private double[] geocode(String city) throws Exception {
        String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q="
                + URLEncoder.encode(city, StandardCharsets.UTF_8);
        HttpResponse<String> res = HttpJson.get(url);
        if (res.statusCode() != 200) {
            return null;
        }
        try (JsonReader reader = Json.createReader(new StringReader(res.body()))) {
            JsonArray arr = reader.readArray();
            if (arr.isEmpty()) {
                return null;
            }
            JsonObject first = arr.getJsonObject(0);
            double lat = Double.parseDouble(first.getString("lat"));
            double lon = Double.parseDouble(first.getString("lon"));
            return new double[]{lat, lon};
        }
    }

    private String queryCharging(String city, double lat, double lon, String apiKey) throws Exception {
        String url = "https://api.openchargemap.io/v3/poi/?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&latitude=" + lat + "&longitude=" + lon
                + "&distance=15&distanceunit=KM&maxresults=80";
        HttpResponse<String> res = HttpJson.get(url);
        if (res.statusCode() != 200) {
            return "EV charging data unavailable (HTTP " + res.statusCode() + ").";
        }
        try (JsonReader reader = Json.createReader(new StringReader(res.body()))) {
            JsonArray pois = reader.readArray();
            int stations = pois.size();
            int points = 0;
            Set<String> operators = new LinkedHashSet<>();
            for (JsonValue v : pois) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }
                JsonObject poi = v.asJsonObject();
                points += asInt(poi.get("NumberOfPoints"));
                String operator = operatorTitle(poi.get("OperatorInfo"));
                if (operator != null && !operator.isBlank() && !operator.contains("Unknown")) {
                    operators.add(operator);
                }
            }
            String ops = operators.isEmpty() ? "no named operator in the dataset" : String.join(", ", operators);
            return "Open Charge Map near " + city + ": ~" + stations + " charging stations ("
                    + points + " charging points). Real operators present: " + ops + ".";
        }
    }

    private static int asInt(JsonValue value) {
        return value != null && value.getValueType() == JsonValue.ValueType.NUMBER
                ? ((JsonNumber) value).intValue()
                : 0;
    }

    private static String operatorTitle(JsonValue operatorInfo) {
        if (operatorInfo == null || operatorInfo.getValueType() != JsonValue.ValueType.OBJECT) {
            return null;
        }
        JsonValue title = operatorInfo.asJsonObject().get("Title");
        return title != null && title.getValueType() == JsonValue.ValueType.STRING
                ? ((JsonString) title).getString()
                : null;
    }
}
