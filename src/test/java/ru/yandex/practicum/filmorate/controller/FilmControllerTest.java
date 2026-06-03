package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilmControllerTest {
    private final FilmController controller = new FilmController();

    @Test
    void createFilmShouldAddValidFilm() {
        Film film = makeFilm();

        Film createdFilm = controller.createFilm(film);

        assertEquals(1, createdFilm.getId());
        assertEquals(1, controller.getFilms().size());
    }

    @Test
    void createFilmShouldThrowExceptionWhenNameIsBlank() {
        Film film = makeFilm();
        film.setName(" ");

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void createFilmShouldThrowExceptionWhenDescriptionIsLongerThan200Symbols() {
        Film film = makeFilm();
        film.setDescription("a".repeat(201));

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void createFilmShouldThrowExceptionWhenReleaseDateIsBeforeFirstFilmReleaseDate() {
        Film film = makeFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void createFilmShouldThrowExceptionWhenDurationIsNotPositive() {
        Film film = makeFilm();
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    private Film makeFilm() {
        Film film = new Film();
        film.setName("Film name");
        film.setDescription("Film description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }
}
