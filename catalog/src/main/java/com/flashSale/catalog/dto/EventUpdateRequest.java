package com.flashSale.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EventUpdateRequest {

    @Size(max = 128, message = "title max length is 128")
    private String title;

    @Size(max = 2000, message = "description max length is 2000")
    private String description;

    private Instant startsAt;
    private Instant endsAt;
}
