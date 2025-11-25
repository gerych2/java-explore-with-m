package ru.practicum.stats.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsClient {

    private final RestTemplate restTemplate;

    @Value("${stats-server.url:http://stats-server:9090}")
    private String statsServerUrl;

    public void saveHit(EndpointHitDto dto) {
        try {
            String url = statsServerUrl + "/hit";
            HttpEntity<EndpointHitDto> request = new HttpEntity<>(dto);
            restTemplate.postForEntity(url, request, Void.class);
            log.debug("Hit saved: app={}, uri={}, ip={}", dto.getApp(), dto.getUri(), dto.getIp());
        } catch (Exception e) {
            log.error("Error saving hit: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save hit to stats service", e);
        }
    }

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(statsServerUrl + "/stats")
                    .queryParam("start", URLEncoder.encode(start, StandardCharsets.UTF_8))
                    .queryParam("end", URLEncoder.encode(end, StandardCharsets.UTF_8));

            if (uris != null && !uris.isEmpty()) {
                String[] uriArray = uris.toArray(String[]::new);
                builder.queryParam("uris", (Object[]) uriArray);
            }

            if (unique != null) {
                builder.queryParam("unique", unique);
            }

            String url = builder.toUriString();
            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );

            log.debug("Stats retrieved: count={}", response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting stats: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get stats from stats service", e);
        }
    }
}

