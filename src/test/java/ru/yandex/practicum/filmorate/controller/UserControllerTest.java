package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserControllerTest {
    private final UserController controller = new UserController(new UserService(new InMemoryUserStorage()));

    @Test
    void createUserShouldAddValidUser() {
        User user = makeUser();

        User createdUser = controller.createUser(user);

        assertEquals(1, createdUser.getId());
        assertEquals(1, controller.getUsers().size());
    }

    @Test
    void createUserShouldUseLoginWhenNameIsBlank() {
        User user = makeUser();
        user.setName(" ");

        User createdUser = controller.createUser(user);

        assertEquals(user.getLogin(), createdUser.getName());
    }

    @Test
    void createUserShouldThrowExceptionWhenEmailIsBlank() {
        User user = makeUser();
        user.setEmail(" ");

        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUserShouldThrowExceptionWhenEmailDoesNotContainAtSign() {
        User user = makeUser();
        user.setEmail("mail.ru");

        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUserShouldThrowExceptionWhenLoginContainsSpace() {
        User user = makeUser();
        user.setLogin("bad login");

        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUserShouldThrowExceptionWhenBirthdayIsInFuture() {
        User user = makeUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    private User makeUser() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("userLogin");
        user.setName("User name");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
