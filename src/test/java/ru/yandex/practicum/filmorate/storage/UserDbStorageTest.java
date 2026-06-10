package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
class UserDbStorageTest {
    private final UserDbStorage userStorage;

    @Autowired
    UserDbStorageTest(UserDbStorage userStorage) {
        this.userStorage = userStorage;
    }

    @Test
    void addAndFindByIdShouldSaveUser() {
        User savedUser = userStorage.add(makeUser("user@mail.ru", "user"));

        User foundUser = userStorage.findById(savedUser.getId());

        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.getEmail()).isEqualTo("user@mail.ru");
    }

    @Test
    void findAllShouldReturnSavedUsers() {
        userStorage.add(makeUser("first@mail.ru", "first"));
        userStorage.add(makeUser("second@mail.ru", "second"));

        assertThat(userStorage.findAll()).hasSize(2);
    }

    @Test
    void updateShouldChangeUserFields() {
        User savedUser = userStorage.add(makeUser("old@mail.ru", "old"));
        savedUser.setEmail("new@mail.ru");
        savedUser.setLogin("new");
        savedUser.setName("New name");

        User updatedUser = userStorage.update(savedUser);

        assertThat(updatedUser.getEmail()).isEqualTo("new@mail.ru");
        assertThat(updatedUser.getLogin()).isEqualTo("new");
        assertThat(updatedUser.getName()).isEqualTo("New name");
    }

    @Test
    void deleteShouldRemoveUser() {
        User savedUser = userStorage.add(makeUser("delete@mail.ru", "delete"));

        userStorage.delete(savedUser.getId());

        assertThatThrownBy(() -> userStorage.findById(savedUser.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addAndDeleteFriendShouldChangeOnlyUserFriends() {
        User user = userStorage.add(makeUser("user@mail.ru", "user"));
        User friend = userStorage.add(makeUser("friend@mail.ru", "friend"));

        userStorage.addFriend(user.getId(), friend.getId());

        assertThat(userStorage.findById(user.getId()).getFriends()).containsExactly(friend.getId());
        assertThat(userStorage.findById(friend.getId()).getFriends()).isEmpty();

        userStorage.deleteFriend(user.getId(), friend.getId());

        assertThat(userStorage.findById(user.getId()).getFriends()).isEmpty();
    }

    private User makeUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login + " name");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
