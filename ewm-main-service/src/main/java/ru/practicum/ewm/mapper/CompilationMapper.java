package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {

    public Compilation toEntity(NewCompilationDto dto, Set<Event> events) {
        return Compilation.builder()
                .events(events != null ? events : new HashSet<>())
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .title(dto.getTitle())
                .build();
    }

    public CompilationDto toDto(Compilation compilation) {
        Set<EventShortDto> eventDtos = compilation.getEvents() != null
                ? compilation.getEvents().stream()
                    .map(EventMapper::toShortDto)
                    .collect(Collectors.toSet())
                : new HashSet<>();

        return CompilationDto.builder()
                .id(compilation.getId())
                .events(eventDtos)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }
}


