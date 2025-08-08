package org.schimodie.albums_to_listen_to.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.DatatypeConverter;
import org.schimodie.albums_to_listen_to.bean.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LastFMClient {
    private static final String ROOT_URL = "http://ws.audioscrobbler.com/2.0/";
    private static final String API_KEY = "1c291f687b142a017916d4b6606e9335";
    private static final Pair API_KEY_PAIR = new Pair("api_key", API_KEY);
    private static final MessageDigest MD5_HASHING_ALGORITHM = LastFMClient.getMd5HashingAlgorithm();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String authToken;

    public LastFMClient() {
        // authToken = getJsonResponse(makeParameters("auth.gettoken")).get("token").asText();
        authToken = "";
    }

    public JsonNode getAlbumInfo(String artist, String album) {
        return getAlbumInfo(artist, album, "Schimodie");
    }

    public JsonNode getAlbumInfo(String artist, String album, String username) {
        if (username == null) {
            return getJsonResponse(makeParameters("album.getinfo",
                    Pair.encodedOf("artist", artist), Pair.encodedOf("album", album)));
        }
        return getJsonResponse(makeParameters("album.getinfo",
                Pair.encodedOf("artist", artist), Pair.encodedOf("album", album), Pair.encodedOf("username", username)));
    }

    private static JsonNode getJsonResponse(List<Pair> parameters) {
        try {
            String params = parameters.stream()
                    .map(parameter -> "&" + parameter.first() + '=' + parameter.second())
                    .collect(Collectors.joining());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(ROOT_URL + "?format=json" + params))
                    .header("User-Agent", "robot/schimodie")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return OBJECT_MAPPER.readTree(response.body());
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Pair> makeParameters(String method, Pair... parameters) {
        return makeParameters(method, false, parameters);
    }

    private static List<Pair> makeParameters(String method, boolean shouldSign, Pair... parameters) {
        List<Pair> params = new ArrayList<>(Arrays.asList(API_KEY_PAIR, Pair.encodedOf("method", method)));
        Collections.addAll(params, parameters);
        Collections.sort(params);

        if (shouldSign) {
            params.add(new Pair("api_sig", createSignature(params)));
        }

        return params;
    }

    private static String createSignature(List<Pair> parameters) {
        StringBuilder sb = new StringBuilder();

        for (Pair parameter : parameters) {
            sb.append(parameter.first()).append(parameter.second());
        }

        MD5_HASHING_ALGORITHM.update(sb.toString().getBytes(StandardCharsets.UTF_8));
        return DatatypeConverter.printHexBinary(MD5_HASHING_ALGORITHM.digest());
    }

    private static MessageDigest getMd5HashingAlgorithm() throws RuntimeException {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
