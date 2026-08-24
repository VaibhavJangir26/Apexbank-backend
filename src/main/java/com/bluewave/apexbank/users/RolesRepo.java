package com.bluewave.apexbank.users;

import com.bluewave.apexbank.utils.common.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolesRepo extends JpaRepository<Roles,String> {

    Optional<Roles> findByAppRole(AppRole appRole);
}
