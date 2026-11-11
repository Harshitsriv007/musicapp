package com.musicapp.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class HttpAudioFetcher implements AudioFetcher {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public FetchedAudio open(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "dhun-music/1.0")
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("Audio source returned HTTP " + response.statusCode());
        }
        String type = response.headers().firstValue("Content-Type").orElse("audio/mpeg")
                .split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (!type.startsWith("audio/") && !type.equals("application/octet-stream")) {
            response.body().close();
            throw new IOException("Audio source returned non-audio content: " + type);
        }
        if (type.equals("application/octet-stream")) {
            type = "audio/mpeg";
        }
        long length = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        return new FetchedAudio(response.body(), type, length);
    }
}
