package com.loopers.interfaces.api.queue;

import com.loopers.domain.queue.EntryTokenService;
import com.loopers.domain.queue.WaitingQueueService;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.LoginUser;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/queue")
public class QueueV1Controller implements QueueV1ApiSpec {

    /**
     * 시스템이 안정적으로 처리할 수 있는 TPS
     * - DB 커넥션 풀: 40 (local jpa.yml maximum-pool-size 실제값)
     * - 주문 평균 처리 시간: 154ms (k6 VU=1 실측 med)
     * - 이론적 최대 TPS = 40 / 0.154 ≈ 259
     * - 안전 마진 70% ≈ 181 TPS
     */
    private static final long THROUGHPUT_TPS = 181;

    private final WaitingQueueService waitingQueueService;
    private final EntryTokenService entryTokenService;

    @PostMapping("/enter")
    @ResponseStatus(HttpStatus.OK)
    @Override
    public ApiResponse<QueueV1Dto.QueueStatusResponse> enter(@LoginUser UserModel user) {
        long position = waitingQueueService.enter(user.getId());
        return ApiResponse.success(QueueV1Dto.QueueStatusResponse.waiting(
            position,
            estimatedWaitSeconds(position)
        ));
    }

    @GetMapping("/position")
    @Override
    public ApiResponse<QueueV1Dto.QueueStatusResponse> getPosition(@LoginUser UserModel user) {
        // 토큰이 발급된 경우 → 즉시 입장 가능
        Optional<String> token = entryTokenService.findToken(user.getId());
        if (token.isPresent()) {
            return ApiResponse.success(QueueV1Dto.QueueStatusResponse.ready(token.get()));
        }

        // 대기열에서 순번 조회
        Optional<Long> position = waitingQueueService.getPositionOptional(user.getId());
        if (position.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "대기열에 진입하지 않은 사용자입니다.");
        }

        long pos = position.get();
        return ApiResponse.success(QueueV1Dto.QueueStatusResponse.waiting(
            pos,
            estimatedWaitSeconds(pos)
        ));
    }

    private long estimatedWaitSeconds(long position) {
        return (position + THROUGHPUT_TPS - 1) / THROUGHPUT_TPS;
    }
}
