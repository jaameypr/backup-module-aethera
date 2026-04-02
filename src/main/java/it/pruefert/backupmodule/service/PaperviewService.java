package it.pruefert.backupmodule.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.util.Map;

@Service
public class PaperviewService {

    private static final Logger log = LoggerFactory.getLogger(PaperviewService.class);

    @Value("${paperview.url:}")
    private String paperviewUrl;

    @Value("${paperview.api-key:}")
    private String paperviewApiKey;

    private final RestClient restClient;

    public PaperviewService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public boolean isAvailable() {
        return paperviewUrl != null && !paperviewUrl.isBlank()
                && paperviewApiKey != null && !paperviewApiKey.isBlank();
    }

    public ShareResult uploadBackup(Path filePath, String filename, String serverName) {
        if (!isAvailable()) {
            throw new IllegalStateException("Paperview is not configured");
        }

        String url = paperviewUrl.replaceAll("/+$", "") + "/api/shares";

        log.info("Uploading {} to Paperview at {}", filename, url);

        // Paperview expects a multipart upload with file + metadata
        var fileResource = new FileSystemResource(filePath);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + paperviewApiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(buildMultipartBody(fileResource, filename, serverName))
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("share")) {
            throw new RuntimeException("Unexpected Paperview response");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> share = (Map<String, Object>) response.get("share");
        String shareId = (String) share.get("_id");
        String shareUrl = paperviewUrl.replaceAll("/+$", "") + "/pages/shares/" + shareId;

        return new ShareResult(shareId, shareUrl);
    }

    private org.springframework.util.MultiValueMap<String, org.springframework.http.HttpEntity<?>> buildMultipartBody(
            FileSystemResource file, String filename, String serverName) {
        var builder = new MultipartBodyBuilder();
        builder.part("file", file)
                .header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=file; filename=" + filename);
        builder.part("title", "Backup: " + serverName);
        builder.part("description", "Automated backup from Aethera");
        builder.part("visibility", "private");
        builder.part("downloadEnabled", "true");
        builder.part("previewMode", "download_only");
        return builder.build();
    }

    public record ShareResult(String shareId, String shareUrl) {}
}
