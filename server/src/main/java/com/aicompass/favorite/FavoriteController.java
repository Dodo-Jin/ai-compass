package com.aicompass.favorite;

import com.aicompass.common.ApiResponse;
import com.aicompass.common.LoginUser;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 담당 C. 즐겨찾기는 모두 로그인 필요. 오류는 ApiException → GlobalExceptionHandler
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favorites;

    public FavoriteController(FavoriteService favorites) {
        this.favorites = favorites;
    }

    public record AddRequest(Integer modelId) {}

    /** 새로 추가하면 201, 이미 즐겨찾기한 툴이면 200. 본문은 { modelId, isFavorited, favoriteCount } */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> add(
            @RequestBody(required = false) AddRequest req, HttpSession session) {
        int userId = LoginUser.require(session); // 본문 검사보다 로그인 확인을 먼저
        FavoriteService.AddResult result = favorites.add(userId, req == null ? null : req.modelId());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.ok(result.body()));
    }

    /** 204. 즐겨찾기에 없던 툴이어도 204 */
    @DeleteMapping("/{modelId}")
    public ResponseEntity<Void> remove(@PathVariable int modelId, HttpSession session) {
        favorites.remove(LoginUser.require(session), modelId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + FavoriteService.SIZE_DEFAULT) int size,
            HttpSession session) {
        return ApiResponse.ok(favorites.list(LoginUser.require(session), category, page, size));
    }

    /** ?modelIds=1,2,3 (2~4개) */
    @GetMapping("/compare")
    public ApiResponse<Map<String, Object>> compare(
            @RequestParam(required = false) String modelIds, HttpSession session) {
        return ApiResponse.ok(favorites.compare(LoginUser.require(session), modelIds));
    }
}
