package com.mango.harugomin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.mango.harugomin.domain.entity.User;
import com.mango.harugomin.dto.UserResponseDto;
import com.mango.harugomin.dto.UserUpdateRequestDto;
import com.mango.harugomin.jwt.JwtService;
import com.mango.harugomin.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@CrossOrigin(origins = "*")
@Slf4j
@Api(tags = "1. User")
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class UserController {

    private final UserService userService;
    private final KakaoAPIService kakaoAPIService;
    private final NaverAPIService naverAPIService;
    private final JwtService jwtService;
    private final S3Service s3Service;

    /**
     * 1. 카카오 로그인
     */
    @ApiOperation("카카오 로그인")
    @PostMapping("/users/login/kakao")
    public String kakaoLogin(@RequestParam String accessToken) {
        log.info("POST :: /user/login/kakao");

        JsonNode json = kakaoAPIService.getKaKaoUserInfo(accessToken);

        String result = null;
        try {
            result = kakaoAPIService.redirectToken(json); // 토큰 발행
        } catch (Exception e) {
            log.error(e + "");
        }

        return result;
    }

    /**
     * 2. 네이버 로그인
     */
    @ApiOperation("네이버 로그인")
    @PostMapping("/users/login/naver")
    public String naverLogin(@RequestParam String accessToken) {
        log.info("POST :: /user/login/naver");

        JsonNode json = naverAPIService.getNaverUserInfo(accessToken);

        String result = null;
        try {
            result = naverAPIService.redirectToken(json);
        } catch (Exception e) {
            log.error(e + "");
        }

        return result;
    }

    /**
     * 3. 토큰 검증
     */
    @ApiOperation("토큰 검증")
    @PostMapping("/users/check")
    public Object checkToken(@RequestParam String jwtToken) {
        log.info("UserController : checkToken");

        Object result = null;

        if (jwtService.isUsable(jwtToken)) {
            result = jwtService.get("user");
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 4. 프로필 사진 등록
     */
    @ApiOperation("유저 프로필 사진 업데이트")
    @PutMapping(value = "/users/profileImage/{id}")
    public String updateUserProfile(@PathVariable(value = "id") Long userId, MultipartFile file) throws IOException {
        User user = userService.findById(userId);
        String imgPath = S3Service.CLOUD_FRONT_DOMAIN_NAME + s3Service.upload(user.getProfileImage(), file);
        user.updateUserImage(imgPath);
        userService.saveUser(user);

        return imgPath;
    }

    /**
     * 5. 유저 프로필 업데이트
     */
    @ApiOperation("유저 프로필 업데이트 [사진, 닉네임, 연령대, 해시태그]")
    @PutMapping(value = "/users")
    public ResponseEntity<UserResponseDto> updateUserProfile(UserUpdateRequestDto requestDto) {
        userService.updateUser(requestDto);
        User user = userService.findById(requestDto.getUserId());
        return new ResponseEntity<>(new UserResponseDto(user), HttpStatus.OK);
    }

    /**
     * 6. 유저 해시태그 업데이트
     */
    @ApiOperation("유저 해시태그 업데이트")
    @PutMapping(value = "/users/hashtag/{id}")
    public ResponseEntity<UserResponseDto> updateUserHashtag(@PathVariable(value = "id") Long userId, String[] hashtags) {

        User user = userService.updateUserHashtag(userId, hashtags);

        return new ResponseEntity<>(new UserResponseDto(user), HttpStatus.OK);
    }

    /**
     * 7. 닉네임 중복검사
     */
    @ApiOperation("유저 닉네임 중복검사")
    @GetMapping(value = "/users/check/{nickname}")
    public ResponseEntity<Boolean> duplicationCheck(@PathVariable("nickname") String nickname) {
        boolean nicknameDuplicationCheckStatus = userService.duplicationCheck(nickname);

        return new ResponseEntity<>(nicknameDuplicationCheckStatus, HttpStatus.OK);
    }

//    /**
//     * 8. 유저 삭제
//     */
//    @ApiOperation("유저 삭제")
//    @GetMapping(value = "/users/{userId}")
//    public ResponseEntity<Long> deleteUser(@PathVariable("userId") Long userId) {
//        Long deleteUserId = userService.deleteUser(userId);
//
//        return new ResponseEntity<>(deleteUserId, HttpStatus.OK);
//    }






    @ApiOperation("(SERVER_TEST용)카카오 AccessToken 발급받기")
    @GetMapping(value = "/users/login/kakao")
    public String getKakaoCode(@RequestParam("code") String code) {
        log.info("User Kakao Code : " + code);

        ResponseEntity<String> AccessToken = kakaoAPIService.getAccessToken(code);

        log.info("My AccessToken : " + AccessToken);
        return "index";
    }


    @ApiOperation("(SERVER_TEST용)네이버 AccessToken 발급받기")
    @GetMapping(value = "/users/login/naver")
    public String getNaverCode(@RequestParam(value = "code") String code,
                               @RequestParam(value = "state") String state) {
        log.info("User Naver Code : " + code);
        log.info("State Code : " + state);

        ResponseEntity<String> AccessToken = naverAPIService.getAccessToken(code, state);

        log.info("Naver AccessToken : " + AccessToken);
        return "index";
    }

}