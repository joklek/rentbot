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
            "AND (:price >= priceMin OR priceMin IS NULL) " +
            "AND (:price <= priceMax OR priceMax IS NULL) " +
            "AND (((:rooms >= roomsMin OR roomsMin IS NULL) AND (:rooms <= roomsMax OR roomsMax IS NULL)) OR :rooms IS NULL) " +
            "AND (:year >= yearMin OR :year IS NULL OR yearMin IS NULL) " +
            "AND (floorMin <= :floor OR :floor IS NULL OR floorMin IS NULL) ")
    List<Long> findAllTelegramIdsInterested(BigDecimal price, Integer rooms, Integer year, Integer floor);

    @Query(value = "SELECT telegramId FROM User " +
            "WHERE enabled = true " +
            "AND (:price >= priceMin OR priceMin IS NULL) " +
            "AND (:price <= priceMax OR priceMax IS NULL) " +
            "AND (((:rooms >= roomsMin OR roomsMin IS NULL) AND (:rooms <= roomsMax OR roomsMax IS NULL)) OR :rooms IS NULL) " +
            "AND (:year >= yearMin OR :year IS NULL OR yearMin IS NULL) " +
            "AND (floorMin <= :floor OR :floor IS NULL OR floorMin IS NULL) " +
            "AND showWithFees = true")
    List<Long> findAllTelegramIdsInterestedWithFee(BigDecimal price, Integer rooms, Integer year, Integer floor);

    @Query(value = "SELECT DISTINCT u.telegramId FROM User u " +
            "LEFT JOIN u.districts d " +
            "WHERE u.enabled = true " +
            "AND u.filterByDistrict = true " +
            "AND (:price >= priceMin OR priceMin IS NULL) " +
            "AND (:price <= priceMax OR priceMax IS NULL) " +
            "AND (((:rooms >= roomsMin OR roomsMin IS NULL) AND (:rooms <= roomsMax OR roomsMax IS NULL)) OR :rooms IS NULL) " +
            "AND (:year >= yearMin OR :year IS NULL OR yearMin IS NULL) " +
            "AND (floorMin <= :floor OR :floor IS NULL OR floorMin IS NULL) " +
            "AND lower(d.name) = lower(:district)")
    List<Long> findAllTelegramIdsInterestedInDistrict(BigDecimal price, Integer rooms, Integer year, Integer floor, String district);

    @Query(value = "SELECT DISTINCT u.telegramId FROM User u " +
            "LEFT JOIN u.districts d " +
            "WHERE u.enabled = true " +
            "AND u.filterByDistrict = true " +
            "AND (:price >= priceMin OR priceMin IS NULL) " +
            "AND (:price <= priceMax OR priceMax IS NULL) " +
            "AND (((:rooms >= roomsMin OR roomsMin IS NULL) AND (:rooms <= roomsMax OR roomsMax IS NULL)) OR :rooms IS NULL) " +
            "AND (:year >= yearMin OR :year IS NULL OR yearMin IS NULL) " +
            "AND (floorMin <= :floor OR :floor IS NULL OR floorMin IS NULL) " +
            "AND lower(d.name) = lower(:district) " +
            "AND u.showWithFees = true")
    List<Long> findAllTelegramIdsInterestedInDistrictWithFee(BigDecimal price, Integer rooms, Integer year, Integer floor, String district);

    @Query(value = "SELECT telegramId FROM User " +
            "WHERE enabled = true " +
            "AND filterByDistrict = false " +
            "AND (:price >= priceMin OR priceMin IS NULL) " +
            "AND (:price <= priceMax OR priceMax IS NULL) " +
            "AND (((:rooms >= roomsMin OR roomsMin IS NULL) AND (:rooms <= roomsMax OR roomsMax IS NULL)) OR :rooms IS NULL) " +
            "AND (:year >= yearMin OR :year IS NULL OR yearMin IS NULL) " +
            "AND (floorMin <= :floor OR :floor IS NULL OR floorMin IS NULL) ")
    List<Long> findAllTelegramIdsNotInterestedInDistricts(BigDecimal price, Integer rooms, Integer year, Integer floor);

    @Query(value = "SELECT telegramId FROM User " +
            "WHERE enabled = true " +
            "AND filterByDistrict = false " +
            "AND (:price >= priceMin OR priceMin IS NULL) " +
            "AND (:price <= priceMax OR priceMax IS NULL) " +
            "AND (((:rooms >= roomsMin OR roomsMin IS NULL) AND (:rooms <= roomsMax OR roomsMax IS NULL)) OR :rooms IS NULL) " +
            "AND (:year >= yearMin OR :year IS NULL OR yearMin IS NULL) " +
            "AND (floorMin <= :floor OR :floor IS NULL OR floorMin IS NULL) " +
            "AND showWithFees = true")
    List<Long> findAllTelegramIdsNotInterestedInDistrictsWithFee(BigDecimal price, Integer rooms, Integer year, Integer floor);
}
