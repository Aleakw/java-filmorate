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
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        log.info("Пользователь id={} и пользователь id={} стали друзьями", userId, friendId);
    }

    public void deleteFriend(long userId, long friendId) {
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        log.info("Пользователь id={} и пользователь id={} больше не друзья", userId, friendId);
    }

    public List<User> getFriends(long userId) {
        User user = userStorage.findById(userId);
        return user.getFriends().stream()
                .map(userStorage::findById)
                .toList();
    }

    public List<User> getCommonFriends(long userId, long otherId) {
        User user = userStorage.findById(userId);
        User otherUser = userStorage.findById(otherId);
        return user.getFriends().stream()
                .filter(otherUser.getFriends()::contains)
                .map(userStorage::findById)
                .toList();
    }
}
