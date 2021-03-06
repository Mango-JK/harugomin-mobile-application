package com.mango.harugomin.controller;

import com.google.gson.JsonObject;
import com.mango.harugomin.domain.entity.*;
import com.mango.harugomin.dto.PostResponseDto;
import com.mango.harugomin.dto.PostSaveRequestDto;
import com.mango.harugomin.service.HashtagService;
import com.mango.harugomin.service.HistoryService;
import com.mango.harugomin.service.PostService;
import com.mango.harugomin.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*")
@Slf4j
@Api(tags = "2. Post")
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final HashtagService hashtagService;
    private final HistoryService historyService;

    @ApiOperation("고민글 작성 or 수정")
    @PostMapping(value = "/posts")
    public ResponseEntity updatePost(@RequestBody PostSaveRequestDto requestDto) throws Exception {
        Post post = null;
        try {
            if (requestDto.getPostId() == -1) {
                post = postService.save(requestDto);
            } else
                post = postService.updatePost(requestDto);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(new PostResponseDto(post), HttpStatus.OK);
    }

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

    @ApiOperation("고민글 상세 조회")
    @GetMapping(value = "/posts/{postId}")
    public ResponseEntity findOne(@PathVariable("postId") Long postId) {
        Post post = postService.findById(postId).get();
        PostResponseDto result = new PostResponseDto(post);

        if (post == null) {
            History history = historyService.findById(postId).get();
            return new ResponseEntity(history, HttpStatus.NOT_FOUND);
        }

        LocalDateTime currentTime = LocalDateTime.now();
        Duration duration = Duration.between(post.getCreatedDate(), currentTime);
        if (duration.getSeconds() >= 86300) {
            postService.postToHistory(post.getPostId());
        }

        return new ResponseEntity(result, HttpStatus.OK);
    }

    @ApiOperation("(Home) - 인기순 해시태그 리스트")
    @GetMapping(value = "/posts/home/hashtag")
    public ResponseEntity homeBestHashtag() throws Exception {
        PageRequest tagRequest = PageRequest.of(0, 12, Sort.by("postingCount").descending());
        List<Hashtag> topTags = null;
        try {
            topTags = hashtagService.findAllTags(tagRequest).getContent();
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(topTags, HttpStatus.OK);
    }

    @ApiOperation("(Home) - 스토리")
    @GetMapping(value = "/posts/home/story")
    public ResponseEntity homeStory() throws Exception {
        List<Post> story = null;
        LocalDateTime currentTime = LocalDateTime.now();
        try {
            PageRequest storyRequest = PageRequest.of(0, 13, Sort.by("createdDate"));
            List<Post> data = postService.findAllPosts(storyRequest).getContent();
            story = new ArrayList<>();
            for (Post post : data) {
                Duration duration = Duration.between(post.getCreatedDate(), currentTime);
                if (duration.getSeconds() >= 86300) {
                    postService.postToHistory(post.getPostId());
                    continue;
                } else {
                    story.add(post);
                }
                if (story.size() > 9)
                    break;
            }
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(story, HttpStatus.OK);
    }

    @ApiOperation("(HOME) - 태그별 새 고민글")
    @GetMapping(value = "/posts/home/{tagName}")
    public ResponseEntity homePosting(@PathVariable("tagName") String tagName, @RequestParam int pageNum) throws
            Exception {
        Page<Post> result = null;
        PageRequest pageRequest = null;
        if (tagName.equals("전체")) {
            pageRequest = PageRequest.of(pageNum, 15, Sort.by("createdDate").descending());
            Page<Post> list = postService.findAllPosts(pageRequest);
            return new ResponseEntity(list.getContent(), HttpStatus.OK);
        }
        try {
            pageRequest = PageRequest.of(pageNum, 15, Sort.by("createdDate").descending());
            result = postService.findAllByHashtag(tagName, pageRequest);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(result.getContent(), HttpStatus.OK);
    }

    @ApiOperation("고민글 통합 검색")
    @GetMapping(value = "/posts/search/{keyword}")
    public ResponseEntity searchAllPosts(@PathVariable("keyword") String keyword, @RequestParam int pageNum) throws
            Exception {
        PageRequest pageRequest = PageRequest.of(pageNum, 15, Sort.by("created_date").descending());
        Page<Post> result = null;
        try {
            result = postService.searchAllPosts(keyword, pageRequest);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(result.getContent(), HttpStatus.OK);
    }

    @ApiOperation("메인 고민글 3개 출력")
    @GetMapping(value = "/posts/main")
    public ResponseEntity mainView(Long userId) throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 15, Sort.by("hits").descending());
        Page<Post> data = null;
        List<Post> result = new ArrayList<>();
        int i = 0;

        try {
            data = postService.findAllPosts(pageRequest);
            if (userId == -1) {
                while (result.size() < 3) {
                    result.add(data.getContent().get(i));
                    i++;
                }
                return new ResponseEntity<>(result, HttpStatus.OK);
            }

            User user = userService.findById(userId).get();
            String userHashString = "";
            List<UserHashtag> userHashtags = user.getUserHashtags();
            if (userHashtags.size() < 1) {
                while (result.size() < 3) {
                    result.add(data.getContent().get(i));
                    i++;
                }
                return new ResponseEntity<>(result, HttpStatus.OK);
            }

            for (UserHashtag userHashtag : userHashtags) {
                userHashString += userHashtag.getHashtag().getTagName();
            }
            for (Post post : data.getContent()) {
                if (result.size() > 2)
                    break;
                if (userHashString.contains(post.getTagName())) {
                    result.add(post);
                }
            }
            for (Post post : data.getContent()) {
                if (result.size() > 2)
                    break;
                if (!result.contains(post)) {
                    result.add(post);
                }
            }

        } catch (Exception e) {
            return new ResponseEntity(result, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ApiOperation("고민글 사진 업로드")
    @PostMapping(value = "/posts/image")
    public String uploadPostImage(@RequestParam MultipartFile files) throws IOException {
        try {
            JsonObject data = new JsonObject();
            String TARGET_DIR = "/home/ubuntu/hago/files/";
            String imagePath = FilenameUtils.getBaseName(files.getOriginalFilename());

            if(files.isEmpty()) {
                data.addProperty("imgPath", "");
                data.addProperty("status", String.valueOf(HttpStatus.OK));
                return data.toString();
            } else {
                String fileName = files.getOriginalFilename();
                String fileNameExtension = FilenameUtils.getExtension(fileName).toLowerCase();
                File targetFile;

                SimpleDateFormat timeFormat = new SimpleDateFormat("yyMMddHHmmss");
                imagePath += timeFormat.format(new Date()) + "." + fileNameExtension;
                targetFile = new File(TARGET_DIR+imagePath);
                log.info("Image uploaded : {}", targetFile);
                files.transferTo(targetFile);
            }

            data.addProperty("imgPath", imagePath);
            data.addProperty("status", String.valueOf(HttpStatus.OK));

            return data.toString();
        } catch (Exception e) {
            return HttpStatus.FORBIDDEN.toString();
        }
    }
}
