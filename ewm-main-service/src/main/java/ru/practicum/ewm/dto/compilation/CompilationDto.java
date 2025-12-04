package ru.practicum.ewm.dto.compilation;

import lombok.*;
import ru.practicum.ewm.dto.event.EventShortDto;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {
    private Long id;
    private Set<EventShortDto> events;
    private Boolean pinned;
    private String title;
}

