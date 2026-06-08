package org.eclipse.jnosql.communication.driver;

import static org.junit.jupiter.api.Assertions.*;

class UUIDValueWriterTest {
    private final UUIDValueWriter writer = new UUIDValueWriter();

    @Nested
    @DisplayName("When evaluating supported types via test()")
    class TestMethodScenarios {

        @Test
        @DisplayName("Should return true when the class type is exactly UUID")
        void shouldReturnTrueWhenClassIsUUID() {
            // Act
            boolean result = writer.test(UUID.class);

            // Assert
            assertThat(result)
                    .as("Writer should support UUID class type")
                    .isTrue();
        }

        @Test
        @DisplayName("Should return false when the class type is not UUID")
        void shouldReturnFalseWhenClassIsNotUUID() {
            // Act
            boolean resultWithString = writer.test(String.class);
            boolean resultWithInteger = writer.test(Integer.class);

            // Assert
            assertThat(resultWithString)
                    .as("Writer should not support String class type")
                    .isFalse();

            assertThat(resultWithInteger)
                    .as("Writer should not support Integer class type")
                    .isFalse();
        }

        @Test
        @DisplayName("Should return false when the class type is null")
        void shouldReturnFalseWhenClassIsNull() {
            // Act
            boolean result = writer.test(null);

            // Assert
            assertThat(result)
                    .as("Writer should handle null type input gracefully and return false")
                    .isFalse();
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
            assertThat(actualString)
                    .as("The written string should match the canonical UUID string representation")
                    .isNotNull()
                    .isEqualTo(expectedString);
        }

        @Test
        @DisplayName("Should return null when the provided UUID parameter is null")
        void shouldReturnNullWhenUUIDIsNull() {
            // Act
            String actualString = writer.write(null);

            // Assert
            assertThat(actualString)
                    .as("Writing a null UUID should result in a null output safely")
                    .isNull();
        }
    }
}