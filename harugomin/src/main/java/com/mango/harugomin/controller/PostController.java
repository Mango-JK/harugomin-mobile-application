package com.mango.harugomin.controller;

import com.google.gson.JsonObject;
import com.mango.harugomin.domain.entity.Comment;
import com.mango.harugomin.domain.entity.Hashtag;
import com.mango.harugomin.domain.entity.Post;
import com.mango.harugomin.dto.PostSaveRequestDto;
import com.mango.harugomin.dto.PostsHomeResponseDto;
import com.mango.harugomin.service.CommentService;
import com.mango.harugomin.service.HashtagService;
import com.mango.harugomin.service.PostService;
import com.mango.harugomin.service.S3Service;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*")
@Slf4j
@Api(tags = "2. Post")
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class PostController {

    private final PostService postService;
    private final HashtagService hashtagService;
    private final CommentService commentService;
    private final S3Service s3Service;

    /**
     * 1. 고민글 작성 or 수정
     */
    @ApiOperation("고민글 작성 or 수정")
    @PostMapping(value = "/posts")
    public ResponseEntity updatePost(@RequestBody PostSaveRequestDto requestDto) throws Exception {
        try {
            if (requestDto.getPostId() == -1) {
                postService.save(requestDto);
            } else
                postService.updatePost(requestDto);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * 2. 고민글 삭제 (History로 이동)
     */
    @ApiOperation("고민글 삭제")
    @DeleteMapping(value = "/posts/{postId}")
    public ResponseEntity deletePost(@PathVariable("postId") Long postId) throws Exception {
        try {
            postService.deletePost(postId);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * 3. 고민글 전체 조회
     */
    @ApiOperation("고민글 전체 조회")
    @GetMapping(value = "/posts")
    public ResponseEntity findAllPosts(@RequestParam("pageNum") int pageNum) throws Exception {
        PageRequest pageRequest = PageRequest.of(pageNum, 15, Sort.by("createdDate").descending());
        Page<Post> result = null;
        try {
            result = postService.findAllPosts(pageRequest);
        } catch (Exception e) {
            return new ResponseEntity(result, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 4. 고민글 상세 조회
     */
    @ApiOperation("고민글 상세 조회")
    @GetMapping(value = "/posts/{postId}")
    public ResponseEntity findOne(@PathVariable("postId") Long postId, @RequestParam int pageNum) {
        PageRequest pageRequest = PageRequest.of(pageNum, 15, Sort.by("createdDate").ascending());
        Page<Comment> result = null;
        result = commentService.pagingComment(postId, pageRequest);

        Post post = postService.findById(postId).get();
        if (post == null) {
            return new ResponseEntity(post, HttpStatus.NOT_FOUND);
        }

        postService.postHits(postId);
        post.getComments().clear();
        post.addCommentList(result.getContent());

        return new ResponseEntity(post, HttpStatus.OK);
    }

    /**
     * 5. 고민글 해시태그별 조회
     */
    @ApiOperation("고민글 해시태그별 조회")
    @GetMapping(value = "/posts/hashtag/{tagName}")
    public ResponseEntity findAllByHashtag(@PathVariable("tagName") String tagName, @RequestParam int pageNum) throws Exception {
        PageRequest pageRequest = PageRequest.of(pageNum, 15, Sort.by("createdDate").descending());
        Page<Post> result = null;
        try {
            result = postService.findAllByHashtag(tagName, pageRequest);
        } catch (Exception e) {
            return new ResponseEntity(result, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 6. 고민글 검색
     */
    @ApiOperation("고민글 통합 검색")
    @GetMapping(value = "/posts/search/{keyword}")
    public ResponseEntity searchAllPosts(@PathVariable("keyword") String keyword) throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 15, Sort.by("created_date").descending());
        Page<Post> result = null;
        try {
            result = postService.searchAllPosts(keyword, pageRequest);
        } catch (Exception e) {
            return new ResponseEntity(result, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(result, HttpStatus.OK);
    }

    /**
     * 7. Home Tab List 출력
     */
    @ApiOperation("hago Second Tab")
    @GetMapping(value = "/posts/home/{tagName}")
    public ResponseEntity homeView(@PathVariable("tagName") String tagName, @RequestParam int pageNum) throws Exception {
        PostsHomeResponseDto responseDtos = null;

        try {
            PageRequest tagRequest = PageRequest.of(0, 12, Sort.by("postingCount").descending());
            List<Hashtag> topTags = hashtagService.findAllTags(tagRequest).getContent();

            LocalDateTime currentTime = LocalDateTime.now();
            PageRequest storyRequest = PageRequest.of(0, 13, Sort.by("createdDate"));
            List<Post> data = postService.findAllPosts(storyRequest).getContent();
            List<Post> story = new ArrayList<>();
            for (Post post : data) {
                Duration duration = Duration.between(post.getCreatedDate(), currentTime);
                long minute = duration.getSeconds();

                if (duration.getSeconds() >= 86400) {
                    postService.postToHistory(post.getPostId());
                } else
                    story.add(post);
                if (story.size() > 9)
                    break;
            }

            PageRequest pageRequest = PageRequest.of(pageNum, 15, Sort.by("createdDate").descending());
            List<Post> postLists = postService.findAllByHashtag(tagName, pageRequest).getContent();
            responseDtos = new PostsHomeResponseDto(topTags, story, postLists);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(responseDtos, HttpStatus.OK);
    }

    /**
     * 8. 메인 고민글 3개 출력
     */
    @ApiOperation("Main 고민")
    @GetMapping(value = "/posts/main")
    public ResponseEntity mainView() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by("hits").descending());
        Page<Post> result = null;
        try {
            result = postService.findAllPosts(pageRequest);
        } catch (Exception e) {
            return new ResponseEntity(result.getContent(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result.getContent(), HttpStatus.OK);
    }

    /**
     * 9. 고민글 사진 업로드
     */
    @ApiOperation("고민글 사진 업로드")
    @PostMapping(value = "/posts/image")
    public String uploadPostImage(MultipartFile file) throws IOException {
        try {
            String imgPath = S3Service.CLOUD_FRONT_DOMAIN_NAME + s3Service.upload(null, file);
            JsonObject data = new JsonObject();
            data.addProperty("imgPath", imgPath);
            data.addProperty("status", String.valueOf(HttpStatus.OK));
            return data.toString();
        } catch (Exception e) {
            return HttpStatus.FORBIDDEN.toString();
        }
    }
}
