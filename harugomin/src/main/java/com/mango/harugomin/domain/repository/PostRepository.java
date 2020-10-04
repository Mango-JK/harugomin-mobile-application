package com.mango.harugomin.domain.repository;

import com.mango.harugomin.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>{

    Page<Post> findAll(Pageable pageable);

    Page<Post> findAllByTagName(String tagName, Pageable pageable);

    @Query(nativeQuery = true, value = " select * from post where content like %?1% OR title like %?1% ",
    countQuery = "SELECT COUNT(p.post_id) FROM post p WHERE p.title LIKE %?1% OR p.content LIKE %?1% ")
    Page<Post> searchAllPosts(String keyword, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(value = "update post set hits = hits + 1 where post_id = ?1 ", nativeQuery = true)
    void postHits(Long postId);

    Page<Post> findAllByUserUserId(Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    void deleteByUserUserId(Long userId);

}
