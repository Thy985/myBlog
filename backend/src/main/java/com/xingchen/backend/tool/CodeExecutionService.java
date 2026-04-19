package com.xingchen.backend.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 代码执行服务
 * 支持 JavaScript、Python（基础）、Java（编译执行）
 */
@Service
@Slf4j
public class CodeExecutionService {

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 代码执行超时时间（毫秒）
    private static final int EXECUTION_TIMEOUT = 30000;
    // 最大输出长度
    private static final int MAX_OUTPUT_LENGTH = 10000;

    /**
     * 执行代码
     *
     * @param code 代码内容
     * @param language 编程语言（javascript, python, java）
     * @return 执行结果
     */
    public CodeExecutionResult execute(String code, String language) {
        if (code == null || code.trim().isEmpty()) {
            return CodeExecutionResult.builder()
                    .success(false)
                    .error("代码不能为空")
                    .build();
        }

        String lang = language != null ? language.toLowerCase() : "javascript";

        return switch (lang) {
            case "javascript", "js" -> executeJavaScript(code);
            case "python", "py" -> executePython(code);
            case "java" -> executeJava(code);
            default -> CodeExecutionResult.builder()
                    .success(false)
                    .error("不支持的语言: " + language)
                    .supportedLanguages(new String[]{"javascript", "python", "java"})
                    .build();
        };
    }

    /**
     * 执行 JavaScript 代码
     */
    private CodeExecutionResult executeJavaScript(String code) {
        long startTime = System.currentTimeMillis();

        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
            if (engine == null) {
                return CodeExecutionResult.builder()
                        .success(false)
                        .error("JavaScript 引擎不可用")
                        .build();
            }

            // 捕获输出
            StringBuilder output = new StringBuilder();
            engine.put("__output", output);

            // 添加辅助函数
            String wrappedCode = """
                var console = {
                    log: function() {
                        var args = Array.prototype.slice.call(arguments);
                        __output.append(args.join(' ')).append('\\n');
                    }
                };
                
                var print = function(msg) {
                    __output.append(msg).append('\\n');
                };
                
                """ + code;

            // 在超时控制下执行
            Future<Object> future = executor.submit(() -> {
                try {
                    return engine.eval(wrappedCode);
                } catch (ScriptException e) {
                    throw new RuntimeException(e);
                }
            });
            Object result = future.get(EXECUTION_TIMEOUT, TimeUnit.MILLISECONDS);

            long elapsed = System.currentTimeMillis() - startTime;

            String outputStr = output.toString();
            if (outputStr.length() > MAX_OUTPUT_LENGTH) {
                outputStr = outputStr.substring(0, MAX_OUTPUT_LENGTH) + "\n... (输出已截断)";
            }

            return CodeExecutionResult.builder()
                    .success(true)
                    .language("javascript")
                    .output(outputStr)
                    .result(result != null ? result.toString() : null)
                    .executionTime(elapsed)
                    .build();

        } catch (TimeoutException e) {
            return CodeExecutionResult.builder()
                    .success(false)
                    .language("javascript")
                    .error("执行超时（超过 " + EXECUTION_TIMEOUT / 1000 + " 秒）")
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            // 处理 ScriptException（被包装在 RuntimeException 中）
            Throwable cause = e.getCause();
            if (cause instanceof javax.script.ScriptException) {
                return CodeExecutionResult.builder()
                        .success(false)
                        .language("javascript")
                        .error("语法错误: " + cause.getMessage())
                        .executionTime(System.currentTimeMillis() - startTime)
                        .build();
            }
            return CodeExecutionResult.builder()
                    .success(false)
                    .language("javascript")
                    .error("执行错误: " + e.getMessage())
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 执行 Python 代码（使用 Python 解释器）
     */
    private CodeExecutionResult executePython(String code) {
        long startTime = System.currentTimeMillis();

        try {
            // 创建临时文件
            String tempId = UUID.randomUUID().toString();
            Path tempFile = Files.createTempFile("python_" + tempId, ".py");
            Files.writeString(tempFile, code, StandardCharsets.UTF_8);

            // 构建命令
            ProcessBuilder pb = new ProcessBuilder("python3", tempFile.toString());
            pb.redirectErrorStream(true);

            // 执行
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (output.length() > MAX_OUTPUT_LENGTH) {
                        output.append("... (输出已截断)");
                        break;
                    }
                }
            }

            // 等待完成（带超时）
            boolean finished = process.waitFor(EXECUTION_TIMEOUT, TimeUnit.MILLISECONDS);

            // 清理临时文件
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}

