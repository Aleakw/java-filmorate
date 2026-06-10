package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public User findById(long id) {
        User user = users.get(id);
        if (user == null) {
            log.warn("Пользователь с id={} не найден", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        return user;
    }

    @Override
    public User add(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Добавлен пользователь: {}", user);
        return user;
    }

    @Override
    public User update(User user) {
        if (!users.containsKey(user.getId())) {
            log.warn("Пользователь с id={} не найден для обновления", user.getId());
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }
        users.put(user.getId(), user);
        log.info("Обновлён пользователь: {}", user);
        return user;
    }

    @Override
    public void delete(long id) {
        if (users.remove(id) == null) {
            log.warn("Пользователь с id={} не найден для удаления", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        log.info("Удалён пользователь с id={}", id);
    }

    @Override
    public void addFriend(long userId, long friendId) {
        User user = findById(userId);
        findById(friendId);
        user.getFriends().add(friendId);
        log.info("Пользователь id={} добавил в друзья пользователя id={}", userId, friendId);
    }

    @Override
    public void deleteFriend(long userId, long friendId) {
        User user = findById(userId);
        findById(friendId);
        user.getFriends().remove(friendId);
        log.info("Пользователь id={} удалил из друзей пользователя id={}", userId, friendId);
    }

    @Override
    public List<User> findFriends(long userId) {
        User user = findById(userId);
        return user.getFriends().stream()
                .map(this::findById)
                .toList();
    }

    @Override
    public List<User> findCommonFriends(long userId, long otherId) {
        User user = findById(userId);
        User otherUser = findById(otherId);
        return user.getFriends().stream()
                .filter(otherUser.getFriends()::contains)
                .map(this::findById)
                .toList();
    }
}
