package ru.nexign.cdr.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.nexign.cdr.dto.CDRStatsResponse;
import ru.nexign.cdr.model.CDR;
import ru.nexign.cdr.model.Subscriber;
import ru.nexign.cdr.repository.CDRRepository;
import ru.nexign.cdr.repository.SubscriberRepository;
import ru.nexign.cdr.service.CDRGeneratorService;

import java.util.List;

@RestController
@RequestMapping("/api/cdr")
@RequiredArgsConstructor
public class CDRController {

    private final CDRGeneratorService cdrGeneratorService;
    private final SubscriberRepository subscriberRepository;
    private final CDRRepository cdrRepository;

    @GetMapping("/generate")
    public String generateCDR() {
        cdrGeneratorService.generate();
        return "CDR генерация запущена";
    }

    @GetMapping("/subscribers")
    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findAll();
    }

    @GetMapping("/cdr")
    public List<CDR> getAllCDRRecords() {
        return cdrRepository.findAll();
    }

    @GetMapping("/stats")
    public CDRStatsResponse getStats() {
        return cdrGeneratorService.getStats();
    }
}
