package com.pro.demo;

import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserRepository() {
        // Initialize with some sample data
        User user1 = new User("John Doe", "john.doe@example.com");
        user1.setId(idGenerator.getAndIncrement());
        user1.setPhone("123-456-7890");
        user1.setAddress("123 Main St, City");

        User user2 = new User("Jane Smith", "jane.smith@example.com");
        user2.setId(idGenerator.getAndIncrement());
        user2.setPhone("987-654-3210");
        user2.setAddress("456 Oak Ave, Town");

        users.put(user1.getId(), user1);
        users.put(user2.getId(), user2);
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        users.put(user.getId(), user);
        return user;
    }

    public void deleteById(Long id) {
        users.remove(id);
    }

    public boolean existsByEmail(String email) {
        return users.values().stream()
                .anyMatch(user -> email.equals(user.getEmail()));
    }

    public List<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> email.equals(user.getEmail()))
                .collect(Collectors.toList());
    }

    public List<User> findByNameContainingIgnoreCase(String name) {
        return users.values().stream()
                .filter(user -> user.getName() != null &&
                        user.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public long count() {
        return users.size();
    }

    public void deleteAll() {
        users.clear();
    }
}