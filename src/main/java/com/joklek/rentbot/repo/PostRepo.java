package com.joklek.rentbot.repo;

import com.joklek.rentbot.entities.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepo extends JpaRepository<Post, Long> {

    boolean existsByExternalIdAndSource(String externalId, String source);

    @EntityGraph(attributePaths = "postPriceHistory")
    Optional<Post> findByExternalIdAndSource(String externalId, String source);

    @Query(value = "SELECT distinct p FROM Post p " +
            "JOIN User u ON u.id = :userId " +
            "LEFT JOIN u.districts d " +
            "WHERE (" +
                "((p.price >= u.priceMin OR u.priceMin IS NULL) AND (p.price <= u.priceMax OR u.priceMax IS NULL)) " +
                "OR p.price IS NULL) " +
            "AND (" +
                "((p.rooms >= u.roomsMin OR u.roomsMin IS NULL) AND (p.rooms <= u.roomsMax OR u.roomsMax IS NULL)) " +
                "OR p.rooms IS NULL) " +
            "AND " +
                "(p.constructionYear >= u.yearMin " +
                "OR u.yearMin IS NULL " +
                "OR p.constructionYear IS NULL) " +
            "AND " +
                "(p.floor >= u.floorMin " +
                "OR u.floorMin IS NULL " +
                "OR p.floor IS NULL) " +
            "AND " +
                "(p.area >= u.areaMin " +
                "OR u.areaMin IS NULL " +
                "OR p.area IS NULL) " +
            "AND (u.filterByDistrict = false OR (" +
            "   d.name = p.district OR " +
            "   NOT EXISTS(SELECT 1 FROM District WHERE name = p.district)" +
            "))" +
            "AND p.createdAt >= :afterDate " +
            "ORDER BY p.id ASC")
    List<Post> getAllPostsForUserFromDays(Long userId, LocalDateTime afterDate);

    @Query(value = "SELECT COUNT(distinct p.id) FROM Post p " +
            "JOIN User u ON u.id = :userId " +
            "LEFT JOIN u.districts d " +
            "WHERE (" +
                "((p.price >= u.priceMin OR u.priceMin IS NULL) AND (p.price <= u.priceMax OR u.priceMax IS NULL)) " +
                "OR p.price IS NULL) " +
            "AND (" +
                "((p.rooms >= u.roomsMin OR u.roomsMin IS NULL) AND (p.rooms <= u.roomsMax OR u.roomsMax IS NULL)) " +
                "OR p.rooms IS NULL) " +
            "AND " +
                "(p.constructionYear >= u.yearMin " +
                "OR u.yearMin IS NULL " +
                "OR p.constructionYear IS NULL) " +
            "AND " +
                "(p.floor >= u.floorMin " +
                "OR u.floorMin IS NULL " +
                "OR p.floor IS NULL) " +
            "AND " +
                "(p.area >= u.areaMin " +
                "OR u.areaMin IS NULL " +
                "OR p.area IS NULL) " +
            "AND (u.filterByDistrict = false OR (" +
            "   d.name = p.district OR " +
            "   NOT EXISTS(SELECT 1 FROM District WHERE name = p.district)" +
            "))" +
            "AND p.createdAt >= :afterDate")
    int getCountOfPostsForUserFromDays(Long userId, LocalDateTime afterDate);
}
