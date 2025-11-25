package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.mapper.StatsMapper;
import ru.practicum.stats.model.EndpointHit;
import ru.practicum.stats.model.ViewStats;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsRepository statsRepository;

    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto endpointHitDto) {
        EndpointHit endpointHit = StatsMapper.toEndpointHit(endpointHitDto);
        EndpointHit saved = statsRepository.save(endpointHit);
        return StatsMapper.toEndpointHitDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        LocalDateTime startTime = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(end, FORMATTER);

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        List<String> uriFilter = (uris == null || uris.isEmpty()) ? null : uris;
        List<ViewStats> stats;
        if (Boolean.TRUE.equals(unique)) {
            stats = statsRepository.getUniqueStats(startTime, endTime, uriFilter);
        } else {
            stats = statsRepository.getStats(startTime, endTime, uriFilter);
        }

        return stats.stream()
                .map(StatsMapper::toViewStatsDto)
                .collect(Collectors.toList());
    }
}

