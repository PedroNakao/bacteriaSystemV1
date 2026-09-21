package com.example.bacteriaSystem.exceptions;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ErroResponse {
    private LocalDateTime timestamp;
    private String status;
    private String message;
    private int httpStatus;
    private String url;
}
