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

/**
 * 内部接口信息 Dubbo 服务实现。
 *
 * <p>每次按方法和上线状态查询，避免已下线接口因本地元数据缓存继续被调用。</p>
 */
@DubboService
public class InnerInterfaceInfoServiceImpl implements InnerInterfaceInfoService {
    /**
     * 用于匹配包含路径变量的已注册接口地址。
     */
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * 接口信息持久层。
     */
    @Resource
    private InterfaceInfoMapper mapper;

    /**
     * 根据方法及路径解析真实接口 ID，精确地址优先于路径模板。
     *
     * @param method HTTP 方法，查询时统一为大写
     * @param path 不包含查询串的原始网关路径
     * @return 匹配的已上线接口 ID，无匹配时返回 null
     */
    @Override
    public Long resolveOnlineInterfaceId(String method, String path) {
        if (method == null || path == null || !path.startsWith("/api/")) return null;
        List<InterfaceInfo> interfaces = mapper.selectList(new QueryWrapper<InterfaceInfo>()
                .eq("method", method.toUpperCase(Locale.ROOT)).eq("status", 1).orderByAsc("id"));
        // 先匹配精确地址，再匹配本地路径模板；外部 URL 不参与路由匹配。
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
