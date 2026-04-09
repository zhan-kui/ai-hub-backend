package com.aihub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LangChain4j 智能体模块配置。
 * <p>
 * 该配置类用于集中管理智能体运行时参数，主要影响以下行为：
 * <p>1) 大模型服务地址与推理参数</p>
 * <p>2) SSE 打字机输出速度（前端看到的字符流动速度）</p>
 * <p>3) memory Agent 的历史窗口大小（会话上下文保留条数）</p>
 * <p>4) skill Agent 的兜底天气城市</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "langchain4j.agent")
public class LangChainAgentProperties {

    /**
     * OpenAI-compatible 模型服务地址。
     * <p>例如 vLLM: http://192.168.88.100:8000/v1</p>
     */
    private String baseUrl = "http://localhost:8000/v1";

    /**
     * 模型访问密钥。
     * <p>如果服务端未启用鉴权，可以留空。</p>
     */
    private String apiKey = "";

    /**
     * 模型名称。
     * <p>需要与服务端暴露的 served-model-name 保持一致。</p>
     */
    private String modelName = "qwen3.5-35b-a3b";

    /**
     * 采样温度。
     */
    private Double temperature = 0.7;

    /**
     * nucleus sampling 参数。
     */
    private Double topP = 0.9;

    /**
     * 单次回复最大 token 数。
     */
    private Integer maxTokens = 2048;

    /**
     * 模型请求超时时间（毫秒）。
     */
    private Integer requestTimeoutMs = 120000;

    /**
     * SSE 打字机输出延迟（毫秒）。
     * <p>值越小，输出越快；值越大，前端“逐字出现”的效果越明显。</p>
     */
    private Integer streamDelayMs = 25;

    /**
     * memory Agent 每个会话保留的消息窗口大小。
     * <p>达到上限后会自动淘汰更早的历史消息。</p>
     */
    private Integer memoryWindowSize = 20;

    /**
     * skill Agent 天气工具默认城市。
     * <p>当用户问题未明确城市时使用该值作为兜底参数。</p>
     */
    private String defaultCity = "北京";
}
