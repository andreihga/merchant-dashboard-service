package com.awin.merchantdashboard.auth.repository;

import com.awin.merchantdashboard.auth.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {}
