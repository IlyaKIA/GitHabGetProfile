package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PivotalInfo {

    private final String name = "name";
    private final String email = "email";
    private final String contributors_url = "contributors_url";

    public void generateInfo () {
        String userName = "pivotal";
        long timer = System.currentTimeMillis();
        CompletableFuture<Map<String, String>> req1 = get("https://api.github.com/users/" + userName)
        .thenApply(this::findInfo);

        CompletableFuture<Map<String, String>> req2 = get("https://api.github.com/users/" + userName + "/repos")
                .thenApply(json -> findValueInJson(json, contributors_url).stream()
                        .limit(5)
                        .collect(Collectors.toList()))
                .thenApply(contribUrls -> contribUrls.stream()
                        .collect(Collectors.toMap(this::formatKey, v -> get(v)
                                .thenApply(json -> String.join(", ", findValueInJson(json, "login")))
                                .join())));

        req1.thenCombine(req2, (m1, m2) -> {
            System.out.println(m1);
            System.out.println(m2);
            return null;
        }).join();
        System.out.println("Total time " + (System.currentTimeMillis() - timer) + "ms");
    }

    private String formatKey(String url) {
        Pattern pattern = Pattern.compile("https://.+/.+/.+/([\\w]+)/");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return "\nContributors for repo \"" + matcher.group(1) + "\" ";
        }
        return null;
    }

    private CompletableFuture<String> get(String uri) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();
        CompletableFuture<HttpResponse<String>> httpResponseCompletableFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        return httpResponseCompletableFuture.thenApply(HttpResponse::body);
    }

    private Map<String, String> findInfo(String json) {
        HashMap<String, String> info = new HashMap<>();
        info.put(name, findValueInJson(json, name).get(0));
        info.put(email, findValueInJson(json, email).get(0));
        return info;
    }

    private List<String> findValueInJson (String json, String name) {
        Pattern pattern = Pattern.compile("\"" + name + "\":\"([\\w@:/\\.]+)\"");
        Matcher matcher = pattern.matcher(json);
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group(1));
        }
        return list;
    }
}