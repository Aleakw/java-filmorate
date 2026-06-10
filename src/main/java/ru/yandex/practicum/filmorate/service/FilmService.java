package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public List<Film> findAll() {
        return List.copyOf(filmStorage.findAll());
    }

    public Film findById(long id) {
        return filmStorage.findById(id);
    }

    public Film add(Film film) {
        return filmStorage.add(film);
    }

    public Film update(Film film) {
        filmStorage.findById(film.getId());
        return filmStorage.update(film);
    }

    public void addLike(long filmId, long userId) {
        filmStorage.findById(filmId);
        userStorage.findById(userId);
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
    }

    public void deleteLike(long filmId, long userId) {
        filmStorage.findById(filmId);
        userStorage.findById(userId);
        filmStorage.deleteLike(filmId, userId);
        log.info("Пользователь id={} удалил лайк у фильма id={}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .toList();
    }
}
