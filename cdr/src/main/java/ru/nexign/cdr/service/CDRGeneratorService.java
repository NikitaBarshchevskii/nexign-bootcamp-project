package ru.nexign.cdr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.nexign.cdr.config.RabbitConfig;
import ru.nexign.cdr.dto.CDRMessage;
import ru.nexign.cdr.dto.CDRStatsResponse;
import ru.nexign.cdr.model.CDR;
import ru.nexign.cdr.model.Subscriber;
import ru.nexign.cdr.repository.CDRRepository;
import ru.nexign.cdr.repository.SubscriberRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CDRGeneratorService {

    private final SubscriberRepository subscriberRepository;
    private final CDRRepository cdrRepository;
    private final RabbitTemplate rabbitTemplate;

    private final Random random = new Random();
    private final int subscriberCount = 30;
    private final int callsPerSubscriber = 100;

    private String lastSentFileName;
    private int fileCounter = 0;

    public void generate() {
        generateSubscribers();
        generateCDRRecords();
    }

    private void generateSubscribers() {
        if (subscriberRepository.count() >= subscriberCount) return;

        for (int i = 0; i < subscriberCount; i++) {
            String msisdn = "79" + String.format("%09d", random.nextInt(1_000_000_000));
            subscriberRepository.save(Subscriber.builder().msisdn(msisdn).build());
        }
    }

    private void generateCDRRecords() {
        List<CDR> validCDRs = new ArrayList<>();
        List<Subscriber> subscribers = subscriberRepository.findAll();

        for (Subscriber from : subscribers) {
            for (int i = 0; i < callsPerSubscriber; i++) {
                Subscriber to;
                do {
                    to = subscribers.get(random.nextInt(subscribers.size()));
                } while (to.getMsisdn().equals(from.getMsisdn()));

                LocalDateTime start = randomDateInLastYear();
                LocalDateTime end = start.plusSeconds(30 + random.nextInt(300));
                String type = random.nextBoolean() ? "01" : "02";

                CDR cdr = CDR.builder()
                        .type(type)
                        .callerNumber(from.getMsisdn())
                        .calleeNumber(to.getMsisdn())
                        .startTime(start)
                        .endTime(end)
                        .build();

                if (isValid(cdr)) {
                    validCDRs.add(cdr);
                } else {
                    logToErrorFile(cdr);
                }

                if (validCDRs.size() == 10) {
                    sendToRabbit(validCDRs);
                    validCDRs.clear();
                }
            }
        }

        if (!validCDRs.isEmpty()) {
            sendToRabbit(validCDRs);
        }
    }

    private boolean isValid(CDR cdr) {
        return cdr.getCallerNumber() != null &&
                cdr.getCalleeNumber() != null &&
                cdr.getStartTime() != null &&
                cdr.getEndTime() != null &&
                cdr.getCallerNumber().length() == 11 &&
                cdr.getCalleeNumber().length() == 11 &&
                !cdr.getCallerNumber().equals(cdr.getCalleeNumber()) &&
                cdr.getStartTime().isBefore(cdr.getEndTime());
    }

    private void logToErrorFile(CDR cdr) {
        try {
            Path logDir = Path.of("logs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            Path logFile = logDir.resolve("cdr_errors.log");
            try (FileWriter writer = new FileWriter(logFile.toFile(), true)) {
                writer.write("[" + LocalDateTime.now() + "] Invalid CDR: " + cdr + System.lineSeparator());
            }
        } catch (Exception e) {
            System.err.println("Ошибка при логировании в cdr_errors.log: " + e.getMessage());
        }
    }

    private void sendToRabbit(List<CDR> cdrs) {
        cdrRepository.saveAll(cdrs);

        String content = cdrs.stream()
                .map(cdr -> String.join(",",
                        cdr.getType(),
                        cdr.getCallerNumber(),
                        cdr.getCalleeNumber(),
                        cdr.getStartTime().toString(),
                        cdr.getEndTime().toString()))
                .collect(Collectors.joining("\n"));

        String fileName = "cdr_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".csv";

        lastSentFileName = fileName;
        fileCounter++;

        CDRMessage message = new CDRMessage(fileName, content);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, message);

        System.out.println("Отправлено в RabbitMQ: " + fileName + "\n" + content);
    }

    public CDRStatsResponse getStats() {
        long subscribers = subscriberRepository.count();
        long calls = cdrRepository.count();

        Path logFile = Path.of("logs/cdr_errors.log");
        long errors = 0;
        try {
            if (Files.exists(logFile)) {
                errors = Files.lines(logFile).count();
            }
        } catch (IOException ignored) {}

        String lastFile = lastSentFileName != null ? lastSentFileName : "_";

        return new CDRStatsResponse(subscribers, calls, fileCounter, lastFile, errors);
    }

    private LocalDateTime randomDateInLastYear() {
        LocalDateTime now = LocalDateTime.now();
        return now.minusDays(random.nextInt(365))
                .withHour(random.nextInt(24))
                .withMinute(random.nextInt(60))
                .withSecond(random.nextInt(60))
                .withNano(0);
    }
}
