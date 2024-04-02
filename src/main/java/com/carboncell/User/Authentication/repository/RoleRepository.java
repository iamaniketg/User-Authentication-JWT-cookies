package com.carboncell.User.Authentication.repository;
import java.util.Optional;

import com.carboncell.User.Authentication.model.Role;
import com.carboncell.User.Authentication.model.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}