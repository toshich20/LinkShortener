package repository;

import domain.model.ShortLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository {
    void save(ShortLink link);
    Optional<ShortLink> findByCode(String code);
    List<ShortLink> findByOwnerUuid(UUID ownerUuid);
    boolean deleteByCode(String code);
    List<ShortLink> findAll();
}