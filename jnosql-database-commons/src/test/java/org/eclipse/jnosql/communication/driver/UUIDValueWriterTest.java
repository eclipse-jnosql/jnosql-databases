package org.eclipse.jnosql.communication.driver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDValueWriterTest {
    private final UUIDValueWriter writer = new UUIDValueWriter();

    @Nested
    @DisplayName("When evaluating supported types via test()")
    class WhenRecognizingSupportedTypes {

        @Test
        @DisplayName("Should return true when the class type is exactly UUID")
        void shouldReturnTrueWhenClassIsUUID() {
            // Act
            boolean result = writer.test(UUID.class);

            // Assert
            assertThat(result)
                    .isTrue();
        }

        @Test
        @DisplayName("Should return false when the class type is not UUID")
        void shouldReturnFalseWhenClassIsNotUUID() {
            // Act
            boolean resultWithString = writer.test(String.class);
            boolean resultWithInteger = writer.test(Integer.class);

            // Assert
            assertThat(resultWithString).isFalse();

            assertThat(resultWithInteger).isFalse();
        }

        @Test
        @DisplayName("Should return false when the class type is null")
        void shouldReturnFalseWhenClassIsNull() {
            // Act
            boolean result = writer.test(null);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("When converting UUID data via write()")
    class WriteMethodScenarios {

        @Test
        @DisplayName("Should return a valid canonical String representation when UUID is provided")
        void shouldReturnStringRepresentationWhenUUIDIsNotNull() {
            // Arrange
            UUID sampleUuid = UUID.randomUUID();
            String expectedString = sampleUuid.toString();

            // Act
            String actualString = writer.write(sampleUuid);

            // Assert
            assertThat(actualString).isNotNull().isEqualTo(expectedString);
        }

        @Test
        @DisplayName("Should return null when the provided UUID parameter is null")
        void shouldReturnNullWhenUUIDIsNull() {
            // Act
            String actualString = writer.write(null);

            // Assert
            assertThat(actualString).isNull();
        }
    }
}