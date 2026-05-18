package com.ssafy.modu.domain.user.repository;

import com.ssafy.modu.domain.user.entity.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDetailRepository extends JpaRepository<UserDetail, Long> {
}