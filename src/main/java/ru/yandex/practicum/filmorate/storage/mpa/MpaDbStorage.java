package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Mpa> findAll() {
        return jdbcTemplate.query(
                "SELECT mpa_id, name FROM mpa ORDER BY mpa_id",
                (rs, rowNum) -> new Mpa(rs.getInt("mpa_id"), rs.getString("name"))
        );
    }

    @Override
    public Mpa findById(int id) {
        return jdbcTemplate.query(
                        "SELECT mpa_id, name FROM mpa WHERE mpa_id = ?",
                        (rs, rowNum) -> new Mpa(rs.getInt("mpa_id"), rs.getString("name")),
                        id
                ).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id=" + id + " не найден"));
    }
}
