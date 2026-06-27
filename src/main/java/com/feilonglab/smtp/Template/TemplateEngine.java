package com.feilonglab.smtp.Template;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简易 HTML/文本 模板渲染引擎。
 * <p>
 * 支持将模板中的占位符（格式为 {@code ${variableName}}）替换为参数 Map 中对应的值。
 * 优化点包括：
 * 1. 使用 try-with-resources 自动释放模板文件的输入流资源，避免资源泄漏。
 * 2. 使用 Regex 正则匹配进行单遍（Single-pass）替换，避免多次调用 String.replace 重建 String 带来的性能损耗。
 * 3. 增强容错：防御性地处理 null 参数和 null 值，防止抛出 NullPointerException。当占位符无对应参数值时保留原样。
 * </p>
 *
 * @author feilonglab
 * @version 0.1.0
 */
public class TemplateEngine {

	// 编译占位符正则表达式，匹配 ${key} 格式，非贪婪匹配以防跨行或匹配多个占位符
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

	/**
	 * 读取指定路径的模板文件，并使用给定的参数映射进行渲染。
	 *
	 * @param templatePath 类路径下的模板文件路径 (以 / 开头)
	 * @param params       替换占位符的参数 Map
	 * @return 渲染后的字符串内容
	 * @throws FileNotFoundException 如果模板文件不存在
	 * @throws IOException           如果读取模板文件出错
	 */
	public static String render(String templatePath, Map<String, String> params) throws IOException {
		// 使用 try-with-resources 自动关闭流，避免资源泄漏
		try (InputStream in = TemplateEngine.class.getResourceAsStream(templatePath)) {
			if (in == null) {
				throw new FileNotFoundException("未找到模板文件：" + templatePath);
			}

			// 将输入流读取为 UTF-8 编码的模板字符串
			String template = new String(in.readAllBytes(), StandardCharsets.UTF_8);

			// 如果参数为空，直接返回原始模板，避免空指针
			if (params == null || params.isEmpty()) {
				return template;
			}

			Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
			StringBuilder sb = new StringBuilder();

			// 单遍正则扫描和匹配替换，效率极高
			while (matcher.find()) {
				String key = matcher.group(1);
				String replacement;
				if (params.containsKey(key)) {
					String val = params.get(key);
					// 防御性设计：如果值为 null 则替换为空白字符
					replacement = (val != null) ? val : "";
				} else {
					// 模板中存在的占位符在 Map 中没有对应 Key 时，保留原有占位符（如 ${username}）
					replacement = matcher.group(0);
				}
				// 对替换值中的 $ 和 \ 特殊符号进行转义保护，以防 Matcher 语法异常
				matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
			}
			matcher.appendTail(sb);

			return sb.toString();
		}
	}
}
