package ru.nexign.cdr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CDRStatsResponse {
    private long totalSubscribers;
    private long totalCalls;
    private long totalFilesSent;
    private String lastGeneratedFile;
    private long errorsLogged;
}
