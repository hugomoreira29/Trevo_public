package pt.trevo.trevo_api.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProfanityFilter {

    private static final Pattern WORD_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final Set<String> blockedWords;

    public ProfanityFilter(ProfanityFilterProperties properties) {
        this.blockedWords = properties.blockedWords().stream()
            .filter(word -> word != null && !word.isBlank())
            .map(word -> word.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }

    public boolean containsBlockedWord(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        for (String word : WORD_SEPARATOR.split(normalized)) {
            if (blockedWords.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
