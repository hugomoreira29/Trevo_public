package pt.trevo.trevo_api.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.profanity-filter")
public record ProfanityFilterProperties(List<String> blockedWords) {

    public ProfanityFilterProperties {
        if (blockedWords == null) {
            blockedWords = List.of();
        } else {
            blockedWords = List.copyOf(blockedWords);
        }
    }
}
