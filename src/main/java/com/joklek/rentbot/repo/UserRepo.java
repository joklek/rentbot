package com.joklek.rentbot.repo;

import com.joklek.rentbot.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByTelegramId(Long id);

    User getByTelegramId(Long id);

    @Query(value = "SELECT telegramId FROM User " +
            "WHERE enabled = true " +
            "AND :price >= priceMin AND :price <= priceMax " +
            "AND :rooms >= roomsMin AND :rooms <= roomsMax " +
            "AND :year >= yearMin " +
            "AND floorMin <= :floor")
    List<User> findAllInterestedTelegramIds(BigDecimal price, Integer rooms, Integer year, Integer floor);
}
