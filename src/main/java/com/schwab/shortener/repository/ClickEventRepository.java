package com.schwab.shortener.repository;

import com.schwab.shortener.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {
    List<ClickEvent> findTop100ByShortCodeOrderByOccurredAtDesc(String shortCode);
}
