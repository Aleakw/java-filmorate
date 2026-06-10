package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<Film> findAll() {
        List<Film> films = jdbcTemplate.query(
                "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, "
                        + "m.mpa_id, m.name AS mpa_name "
                        + "FROM films f LEFT JOIN mpa m ON f.mpa_id = m.mpa_id ORDER BY f.film_id",
                (rs, rowNum) -> mapFilmWithoutCollections(rs)
        );
        films.forEach(this::fillCollections);
        return films;
    }

    @Override
    public Film findById(long id) {
        Film film = jdbcTemplate.query(
                        "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, "
                                + "m.mpa_id, m.name AS mpa_name "
                                + "FROM films f LEFT JOIN mpa m ON f.mpa_id = m.mpa_id WHERE f.film_id = ?",
                        (rs, rowNum) -> mapFilmWithoutCollections(rs),
                        id
                ).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
        fillCollections(film);
        return film;
    }

    @Override
    public Film add(Film film) {
        validateMpa(film);
        validateGenres(film);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setLong(4, film.getDuration());
            if (film.getMpa() == null || film.getMpa().getId() == 0) {
                ps.setObject(5, null);
            } else {
                ps.setInt(5, film.getMpa().getId());
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Не удалось получить id созданного фильма");
        }
        film.setId(key.longValue());
        saveGenres(film);
        log.info("Добавлен фильм в БД: {}", film);
        return findById(film.getId());
    }

    @Override
    public Film update(Film film) {
        findById(film.getId());
        validateMpa(film);
        validateGenres(film);
        jdbcTemplate.update(
                "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?",
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() == null || film.getMpa().getId() == 0 ? null : film.getMpa().getId(),
                film.getId()
        );
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        saveGenres(film);
        log.info("Обновлён фильм в БД: {}", film);
        return findById(film.getId());
    }

    @Override
    public void delete(long id) {
        findById(id);
        jdbcTemplate.update("DELETE FROM films WHERE film_id = ?", id);
        log.info("Удалён фильм из БД с id={}", id);
    }

    @Override
    public void addLike(long filmId, long userId) {
        findById(filmId);
        jdbcTemplate.update("MERGE INTO film_likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)", filmId, userId);
        log.info("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
    }

    @Override
    public void deleteLike(long filmId, long userId) {
        findById(filmId);
        jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ? AND user_id = ?", filmId, userId);
        log.info("Пользователь id={} удалил лайк у фильма id={}", userId, filmId);
    }

    private Film mapFilmWithoutCollections(java.sql.ResultSet rs) throws java.sql.SQLException {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getLong("duration"));

        int mpaId = rs.getInt("mpa_id");
        if (!rs.wasNull()) {
            film.setMpa(new Mpa(mpaId, rs.getString("mpa_name")));
        }
        return film;
    }

    private void fillCollections(Film film) {
        film.setGenres(findGenres(film.getId()));
        film.setLikes(findLikes(film.getId()));
    }

    private Set<Genre> findGenres(long filmId) {
        return new LinkedHashSet<>(jdbcTemplate.query(
                "SELECT g.genre_id, g.name FROM genres g "
                        + "JOIN film_genres fg ON g.genre_id = fg.genre_id "
                        + "WHERE fg.film_id = ? ORDER BY g.genre_id",
                (rs, rowNum) -> new Genre(rs.getInt("genre_id"), rs.getString("name")),
                filmId
        ));
    }

    private Set<Long> findLikes(long filmId) {
        return new HashSet<>(jdbcTemplate.query(
                "SELECT user_id FROM film_likes WHERE film_id = ?",
                (rs, rowNum) -> rs.getLong("user_id"),
                filmId
        ));
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null) {
            return;
        }
        for (Genre genre : film.getGenres()) {
            jdbcTemplate.update(
                    "MERGE INTO film_genres (film_id, genre_id) KEY (film_id, genre_id) VALUES (?, ?)",
                    film.getId(),
                    genre.getId()
            );
        }
    }

    private void validateMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == 0) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mpa WHERE mpa_id = ?",
                Integer.class,
                film.getMpa().getId()
        );
        if (count == null || count == 0) {
            throw new NotFoundException("Рейтинг MPA с id=" + film.getMpa().getId() + " не найден");
        }
    }

    private void validateGenres(Film film) {
        if (film.getGenres() == null) {
            return;
        }
        for (Genre genre : film.getGenres()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM genres WHERE genre_id = ?",
                    Integer.class,
                    genre.getId()
            );
            if (count == null || count == 0) {
                throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
            }
        }
    }
}
