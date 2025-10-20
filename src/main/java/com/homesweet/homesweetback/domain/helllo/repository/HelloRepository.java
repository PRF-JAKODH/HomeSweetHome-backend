package com.homesweet.homesweetback.domain.helllo.repository;

import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Hello 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
@Repository
public class HelloRepository {

    private final Map<Long, HelloEntity> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(0);

    public HelloEntity save(String name) {
        long id = seq.incrementAndGet();
        HelloEntity e = new HelloEntity(id, name);
        store.put(id, e);
        return e;
    }

    public boolean existsByName(String name) {
        return store.values().stream().anyMatch(e -> e.getName().equalsIgnoreCase(name));
    }
}
