package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> findAll() {
        return List.copyOf(userStorage.findAll());
    }

    public User findById(long id) {
        return userStorage.findById(id);
    }

    public User add(User user) {
        return userStorage.add(user);
    }

    public User update(User user) {
        User oldUser = userStorage.findById(user.getId());
        user.setFriends(oldUser.getFriends());
        return userStorage.update(user);
    }

    public void addFriend(long userId, long friendId) {
        userStorage.addFriend(userId, friendId);
        log.info("Пользователь id={} добавил в друзья пользователя id={}", userId, friendId);
    }

    public void deleteFriend(long userId, long friendId) {
        userStorage.deleteFriend(userId, friendId);
        log.info("Пользователь id={} удалил из друзей пользователя id={}", userId, friendId);
    }

    public List<User> getFriends(long userId) {
        return userStorage.findFriends(userId);
    }

    public List<User> getCommonFriends(long userId, long otherId) {
        return userStorage.findCommonFriends(userId, otherId);
    }
}
