package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<User> findAll() {
        List<User> users = jdbcTemplate.query(
                "SELECT user_id, email, login, name, birthday FROM users ORDER BY user_id",
                (rs, rowNum) -> {
                    User user = new User();
                    user.setId(rs.getLong("user_id"));
                    user.setEmail(rs.getString("email"));
                    user.setLogin(rs.getString("login"));
                    user.setName(rs.getString("name"));
                    user.setBirthday(rs.getDate("birthday").toLocalDate());
                    return user;
                }
        );
        users.forEach(user -> user.setFriends(findFriendIds(user.getId())));
        return users;
    }

    @Override
    public User findById(long id) {
        User user = jdbcTemplate.query(
                        "SELECT user_id, email, login, name, birthday FROM users WHERE user_id = ?",
                        (rs, rowNum) -> {
                            User foundUser = new User();
                            foundUser.setId(rs.getLong("user_id"));
                            foundUser.setEmail(rs.getString("email"));
                            foundUser.setLogin(rs.getString("login"));
                            foundUser.setName(rs.getString("name"));
                            foundUser.setBirthday(rs.getDate("birthday").toLocalDate());
                            return foundUser;
                        },
                        id
                ).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
        user.setFriends(findFriendIds(id));
        return user;
    }

    @Override
    public User add(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Не удалось получить id созданного пользователя");
        }
        user.setId(key.longValue());
        log.info("Добавлен пользователь в БД: {}", user);
        return findById(user.getId());
    }

    @Override
    public User update(User user) {
        findById(user.getId());
        jdbcTemplate.update(
                "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?",
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId()
        );
        log.info("Обновлён пользователь в БД: {}", user);
        return findById(user.getId());
    }

    @Override
    public void delete(long id) {
        findById(id);
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", id);
        log.info("Удалён пользователь из БД с id={}", id);
    }

    @Override
    public void addFriend(long userId, long friendId) {
        findById(userId);
        findById(friendId);
        jdbcTemplate.update("MERGE INTO user_friends (user_id, friend_id) KEY (user_id, friend_id) VALUES (?, ?)", userId, friendId);
        log.info("Пользователь id={} добавил в друзья пользователя id={}", userId, friendId);
    }

    @Override
    public void deleteFriend(long userId, long friendId) {
        findById(userId);
        findById(friendId);
        jdbcTemplate.update("DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?", userId, friendId);
        log.info("Пользователь id={} удалил из друзей пользователя id={}", userId, friendId);
    }

    private Set<Long> findFriendIds(long userId) {
        return new HashSet<>(jdbcTemplate.query(
                "SELECT friend_id FROM user_friends WHERE user_id = ?",
                (rs, rowNum) -> rs.getLong("friend_id"),
                userId
        ));
    }
}
