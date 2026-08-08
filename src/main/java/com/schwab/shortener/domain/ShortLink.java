package com.schwab.shortener.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "short_links", indexes = {@Index(name = "idx_short_links_code", columnList = "short_code", unique = true)})
public class ShortLink {
    @Id private UUID id;
    @Column(name="short_code", nullable=false, length=32, unique=true) private String shortCode;
    @Column(name="original_url", nullable=false, length=2048) private String originalUrl;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) private LinkStatus status;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="expires_at") private Instant expiresAt;
    @Column(name="access_count", nullable=false) private long accessCount;
    @Column(name="last_accessed_at") private Instant lastAccessedAt;
    @Version private long version;

    protected ShortLink() {}
    public ShortLink(UUID id, String shortCode, String originalUrl, Instant createdAt, Instant expiresAt) {
        this.id=id; this.shortCode=shortCode; this.originalUrl=originalUrl; this.createdAt=createdAt; this.expiresAt=expiresAt;
        this.status=LinkStatus.ACTIVE; this.accessCount=0;
    }
    public UUID getId(){return id;} public String getShortCode(){return shortCode;} public String getOriginalUrl(){return originalUrl;}
    public LinkStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getExpiresAt(){return expiresAt;}
    public long getAccessCount(){return accessCount;} public Instant getLastAccessedAt(){return lastAccessedAt;}
    public boolean isExpired(Instant now){return expiresAt!=null && !expiresAt.isAfter(now);} public boolean isActive(Instant now){return status==LinkStatus.ACTIVE && !isExpired(now);}
    public void deactivate(){this.status=LinkStatus.INACTIVE;} public void recordAccess(Instant now){accessCount++; lastAccessedAt=now;}
}
