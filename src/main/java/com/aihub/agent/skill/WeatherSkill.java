package com.aihub.agent.skill;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 天气工具（Skill）示例。
 * <p>
 * 该工具用于演示 LangChain4j 的 @Tool 能力：
 * <p>1) Agent 识别到“天气意图”后触发工具调用</p>
 * <p>2) 工具返回结构化结果，再由 Agent 组合回复</p>
 * <p>3) Controller 端会向前端额外发送 tool_call / tool_result 事件</p>
 */
@Component
public class WeatherSkill {

    /**
     * 模拟天气数据。
     * <p>当前实现不依赖第三方天气 API，便于本地离线演示。</p>
     */
    private static final Map<String, String> WEATHER = Map.of(
            "北京", "晴，16-24℃，东北风3级",
            "上海", "多云，18-26℃，东南风2级",
            "深圳", "阵雨，24-30℃，南风3级",
            "杭州", "阴，17-25℃，东风2级",
            "广州", "雷阵雨，25-31℃，南风3级"
    );

    /**
     * 查询城市天气（模拟）。
     *
     * @param city 城市名称；为空时使用默认城市（北京）
     * @return 标准文本结果，供 Agent 直接拼接回复
     */
    @Tool("查询指定城市天气，返回模拟天气信息")
    public String queryWeather(@P("城市名称") String city) {
        String target = (city == null || city.isBlank()) ? "北京" : city.trim();
        String result = WEATHER.getOrDefault(target, "多云，20-28℃，微风");
        return target + "天气：" + result;
    }
}