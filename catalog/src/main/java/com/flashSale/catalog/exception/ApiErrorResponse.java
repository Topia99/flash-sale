package com.flashSale.catalog.exception;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;     // e.g. "Bad Request"
    private String code;      // e.g. "VALIDATION_ERROR"
    private String message;   // human readable
    private String path;
    private Map<String, String> fieldErrors; // optional
}
