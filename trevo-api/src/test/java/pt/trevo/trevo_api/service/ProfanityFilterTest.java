package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProfanityFilterTest {

    private final ProfanityFilter profanityFilter = new ProfanityFilter(
        new ProfanityFilterProperties(List.of("badword", "merda", "cabrão", "cabrao", "porra"))
    );

    @ParameterizedTest
    @ValueSource(strings = {"badword", "BadWord", "BADWORD"})
    void detectsCaseVariants(String comment) {
        assertThat(profanityFilter.containsBlockedWord(comment)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "badword no inicio",
        "comentario com badword no meio",
        "comentario termina em badword"
    })
    void detectsWordAtStartMiddleAndEnd(String comment) {
        assertThat(profanityFilter.containsBlockedWord(comment)).isTrue();
    }

    @Test
    void detectsMultipleBlockedWordsInOneComment() {
        assertThat(profanityFilter.containsBlockedWord("Isto e merda, badword e porra.")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"badword!", "badword.", "(badword)"})
    void detectsBlockedWordWithAdjacentPunctuation(String comment) {
        assertThat(profanityFilter.containsBlockedWord(comment)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"merda", "cabrão", "cabrao", "porra"})
    void detectsPortugueseBlockedWordsAndVariants(String comment) {
        assertThat(profanityFilter.containsBlockedWord(comment)).isTrue();
    }

    @Test
    void ignoresNullAndWhitespaceOnlyComments() {
        assertThat(profanityFilter.containsBlockedWord(null)).isFalse();
        assertThat(profanityFilter.containsBlockedWord("")).isFalse();
        assertThat(profanityFilter.containsBlockedWord("   ")).isFalse();
    }

    @Test
    void ignoresCleanComment() {
        assertThat(profanityFilter.containsBlockedWord("Servico rapido e produto impecavel.")).isFalse();
    }
}