            long elapsed = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return CodeExecutionResult.builder()
                        .success(false)
                        .language("python")
                        .error("执行超时（超过 " + EXECUTION_TIMEOUT / 1000 + " 秒）")
                        .executionTime(elapsed)
                        .build();
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            if (exitCode == 0) {
                return CodeExecutionResult.builder()
                        .success(true)
                        .language("python")
                        .output(outputStr)
                        .executionTime(elapsed)
                        .build();
            } else {
                return CodeExecutionResult.builder()
                        .success(false)
                        .language("python")
                        .error("执行失败（退出码: " + exitCode + "）\n" + outputStr)
                        .executionTime(elapsed)
                        .build();
            }

        } catch (IOException e) {
            return CodeExecutionResult.builder()
                    .success(false)
                    .language("python")
                    .error("无法执行 Python（请确保已安装 Python3）: " + e.getMessage())
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CodeExecutionResult.builder()
                    .success(false)
                    .language("python")
                    .error("执行被中断")
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 执行 Java 代码（编译并运行）
     * 注意：仅支持简单的单文件程序
     */
    private CodeExecutionResult executeJava(String code) {
        long startTime = System.currentTimeMillis();

        try {
            // 提取类名
            String className = extractClassName(code);
            if (className == null) {
                // 如果没有类定义，包装一个
                className = "Main" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                code = """
                    public class %s {
                        public static void main(String[] args) {
                            %s
                        }
                    }
                    """.formatted(className, code);
            }

            // 创建临时目录
            Path tempDir = Files.createTempDirectory("java_exec_");
            Path sourceFile = tempDir.resolve(className + ".java");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            // 编译
            ProcessBuilder compilePb = new ProcessBuilder("javac", sourceFile.toString());
            compilePb.directory(tempDir.toFile());
            compilePb.redirectErrorStream(true);

            Process compileProcess = compilePb.start();
            StringBuilder compileOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(compileProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    compileOutput.append(line).append("\n");
                }
            }

            boolean compiled = compileProcess.waitFor(EXECUTION_TIMEOUT, TimeUnit.MILLISECONDS);

            if (!compiled || compileProcess.exitValue() != 0) {
                // 清理
                deleteDirectory(tempDir);
                return CodeExecutionResult.builder()
                        .success(false)
                        .language("java")
                        .error("编译失败:\n" + compileOutput)
                        .executionTime(System.currentTimeMillis() - startTime)
                        .build();
            }

            // 运行
            ProcessBuilder runPb = new ProcessBuilder("java", "-cp", tempDir.toString(), className);
            runPb.directory(tempDir.toFile());
            runPb.redirectErrorStream(true);

            Process runProcess = runPb.start();
            StringBuilder runOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(runProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    runOutput.append(line).append("\n");
                    if (runOutput.length() > MAX_OUTPUT_LENGTH) {
                        runOutput.append("... (输出已截断)");
                        break;
                    }
                }
            }

            boolean finished = runProcess.waitFor(EXECUTION_TIMEOUT, TimeUnit.MILLISECONDS);

            // 清理
            deleteDirectory(tempDir);

            long elapsed = System.currentTimeMillis() - startTime;

            if (!finished) {
                runProcess.destroyForcibly();
                return CodeExecutionResult.builder()
                        .success(false)
                        .language("java")
                        .error("执行超时（超过 " + EXECUTION_TIMEOUT / 1000 + " 秒）")
                        .executionTime(elapsed)
                        .build();
            }

            return CodeExecutionResult.builder()
                    .success(true)
                    .language("java")
                    .output(runOutput.toString())
                    .executionTime(elapsed)
                    .build();

        } catch (IOException e) {
            return CodeExecutionResult.builder()
                    .success(false)
                    .language("java")
                    .error("无法执行 Java（请确保已安装 JDK）: " + e.getMessage())
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CodeExecutionResult.builder()
                    .success(false)
                    .language("java")
                    .error("执行被中断")
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 从代码中提取类名
     */
    private String extractClassName(String code) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "public\\s+class\\s+(\\w+)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}
    }

    /**
     * 代码执行结果
     */
    @lombok.Data
    @lombok.Builder
    public static class CodeExecutionResult {
        private boolean success;
        private String language;
        private String output;
        private String result;
        private String error;
        private long executionTime;
        private String[] supportedLanguages;
    }
}
