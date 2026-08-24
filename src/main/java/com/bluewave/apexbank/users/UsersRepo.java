package com.bluewave.apexbank.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepo extends JpaRepository<Users,String> {
    Optional<Users> findByUsername(String username);
    Optional<Users> findByEmail(String email);
}
