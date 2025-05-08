package ru.nexign.cdr.model;

import lombok.*;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CDR implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private String callerNumber;
    private String calleeNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

