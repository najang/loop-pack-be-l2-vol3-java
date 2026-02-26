package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping("/api/v1/products/{productId}/likes")
    @Override
    public ApiResponse<LikeV1Dto.LikeResponse> like(
        @PathVariable Long productId,
        @LoginUser UserModel user
    ) {
        return ApiResponse.success(LikeV1Dto.LikeResponse.from(likeFacade.like(user.getId(), productId)));
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void unlike(
        @PathVariable Long productId,
        @LoginUser UserModel user
    ) {
        likeFacade.unlike(user.getId(), productId);
    }

    @GetMapping("/api/v1/users/me/likes")
    @Override
    public ApiResponse<LikeV1Dto.LikedProductPageResponse> getLikedProducts(
        @LoginUser UserModel user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(LikeV1Dto.LikedProductPageResponse.from(
            likeFacade.findLikedProducts(user.getId(), pageable)
        ));
    }
}
