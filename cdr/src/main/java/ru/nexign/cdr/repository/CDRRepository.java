package ru.nexign.cdr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nexign.cdr.model.CDR;

import java.time.LocalDateTime;
import java.util.List;

public interface CDRRepository extends JpaRepository<CDR, Long> {

    List<CDR> findByCallerNumberOrCalleeNumberAndStartTimeBetween(
            String caller, String callee, LocalDateTime from, LocalDateTime to
    );

    List<CDR> findByCallerNumberOrCalleeNumber(String caller, String callee);
}
