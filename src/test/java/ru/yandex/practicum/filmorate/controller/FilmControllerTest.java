package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilmControllerTest {
    private final FilmController controller = new FilmController();

    @Test
    @DisplayName("Добавить корректный фильм")
    void createFilmShouldAddValidFilm() {
        Film film = makeFilm();

        Film createdFilm = controller.createFilm(film);

        assertEquals(1, createdFilm.getId());
        assertEquals(1, controller.getFilms().size());
    }

    @Test
    @DisplayName("Выбросить исключение, если название фильма пустое")
    void createFilmShouldThrowExceptionWhenNameIsBlank() {
        Film film = makeFilm();
        film.setName("");

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    @DisplayName("Выбросить исключение, если описание фильма длиннее 200 символов")
    void createFilmShouldThrowExceptionWhenDescriptionIsTooLong() {
        Film film = makeFilm();
        film.setDescription("a".repeat(201));

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    @DisplayName("Выбросить исключение, если дата релиза раньше 28 декабря 1895 года")
    void createFilmShouldThrowExceptionWhenReleaseDateIsTooEarly() {
        Film film = makeFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    @DisplayName("Выбросить исключение, если продолжительность фильма отрицательная")
    void createFilmShouldThrowExceptionWhenDurationIsNegative() {
        Film film = makeFilm();
        film.setDuration(-1);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    @DisplayName("Обновить корректный фильм")
    void updateFilmShouldUpdateValidFilm() {
        Film film = makeFilm();
        Film createdFilm = controller.createFilm(film);

        createdFilm.setName("Updated film");

        Film updatedFilm = controller.updateFilm(createdFilm);

        assertEquals("Updated film", updatedFilm.getName());
        assertEquals(1, controller.getFilms().size());
    }

    @Test
    @DisplayName("Выбросить исключение, если обновляемый фильм не найден")
    void updateFilmShouldThrowExceptionWhenFilmNotFound() {
        Film film = makeFilm();
        film.setId(999);

        assertThrows(NotFoundException.class, () -> controller.updateFilm(film));
    }

    private Film makeFilm() {
        Film film = new Film();
        film.setName("Avatar");
        film.setDescription("Film");
        film.setReleaseDate(LocalDate.of(2009, 12, 18));
        film.setDuration(162);
        return film;
    }
}