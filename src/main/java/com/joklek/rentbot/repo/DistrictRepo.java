package com.joklek.rentbot.repo;

import com.joklek.rentbot.entities.District;
import com.joklek.rentbot.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface DistrictRepo extends JpaRepository<District, Long> {

    boolean existsByName(String name);

    Set<District> findByUsers(User user);

    List<District> findAllByOrderByNameAsc();
}
