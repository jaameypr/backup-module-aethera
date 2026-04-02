package it.pruefert.backupmodule.service;

import it.pruefert.backupmodule.model.BackupCallback;
import it.pruefert.backupmodule.model.BackupJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CallbackService {

    private static final Logger log = LoggerFactory.getLogger(CallbackService.class);

    private final RestClient restClient;

    @Value("${aethera.callback-url}")
    private String callbackUrl;

    @Value("${aethera.api-key:}")
    private String apiKey;

    public CallbackService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void notifyAethera(BackupJob job) {
        String url = job.getCallbackUrl() != null ? job.getCallbackUrl() : callbackUrl;

        BackupCallback payload;
        if (job.getStatus() == BackupJob.Status.COMPLETED) {
            payload = BackupCallback.success(
                    job.getJobId(), job.getServerId(),
                    job.getFilename(), job.getPath(), job.getSize(),
                    job.getShareUrl(), job.getShareId()
            );
        } else {
            payload = BackupCallback.failure(job.getJobId(), job.getServerId(), job.getError());
        }

        try {
            var request = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON);

            if (apiKey != null && !apiKey.isBlank()) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }

            request.body(payload).retrieve().toBodilessEntity();

            log.info("Callback sent to {} for job {} — status: {}", url, job.getJobId(), job.getStatus());
        } catch (Exception e) {
            log.error("Failed to send callback to {} for job {}: {}", url, job.getJobId(), e.getMessage());
        }
    }
}
