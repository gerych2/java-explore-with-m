package ru.practicum.stats.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {
    private Long id;

    @NotBlank(message = "App must not be blank")
    private String app;

    @NotBlank(message = "URI must not be blank")
    private String uri;

    @NotBlank(message = "IP must not be blank")
    private String ip;

    @NotBlank(message = "Timestamp must not be blank")
    private String timestamp;
}

