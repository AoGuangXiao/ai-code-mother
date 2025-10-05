package com.agx.aicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import com.agx.aicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件读取工具
 * 支持 AI 通过工具调用的方式读取指定文件内容
 */
@Component
@Slf4j
public class FileReadTool extends BaseTool {

    @Tool("读取指定路径的文件内容")
    public String readFile(@P("目标文件的相对路径") String relativeFilePath, @ToolMemoryId Long appId) {
        try{
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "错误：指定路径不是文件或文件不存在 - " + relativeFilePath;
            }
            return Files.readString(path);
        } catch(IOException e) {
            String errorMessage = "读取文件内容失败：" + relativeFilePath + "，错误：" + e.getMessage();
            log.error(errorMessage);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getDisplayName() {
        return "读取文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("[工具调用] %s %s", getDisplayName(), relativeFilePath);
    }
}
