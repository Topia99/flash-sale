package com.flashSale.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Getter @Setter
public class EventCreateRequest {

    @NotBlank(message = "title is required")
    @Size(max = 128, message = "title max length is 128")
    private String title;

    @Size(max = 2000, message = "description max length is 2000")
    private String description;

    @NotNull(message = "startsAt is required")
    private Instant startsAt;

    @NotNull(message = "endsAt is required")
    private Instant endsAt;
}
