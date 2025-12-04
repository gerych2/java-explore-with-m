package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class StatsClient {

    private final RestTemplate restTemplate;
    private final String statsServerUrl;

    public StatsClient(RestTemplate restTemplate,
                       @Value("${stats-server.url:http://stats-server:9090}") String statsServerUrl) {
        this.restTemplate = restTemplate;
        this.statsServerUrl = statsServerUrl;
    }

    public void saveHit(EndpointHitDto dto) {
        try {
            String url = statsServerUrl + "/hit";
            HttpEntity<EndpointHitDto> request = new HttpEntity<>(dto);
            restTemplate.postForEntity(url, request, Void.class);
            log.debug("Hit saved: app={}, uri={}, ip={}", dto.getApp(), dto.getUri(), dto.getIp());
        } catch (Exception e) {
            log.error("Error saving hit: {}", e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        try {
            StringBuilder urlBuilder = new StringBuilder(statsServerUrl + "/stats?");
            urlBuilder.append("start=").append(start);
            urlBuilder.append("&end=").append(end);

            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    urlBuilder.append("&uris=").append(uri);
                }
            }

            if (unique != null) {
                urlBuilder.append("&unique=").append(unique);
            }

            String url = urlBuilder.toString();
            log.debug("Getting stats from: {}", url);

            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );

            List<ViewStatsDto> body = response.getBody();
            log.debug("Stats retrieved: count={}", body != null ? body.size() : 0);
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error getting stats: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
