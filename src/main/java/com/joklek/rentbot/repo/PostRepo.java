package com.joklek.rentbot.repo;

import com.joklek.rentbot.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepo extends JpaRepository<Post, Long> {

    boolean existsByExternalIdAndSource(String externalId, String source);
}
