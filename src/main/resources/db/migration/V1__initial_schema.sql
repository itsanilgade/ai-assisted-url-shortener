CREATE TABLE short_links (
  id UUID PRIMARY KEY,
  short_code VARCHAR(32) NOT NULL UNIQUE,
  original_url VARCHAR(2048) NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE,
  access_count BIGINT NOT NULL DEFAULT 0,
  last_accessed_at TIMESTAMP WITH TIME ZONE,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_short_links_code ON short_links(short_code);
CREATE TABLE click_events (
  id UUID PRIMARY KEY,
  short_code VARCHAR(32) NOT NULL,
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
  referrer VARCHAR(512),
  user_agent VARCHAR(512)
);
CREATE INDEX idx_click_code_time ON click_events(short_code, occurred_at DESC);
