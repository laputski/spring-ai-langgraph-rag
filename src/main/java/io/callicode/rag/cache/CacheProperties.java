package io.callicode.rag.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    private double similarityThreshold = 0.92;
    private Duration ttl = Duration.ofHours(1);
}
