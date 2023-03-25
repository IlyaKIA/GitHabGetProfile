package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PivotalInfo {

    private final String name = "name";
    private final String email = "email";
    private final String contributors_url = "contributors_url";

    public void generateInfo () {
        CompletableFuture<Map<String, String>> req1 = get("https://api.github.com/users/pivotal")
        .thenApply(f -> findInfo(f.body()));
        CompletableFuture<Map<String, String>> req2 = get("https://api.github.com/users/pivotal/repos")
                .thenApply(f -> findUrl(f.body()))
                .thenApply(this::get)
                .thenApply(c -> findContributors(c.join().body()));
        Map<String, String> result = req1.thenCombine(req2, (m1, m2) -> {
            Map<String, String> hashMap = new HashMap<>();
            hashMap.putAll(m1);
            hashMap.putAll(m2);
            return hashMap;
        }).join();

        System.out.println(name + "=" + result.get(name));
        result.remove(name);
        System.out.println(email + "=" + result.get(email));
        result.remove(email);
        for (Map.Entry<String,String> entry : result.entrySet()) {
            System.out.println(entry);
        }
    }

    private HashMap<String, String> findContributors(String json) {
        HashMap<String, String> info = new HashMap<>();
        Pattern pattern = Pattern.compile("\"" + "login" + "\":\"([\\w@:/\\.]+)\"");
        Matcher matcher = pattern.matcher(json);
        int n = 0;
        while (matcher.find()) {
            info.put("Contributor" + n, matcher.group(1));
            n++;
        }
        return info;
    }

    private String findUrl(String json) {
        return findValueInJson(json, contributors_url);
    }

    private CompletableFuture<HttpResponse<String>> get(String uri) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, String> findInfo(String json) {
        HashMap<String, String> info = new HashMap<>();
        info.put(name, findValueInJson(json, name));
        info.put(email, findValueInJson(json, email));
        return info;
    }

    private String findValueInJson (String json, String name) {
        Pattern pattern = Pattern.compile("\"" + name + "\":\"([\\w@:/\\.]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
