package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final StatsClient statsClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Private API methods

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User initiator = userService.getUserById(userId);
        Category category = categoryService.getCategoryById(dto.getCategory());

        LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), FORMATTER);
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }

        Event event = EventMapper.toEntity(dto, category, initiator);
        Event savedEvent = eventRepository.save(event);
        log.info("Event created: id={}, title={}", savedEvent.getId(), savedEvent.getTitle());
        return EventMapper.toFullDto(savedEvent);
    }

    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        userService.getUserById(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        return events.stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {
        userService.getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return EventMapper.toFullDto(event);
    }

    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        userService.getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        updateEventFields(event, request.getAnnotation(), request.getCategory(), request.getDescription(),
                request.getEventDate(), request.getLocation(), request.getPaid(),
                request.getParticipantLimit(), request.getRequestModeration(), request.getTitle());

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event savedEvent = eventRepository.save(event);
        log.info("Event updated by user: id={}", savedEvent.getId());
        return EventMapper.toFullDto(savedEvent);
    }

    // Admin API methods

    public List<EventFullDto> getEventsAdmin(List<Long> users, List<String> states, List<Long> categories,
                                              String rangeStart, String rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        List<EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        Page<Event> events = eventRepository.findEventsAdmin(users, eventStates, categories, start, end, pageable);

        List<Event> eventList = events.getContent();
        setViewsToEvents(eventList);

        return eventList.stream()
                .map(EventMapper::toFullDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventById(eventId);

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Cannot publish the event because it's not in the right state: "
                                + event.getState());
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Event date must be at least 1 hour from publication");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already published");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        updateEventFields(event, request.getAnnotation(), request.getCategory(), request.getDescription(),
                request.getEventDate(), request.getLocation(), request.getPaid(),
                request.getParticipantLimit(), request.getRequestModeration(), request.getTitle());

        Event savedEvent = eventRepository.save(event);
        log.info("Event updated by admin: id={}", savedEvent.getId());
        return EventMapper.toFullDto(savedEvent);
    }

    // Public API methods

    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                                String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                                String sort, int from, int size, String ip, String uri) {
        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Start date must be before end date");
        }

        if (start == null && end == null) {
            start = LocalDateTime.now();
        }

        Pageable pageable;
        if ("EVENT_DATE".equals(sort)) {
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate"));
        } else {
            pageable = PageRequest.of(from / size, size);
        }

        Page<Event> events = eventRepository.findEventsPublic(text, categories, paid, start, end,
                onlyAvailable != null ? onlyAvailable : false, pageable);

        List<Event> eventList = new ArrayList<>(events.getContent());
        setViewsToEvents(eventList);

        if ("VIEWS".equals(sort)) {
            eventList.sort(Comparator.comparing(Event::getViews).reversed());
        }

        saveHit(ip, uri);

        return eventList.stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    public EventFullDto getEventPublic(Long id, String ip, String uri) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        setViewsToEvents(List.of(event));
        saveHit(ip, uri);

        return EventMapper.toFullDto(event);
    }

    // Helper methods

    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    public List<Event> getEventsByIds(Set<Long> ids) {
        return eventRepository.findByIdIn(ids);
    }

    private void updateEventFields(Event event, String annotation, Long categoryId, String description,
                                   String eventDate, LocationDto location, Boolean paid,
                                   Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (categoryId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            event.setCategory(category);
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (eventDate != null) {
            LocalDateTime newEventDate = LocalDateTime.parse(eventDate, FORMATTER);
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Event date must be at least 2 hours from now");
            }
            event.setEventDate(newEventDate);
        }
        if (location != null) {
            event.setLocation(LocationMapper.toEntity(location));
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (title != null) {
            event.setTitle(title);
        }
    }

    private void setViewsToEvents(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        LocalDateTime minDate = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    minDate.format(FORMATTER),
                    LocalDateTime.now().format(FORMATTER),
                    uris,
                    true
            );

            Map<String, Long> viewsMap = stats.stream()
                    .collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits));

            for (Event event : events) {
                String uri = "/events/" + event.getId();
                event.setViews(viewsMap.getOrDefault(uri, 0L));
            }
        } catch (Exception e) {
            log.error("Failed to get views from stats service: {}", e.getMessage());
            events.forEach(event -> event.setViews(0L));
        }
    }

    private void saveHit(String ip, String uri) {
        try {
            EndpointHitDto hit = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri(uri)
                    .ip(ip)
                    .timestamp(LocalDateTime.now().format(FORMATTER))
                    .build();
            statsClient.saveHit(hit);
        } catch (Exception e) {
            log.error("Failed to save hit to stats service: {}", e.getMessage());
        }
    }

    @Transactional
    public void updateConfirmedRequests(Long eventId, long count) {
        Event event = getEventById(eventId);
        event.setConfirmedRequests(count);
        eventRepository.save(event);
    }
}

