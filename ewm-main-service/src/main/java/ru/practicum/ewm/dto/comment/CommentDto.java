package ru.practicum.ewm.dto.comment;

import lombok.*;
import ru.practicum.ewm.dto.user.UserShortDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;
    private String text;
    private Long eventId;
    private UserShortDto author;
    private String created;
    private String edited;
}

