package com.aihub.agent.skill;

import com.aihub.repository.UserRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 数据库查询工具（Skill）示例。
 * <p>
 * 该工具演示 Agent 与真实业务数据源协作：
 * <p>1) 注入项目现有 Repository</p>
 * <p>2) 调用数据库统计当前有效用户数</p>
 * <p>3) 作为 tool_result 回传给前端展示</p>
 */
@Component
@RequiredArgsConstructor
public class UserCountSkill {

    /**
     * 用户仓储。
     */
    private final UserRepository userRepository;

    /**
     * 查询有效用户总数。
     *
     * @return 用户数量说明文本
     */
    @Tool("查询系统当前有效用户数量")
    public String queryUserCount() {
        long count = userRepository.countByDeletedFalse();
        return "当前系统有效用户数量为：" + count;
    }
}