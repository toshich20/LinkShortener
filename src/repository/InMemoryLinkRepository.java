package repository;

import domain.model.ShortLink;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLinkRepository implements LinkRepository{

    private final Map<String, ShortLink> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ShortLink link) {
        storage.put(link.getShortCode(), link);
    }

    @Override
    public Optional<ShortLink> findByCode(String code) {
        return Optional.ofNullable(storage.get(code));
    }

    @Override
    public List<ShortLink> findByOwnerUuid(UUID ownerUuid) {
        return storage.values().stream()
                .filter(link -> link.getOwnerUuid().equals(ownerUuid))
                .toList();
    }

    @Override
    public boolean deleteByCode(String code) {
        return storage.remove(code) != null;
    }

    @Override
    public List<ShortLink> findAll() {
        return new ArrayList<>(storage.values());
    }
}
