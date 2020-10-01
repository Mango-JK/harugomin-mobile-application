package com.mango.harugomin.controller;

import com.mango.harugomin.domain.entity.Comment;
import com.mango.harugomin.domain.entity.Liker;
import com.mango.harugomin.dto.CommentResponseDto;
import com.mango.harugomin.dto.CommentSaveRequestDto;
import com.mango.harugomin.dto.CommentUpdateRequestDto;
import com.mango.harugomin.service.CommentService;
import com.mango.harugomin.service.LikerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@Slf4j
@Api(tags = "3. Comment")
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class CommentController {

    private final CommentService commentService;
    private final LikerService likerService;

    /**
     * 1. 댓글 작성
     */
    @ApiOperation("댓글 작성")
    @PostMapping(value = "/comments")
    public ResponseEntity writePost(@RequestBody CommentSaveRequestDto requestDto) throws Exception {
        CommentResponseDto responseDto = null;
        try {
            responseDto = new CommentResponseDto(commentService.save(requestDto));
        } catch (Exception e) {
            return new ResponseEntity(responseDto, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(responseDto, HttpStatus.OK);
    }

    /**
     * 2. 댓글 수정
     */
    @ApiOperation("댓글 수정")
    @PutMapping(value = "/comments/{commentId}")
    public ResponseEntity updatePost(@PathVariable("commentId") Long commentId, @RequestBody CommentUpdateRequestDto requestDto) throws Exception {
        try {
            commentService.updateComment(commentId, requestDto);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * 3. 댓글 삭제
     */
    @ApiOperation("댓글 삭제")
    @DeleteMapping(value = "/comments/{commentId}")
    public ResponseEntity deletePost(@PathVariable("commentId") Long commentId) throws Exception {
        try {
            commentService.deleteComment(commentId);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * 4. 댓글 좋아요 or 취소
     */
    @ApiOperation("댓글 좋아요")
    @PutMapping(value = "/comments/like")
    public ResponseEntity likeComment(@RequestParam("commentId") Long commentId, @RequestParam("userId") Long userId) throws Exception {
        Comment comment = commentService.findById(commentId).get();
        try {
            if (likerService.findLiker(commentId, userId) > 0) {
                likerService.deteleLike(commentId, userId);
                commentService.likeUpdate(commentId, -1);
            } else {
                Liker liker = new Liker(userId, comment);
                likerService.save(liker);
                commentService.likeUpdate(commentId, 1);
            }
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

}
