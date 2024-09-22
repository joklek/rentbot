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
    List<Long> findAllTelegramIdsInterested(BigDecimal price, Integer rooms, Integer year, Integer floor);

    @Query(value = "SELECT DISTINCT u.telegramId FROM User u " +
            "LEFT JOIN u.districts d " +
            "WHERE u.enabled = true " +
            "AND u.filterByDistrict = true " +
            "AND :price >= u.priceMin AND :price <= u.priceMax " +
            "AND :rooms >= u.roomsMin AND :rooms <= u.roomsMax " +
            "AND :year >= u.yearMin " +
            "AND u.floorMin <= :floor " +
            "AND lower(d.name) = lower(:district)")
    List<Long> findAllTelegramIdsInterestedInDistrict(BigDecimal price, Integer rooms, Integer year, Integer floor, String district);

    @Query(value = "SELECT telegramId FROM User " +
            "WHERE enabled = true " +
            "AND filterByDistrict = false " +
            "AND :price >= priceMin AND :price <= priceMax " +
            "AND :rooms >= roomsMin AND :rooms <= roomsMax " +
            "AND :year >= yearMin " +
            "AND floorMin <= :floor")
    List<Long> findAllTelegramIdsNotInterestedInDistricts(BigDecimal price, Integer rooms, Integer year, Integer floor);
}
