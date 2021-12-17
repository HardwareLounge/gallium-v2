package net.hardwarelounge.gallium.config;

import lombok.*;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class SecurityConfig extends DefaultConfigFactory {

    private long minAccountAgeSeconds;
    private int spamWordCountAutoMod;
    private int blacklistRepeatAutoLimit;
    private Set<String> blacklistedPhrases;
    private Set<String> potentialSpamPhrases;

    public static DefaultConfigFactory createDefault() {
        return SecurityConfig.builder()
                .minAccountAgeSeconds(Duration.ofDays(2).toSeconds())
                .spamWordCountAutoMod(3)
                .blacklistRepeatAutoLimit(3)
                .blacklistedPhrases(new HashSet<>())
                .blacklistedPhrases(new HashSet<>())
                .build();
    }
}
