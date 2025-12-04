package ru.practicum.ewm.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequest {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    @Size(min = 6, max = 254, message = "Email must be between 6 and 254 characters")
    private String email;

    @NotBlank(message = "Name must not be blank")
    @Size(min = 2, max = 250, message = "Name must be between 2 and 250 characters")
    private String name;
}


