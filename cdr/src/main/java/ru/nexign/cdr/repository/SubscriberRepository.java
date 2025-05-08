package ru.nexign.cdr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nexign.cdr.model.Subscriber;

public interface SubscriberRepository extends JpaRepository<Subscriber, String> {
}
