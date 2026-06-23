package com.ssafy.modu.domain.user.repository;

import com.ssafy.modu.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUserNameAndIsDeletedFalse(String userName);

    boolean existsByNicknameAndIsDeletedFalse(String nickname);

    Optional<User> findByUserNameAndIsDeletedFalse(String userName);

    Optional<User> findByIdAndIsDeletedFalse(Long id);

    List<User> findAllByIdInAndIsDeletedFalse(Collection<Long> ids);
}