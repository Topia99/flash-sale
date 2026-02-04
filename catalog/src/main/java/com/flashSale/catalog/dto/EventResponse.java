package com.flashSale.catalog.dto;


import com.flashSale.catalog.domain.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private Instant startsAt;
    private Instant endsAt;
    private String status;
}
