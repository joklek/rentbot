package com.joklek.rentbot.repo;

import com.joklek.rentbot.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepo extends JpaRepository<Post, Long> {

    boolean existsByExternalIdAndSource(String externalId, String source);

    @Query(value = "SELECT p FROM Post p " +
            "JOIN User u ON u.id = :userId " +
            "LEFT JOIN u.districts d " +
            "WHERE ((p.price >= u.priceMin AND p.price <= u.priceMax) OR p.price IS NULL) " +
            "AND ((p.rooms >= u.roomsMin AND p.rooms <= u.roomsMax) OR p.rooms IS NULL) " +
            "AND ((p.constructionYear >= u.yearMin) OR p.constructionYear IS NULL) " +
            "AND ((p.floor >= u.floorMin) OR p.floor IS NULL) " +
            "AND (u.showWithFees = true OR p.isWithFees = false) " +
            "AND (u.filterByDistrict = false OR (" +
            "   d.name = p.district OR " +
            "   NOT EXISTS(SELECT 1 FROM District WHERE name = p.district)" +
            "))" +
            "AND p.createdAt >= :afterDate " +
            "ORDER BY p.id ASC")
    List<Post> getAllPostsForUserFromDays(Long userId, LocalDateTime afterDate);

    @Query(value = "SELECT COUNT(p) FROM Post p " +
            "JOIN User u ON u.id = :userId " +
            "LEFT JOIN u.districts d " +
            "WHERE ((p.price >= u.priceMin AND p.price <= u.priceMax) OR p.price IS NULL) " +
            "AND ((p.rooms >= u.roomsMin AND p.rooms <= u.roomsMax) OR p.rooms IS NULL) " +
            "AND ((p.constructionYear >= u.yearMin) OR p.constructionYear IS NULL) " +
            "AND ((p.floor >= u.floorMin) OR p.floor IS NULL) " +
            "AND (u.showWithFees = true OR p.isWithFees = false) " +
            "AND (u.filterByDistrict = false OR (" +
            "   d.name = p.district OR " +
            "   NOT EXISTS(SELECT 1 FROM District WHERE name = p.district)" +
            "))" +
            "AND p.createdAt >= :afterDate")
    int getCountOfPostsForUserFromDays(Long userId, LocalDateTime afterDate);
}
