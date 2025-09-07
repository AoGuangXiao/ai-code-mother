package com.agx.aicodemother.ai;

import com.agx.aicodemother.ai.tools.*;
import com.agx.aicodemother.exception.BusinessException;
import com.agx.aicodemother.exception.ErrorCode;
import com.agx.aicodemother.model.enums.CodeGenTypeEnum;
import com.agx.aicodemother.service.ChatHistoryOriginalService;
import com.agx.aicodemother.service.ChatHistoryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 使用工厂创建 AiCodeGeneratorService
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;
    @Resource
    private StreamingChatModel openAiStreamingChatModel;
    @Resource
    private StreamingChatModel reasoningStreamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ToolManager toolManager;
    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

    /**
     * AI 服务实例缓存
     * 缓存策略:
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务(带缓存)，这个方法是为了兼容历史逻辑
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 和代码生成类型获取服务(带缓存)
     * @param appId
     * @param codeGenType
     * @return
     */
    public AiCodeGeneratorService  getAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }


    /**
     * 创建新的 AI 服务实例
     * @param appId
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        AiCodeGeneratorService aiCodeGeneratorService;
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(60)   // 一次工具调用也算一次记忆，maxMessages得设置得大一点，不然模型会失忆一直循环调用工具
                .build();
        // 根据代码生成类型选择不同的模型配置
        switch (codeGenType) {
            case VUE_PROJECT -> {
                // 从数据库加载历史对话到缓存中，由于多了工具调用相关信息，加载的最大数量稍微多一些
                chatHistoryOriginalService.loadOriginalChatHistoryToMemory(appId, chatMemory, 50);
                // Vue 项目生成使用推理模型
                aiCodeGeneratorService = AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(toolManager.getAllTools())
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                            toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                    ))
                    .build();
            }
            case HTML, MULTI_FILE -> {
                // 从数据库加载历史对话到缓存中
                chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
                // HTML 和多文件生成模式使用默认模型
                aiCodeGeneratorService = AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
        return aiCodeGeneratorService;
    }

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return getAiCodeGeneratorService(0L);
    }

    /**
     * 构建缓存键
     * @param appId
     * @param codeGenType
     * @return
     */
    private String buildCacheKey(Long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

}
