package com.agx.aicodemother.core.saver;

import cn.hutool.core.util.StrUtil;
import com.agx.aicodemother.ai.model.HtmlCodeResult;
import com.agx.aicodemother.ai.model.MultiFileCodeResult;
import com.agx.aicodemother.exception.BusinessException;
import com.agx.aicodemother.exception.ErrorCode;
import com.agx.aicodemother.model.enums.CodeGenTypeEnum;

/**
 * 多文件代码保存器
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        // 保存 HTML 代码
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存 CSS 代码
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        // 保存 JS 代码
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 至少要有 HTML 代码, CSS 和 JS可以为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML 代码内容不能为空！");
        }
    }
}
