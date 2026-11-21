package com.aicompass.trend;

import com.aicompass.common.ApiResponse;
import com.aicompass.common.LoginUser;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 담당 C. 트렌드 랭킹은 로그인 없이 볼 수 있다 (로그인하면 isFavorited 표시)
@RestController
@RequestMapping("/api/v1/trends")
public class TrendController {

    private final TrendService trends;

    public TrendController(TrendService trends) {
        this.trends = trends;
    }

    /** ?period=realtime|weekly|monthly&category=&page=1&size=10 */
    @GetMapping("/rankings")
    public ApiResponse<Map<String, Object>> rankings(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + TrendService.SIZE_DEFAULT) int size,
            HttpSession session) {
        return ApiResponse.ok(trends.rankings(period, category, page, size, LoginUser.find(session)));
    }
}
