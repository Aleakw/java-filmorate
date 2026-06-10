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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        fillCollections(films);
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
        fillCollections(List.of(film));
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

    private void fillCollections(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        Map<Long, Film> filmsById = films.stream()
                .collect(Collectors.toMap(Film::getId, film -> film, (first, second) -> first, LinkedHashMap::new));
        String placeholders = createPlaceholders(filmsById.size());
        Object[] filmIds = filmsById.keySet().toArray();

        jdbcTemplate.query(
                "SELECT fg.film_id, g.genre_id, g.name "
                        + "FROM film_genres fg "
                        + "JOIN genres g ON fg.genre_id = g.genre_id "
                        + "WHERE fg.film_id IN (" + placeholders + ") "
                        + "ORDER BY fg.film_id, g.genre_id",
                rs -> {
                    Film film = filmsById.get(rs.getLong("film_id"));
                    if (film != null) {
                        film.getGenres().add(new Genre(rs.getInt("genre_id"), rs.getString("name")));
                    }
                },
                filmIds
        );

        jdbcTemplate.query(
                "SELECT film_id, user_id FROM film_likes WHERE film_id IN (" + placeholders + ")",
                rs -> {
                    Film film = filmsById.get(rs.getLong("film_id"));
                    if (film != null) {
                        film.getLikes().add(rs.getLong("user_id"));
                    }
                },
                filmIds
        );
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        List<Genre> genres = new ArrayList<>(film.getGenres());
        jdbcTemplate.batchUpdate(
                "MERGE INTO film_genres (film_id, genre_id) KEY (film_id, genre_id) VALUES (?, ?)",
                genres,
                genres.size(),
                (ps, genre) -> {
                    ps.setLong(1, film.getId());
                    ps.setInt(2, genre.getId());
                }
        );
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
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        Set<Integer> genreIds = film.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String placeholders = createPlaceholders(genreIds.size());
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM genres WHERE genre_id IN (" + placeholders + ")",
                Integer.class,
                genreIds.toArray()
        );
        if (count == null || count != genreIds.size()) {
            throw new NotFoundException("Один или несколько жанров не найдены");
        }
    }

    private String createPlaceholders(int count) {
        return "?,".repeat(count).replaceAll(",$", "");
    }
}
