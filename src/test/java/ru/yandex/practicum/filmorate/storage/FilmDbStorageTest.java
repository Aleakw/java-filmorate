package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, UserDbStorage.class})
class FilmDbStorageTest {
    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;

    @Autowired
    FilmDbStorageTest(FilmDbStorage filmStorage, UserDbStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    @Test
    void addAndFindByIdShouldSaveFilmWithMpaAndGenres() {
        Film savedFilm = filmStorage.add(makeFilm("Avatar"));

        Film foundFilm = filmStorage.findById(savedFilm.getId());

        assertThat(foundFilm.getName()).isEqualTo("Avatar");
        assertThat(foundFilm.getMpa()).isEqualTo(new Mpa(1, "G"));
        assertThat(foundFilm.getGenres())
                .extracting(Genre::getId)
                .containsExactly(1, 2);
    }

    @Test
    void findAllShouldReturnSavedFilms() {
        filmStorage.add(makeFilm("First film"));
        filmStorage.add(makeFilm("Second film"));

        assertThat(filmStorage.findAll()).hasSize(2);
    }

    @Test
    void updateShouldChangeFilmFields() {
        Film savedFilm = filmStorage.add(makeFilm("Old film"));
        savedFilm.setName("New film");
        savedFilm.setDuration(120);
        savedFilm.setMpa(new Mpa(2, null));
        savedFilm.setGenres(new LinkedHashSet<>(List.of(new Genre(3, null))));

        Film updatedFilm = filmStorage.update(savedFilm);

        assertThat(updatedFilm.getName()).isEqualTo("New film");
        assertThat(updatedFilm.getDuration()).isEqualTo(120);
        assertThat(updatedFilm.getMpa()).isEqualTo(new Mpa(2, "PG"));
        assertThat(updatedFilm.getGenres())
                .extracting(Genre::getId)
                .containsExactly(3);
    }

    @Test
    void deleteShouldRemoveFilm() {
        Film savedFilm = filmStorage.add(makeFilm("Delete film"));

        filmStorage.delete(savedFilm.getId());

        assertThatThrownBy(() -> filmStorage.findById(savedFilm.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addAndDeleteLikeShouldChangeFilmLikes() {
        Film film = filmStorage.add(makeFilm("Liked film"));
        User user = userStorage.add(makeUser());

        filmStorage.addLike(film.getId(), user.getId());

        assertThat(filmStorage.findById(film.getId()).getLikes()).containsExactly(user.getId());

        filmStorage.deleteLike(film.getId(), user.getId());

        assertThat(filmStorage.findById(film.getId()).getLikes()).isEmpty();
    }

    private Film makeFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2009, 12, 18));
        film.setDuration(162);
        film.setMpa(new Mpa(1, null));
        film.setGenres(new LinkedHashSet<>(List.of(new Genre(1, null), new Genre(2, null))));
        return film;
    }

    private User makeUser() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("user");
        user.setName("User");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
