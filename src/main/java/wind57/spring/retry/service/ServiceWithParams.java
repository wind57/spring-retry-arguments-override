package wind57.spring.retry.service;

import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Service
public class ServiceWithParams {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss");

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static final String PARAMS = "start=#START#&end=#END#";

    public static final String HOURLY_URL = "http://localhost/hour?";

    public static final String MINUTES_URL = "http://localhost/minute?";

    public static final String UNDER_MINUTE_URL = "http://localhost/under-minute?";

    /**
     * this is a stub that acts on well-defined rules.
     */
    @Retryable(interceptor = "retryInterceptor")
    public String status(LocalDateTime start, LocalDateTime end) {

        long secondsBetween = ChronoUnit.SECONDS.between(start, end);

        String startEncoded = URLEncoder.encode(FORMATTER.format(start), StandardCharsets.UTF_8);
        String endEncoded = URLEncoder.encode(FORMATTER.format(end), StandardCharsets.UTF_8);
        String encoded = PARAMS.replaceFirst("#START#", startEncoded).replaceFirst("#END#", endEncoded);

        // duration is bigger then an hour
        if (secondsBetween > TimeUnit.HOURS.toSeconds(1)) {
            return getString(encoded, HOURLY_URL);
        }

        // duration is bigger then an hour
        if (ChronoUnit.SECONDS.between(start, end) > TimeUnit.MINUTES.toSeconds(1)) {
            return getString(encoded, MINUTES_URL);
        } else {
            return getString(encoded, UNDER_MINUTE_URL);
        }

    }

    /**
     * this is a stub that acts on well-defined rules.
     */
    @Retryable(interceptor = "retryInterceptor")
    public String statusNoHttpCalls(LocalDateTime start, LocalDateTime end) {

        long secondsBetween = ChronoUnit.SECONDS.between(start, end);

        // duration is bigger then an hour
        if (secondsBetween > TimeUnit.HOURS.toSeconds(1)) {
            throw new RuntimeException("diff is in hours, too big. Got : " + secondsBetween / (60 * 60) + " hours");
        }

        // duration is bigger then an hour
        if (ChronoUnit.SECONDS.between(start, end) > TimeUnit.MINUTES.toSeconds(1)) {
            throw new RuntimeException("diff is in minutes, too big. Got : " + secondsBetween / 60 + " minutes");
        } else {
            System.out.println("under one minute, good!");
            return "nice";
        }

    }

    private String getString(String encoded, String url) {
        String s = url + encoded;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(s))
                .build();

        try {
            // this call will be mocked
            return handleResponse(CLIENT.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String handleResponse(HttpResponse<String> resp) {
        if (resp.statusCode() != 200) {
            throw new RuntimeException(resp.body());
        }

        return resp.body();
    }

}
