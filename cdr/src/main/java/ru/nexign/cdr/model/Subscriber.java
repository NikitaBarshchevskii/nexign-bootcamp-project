package ru.nexign.cdr.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscriber implements Serializable {

    @Id
    private String msisdn;
}
