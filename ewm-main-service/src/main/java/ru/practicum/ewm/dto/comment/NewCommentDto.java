package ru.practicum.ewm.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {

    @NotBlank(message = "Comment text must not be blank")
    @Size(min = 1, max = 2000, message = "Comment text must be between 1 and 2000 characters")
    private String text;
}

