package com.ten.project.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ten.common.service.InnerInterfaceInfoService;
import com.ten.project.mapper.InterfaceInfoMapper;
import com.ten.project.model.entity.InterfaceInfo;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.AntPathMatcher;
import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;

@DubboService
public class InnerInterfaceInfoServiceImpl implements InnerInterfaceInfoService {
    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    @Resource
    private InterfaceInfoMapper mapper;

    @Override
    public Long resolveOnlineInterfaceId(String method, String path) {
        if (method == null || path == null || !path.startsWith("/api/")) return null;
        List<InterfaceInfo> interfaces = mapper.selectList(new QueryWrapper<InterfaceInfo>()
                .eq("method", method.toUpperCase(Locale.ROOT)).eq("status", 1).orderByAsc("id"));
        // Prefer exact registrations over path templates. Never match arbitrary external URLs.
        for (InterfaceInfo item : interfaces) {
            if (path.equals(item.getUrl())) return item.getId();
        }
        for (InterfaceInfo item : interfaces) {
            String pattern = item.getUrl();
            if (pattern != null && pattern.startsWith("/api/") && pattern.contains("{")
                    && MATCHER.match(pattern, path)) return item.getId();
        }
        return null;
    }
}
