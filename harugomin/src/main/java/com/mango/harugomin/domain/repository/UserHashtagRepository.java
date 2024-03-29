package com.mango.harugomin.domain.repository;

import com.mango.harugomin.domain.entity.UserHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface UserHashtagRepository extends JpaRepository<UserHashtag, Long> {

	@Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "delete from user_hashtag where user_id = ?1 ", nativeQuery = true)
    void deleteAllByUserId(Long userId);
}
