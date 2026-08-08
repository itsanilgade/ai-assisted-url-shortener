package com.schwab.shortener.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="click_events", indexes={@Index(name="idx_click_code_time", columnList="short_code,occurred_at")})
public class ClickEvent {
    @Id private UUID id;
    @Column(name="short_code", nullable=false, length=32) private String shortCode;
    @Column(name="occurred_at", nullable=false) private Instant occurredAt;
    @Column(length=512) private String referrer;
    @Column(name="user_agent", length=512) private String userAgent;
    protected ClickEvent() {}
    public ClickEvent(UUID id,String shortCode,Instant occurredAt,String referrer,String userAgent){this.id=id;this.shortCode=shortCode;this.occurredAt=occurredAt;this.referrer=referrer;this.userAgent=userAgent;}
    public String getReferrer(){return referrer;} public Instant getOccurredAt(){return occurredAt;}
}
