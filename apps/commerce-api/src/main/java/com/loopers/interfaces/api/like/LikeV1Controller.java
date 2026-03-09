package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.LoginUser;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Override
    public ApiResponse<LikeV1Dto.LikeResponse> unlike(
        @PathVariable Long productId,
        @LoginUser UserModel user
    ) {
        return ApiResponse.success(LikeV1Dto.LikeResponse.from(likeFacade.unlike(user.getId(), productId)));
    }

    @GetMapping("/api/v1/users/{userId}/likes")
    @Override
    public ApiResponse<LikeV1Dto.LikedProductPageResponse> getLikedProducts(
        @PathVariable Long userId,
        @LoginUser UserModel user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        if (!user.getId().equals(userId)) {
            throw new CoreException(ErrorType.FORBIDDEN, "다른 사용자의 좋아요 목록은 조회할 수 없습니다.");
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(LikeV1Dto.LikedProductPageResponse.from(
            likeFacade.findLikedProducts(user.getId(), pageable)
        ));
    }
}
