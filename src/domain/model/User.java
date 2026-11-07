package domain.model;

import java.util.Objects;
import java.util.UUID;

public record User(UUID id) {
    public User {
        Objects.requireNonNull(id, "ID не может быть null");
    }
}
