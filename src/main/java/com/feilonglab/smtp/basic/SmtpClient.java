package com.feilonglab.smtp.basic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feilonglab.smtp.Template.TemplateEngine;
import com.feilonglab.smtp.unified.MailRequest;

/**
 * SMTP 邮件发送客户端封装。
 * <p>
 * 提供连接的打开、邮件发送、自动关闭连接以及基于配置文件的参数加载等功能。
 * </p>
 *
 * @author feilonglab
 * @version 0.1.0
 */
public class SmtpClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(SmtpClient.class);

	private static final String CONFIG_FILE_PATH = "/mail.properties";

	/** 邮件服务器会话对象，用于存储连接配置 */
	private Session session;
	/** 邮件传输对象，用于管理底层的 TCP 连接和数据发送 */
	private Transport transport;
	/** SMTP 服务器的主机名或 IP 地址 */
	private String host;
	/** SMTP 服务器端口，默认 587 (常用于 STARTTLS) */
	private int port = 587;
	/** 认证用户名 */
	private String username;
	/** 认证密码或应用授权码 */
	private String password;
	/** 是否启用 STARTTLS 安全升级，默认 true */
	private boolean useTls = true;
	/** 是否直接启用 SSL 安全加密连接，默认 false */
	private boolean useSsl = false;

	/** 发件人名称（展示给接收方的别名） */
	private String senderName;
	/** 发件人邮箱地址 */
	private String senderEmail;

	/** 调试拦截标志: "0" 表示开启拦截（模拟连接与发送），"1" 表示关闭拦截（真实连接） */
	private String debugFlag = "1";

	/** 连接超时配置，单位：毫秒 (默认 5000) */
	private String connectionTimeoutMs = "5000";
	/** 读取/读取响应超时配置，单位：毫秒 (默认 5000) */
	private String timeoutMs = "5000";
	/** 写入超时配置，单位：毫秒 (默认 5000) */
	private String writeTimeoutMs = "5000";

	/** 调试模式标记，控制是否输出 javax.mail 协议层详细通信日志 */
	private boolean debug = false;

	/**
	 * 保护构造函数，用于特殊初始化（如 Builder 模式下控制是否加载默认配置文件）。
	 *
	 * @param loadConfig 是否加载默认配置文件 "/mail.properties"
	 */
	protected SmtpClient(boolean loadConfig) {
		if (loadConfig) {
			loadConfig();
		}
	}

	/**
	 * 默认构造函数。
	 * 自动尝试从类路径下的固定配置文件路径 "/mail.properties" 加载 SMTP 属性配置。
	 * 如果加载失败（例如文件不存在），将记录警告日志而不是抛出异常，以便允许后续手动设置或使用 Builder。
	 */
	public SmtpClient() {
		this(true);
	}

	/**
	 * 使用指定的配置文件路径构造 SMTP 客户端。
	 *
	 * @param configFilePath 类路径下的配置文件路径 (以 / 开头)
	 */
	public SmtpClient(String configFilePath) {
		this(false);
		loadConfig(configFilePath);
	}

	/**
	 * 直接使用指定连接参数构造 SMTP 客户端。
	 *
	 * @param host     SMTP 服务器主机名 (Host)
	 * @param port     SMTP 服务端口 (Port)
	 * @param username 邮箱账号用户名
	 * @param password 邮箱账号密码/授权码
	 * @param useTls   是否开启 STARTTLS
	 * @param useSsl   是否开启 SSL
	 */
	public SmtpClient(String host, int port, String username, String password, boolean useTls, boolean useSsl) {
		this(false);
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.useTls = useTls;
		this.useSsl = useSsl;
	}

	/**
	 * 从默认的固定配置文件 "/mail.properties" 中加载邮件配置属性。
	 */
	public void loadConfig() {
		loadConfig(CONFIG_FILE_PATH);
	}

	/**
	 * 从指定的类路径配置文件加载邮件配置属性。
	 *
	 * @param configFilePath 类路径下的配置文件路径 (以 / 开头)
	 */
	public void loadConfig(String configFilePath) {
		logger.info("正在尝试从类路径加载 SMTP 配置：{}", configFilePath);
		Properties props = new Properties();
		// 通过 ClassLoader 以输入流方式安全载入配置文件，并支持在 try-with-resources 中自动关闭流
		try (InputStream in = getClass().getResourceAsStream(configFilePath)) {
			if (in == null) {
				logger.warn("在类路径下未找到指定的配置文件：{}", configFilePath);
				return;
			}
			props.load(in);
			logger.info("SMTP 配置文件加载成功，路径为：{}", configFilePath);
		} catch (Exception e) {
			logger.warn("从路径加载 SMTP 配置文件失败：{}。错误信息：{}", configFilePath, e.getMessage());
			return;
		}

		// 读取连接所需的基本参数
		this.host = props.getProperty("mail.smtp.host");
		String portProp = props.getProperty("mail.smtp.port");
		this.port = Integer.parseInt(portProp != null ? portProp.trim() : "587");
		this.username = props.getProperty("mail.smtp.username");
		this.password = props.getProperty("mail.smtp.password");

		// 加载安全协议、超时及发件人配置
		this.useTls = Boolean.parseBoolean(props.getProperty("mail.smtp.starttls.enable", "true"));
		this.useSsl = Boolean.parseBoolean(props.getProperty("mail.smtp.ssl.enable", "false"));
		this.connectionTimeoutMs = props.getProperty("mail.smtp.connectiontimeout", "5000");
		this.timeoutMs = props.getProperty("mail.smtp.timeout", "5000");
		this.writeTimeoutMs = props.getProperty("mail.smtp.writetimeout", "5000");
		this.debug = Boolean.parseBoolean(props.getProperty("mail.debug", "false"));
		this.senderName = props.getProperty("mail.smtp.sender.name");
		this.senderEmail = props.getProperty("mail.smtp.sender.email");
		this.debugFlag = props.getProperty("mail.smtp.debug.flag", "1");
	}

	/**
	 * 建立与 SMTP 服务器的连接，并初始化 Session 与 Transport。
	 *
	 * @throws MessagingException    连接失败或握手异常时抛出
	 * @throws IllegalStateException 如果 SMTP 主机地址未配置
	 */
	public synchronized void open() throws MessagingException {
		// 拦截模式检查：不进行真实网络连接
		if ("0".equals(debugFlag)) {
			logger.info("【拦截模式】开启。模拟连接至 SMTP 服务器 {}:{}", host, port);
			// 即使在拦截模式下也初始化默认 Session，防止 MimeMessage 传入 null
			Properties sessionProps = new Properties();
			sessionProps.put("mail.smtp.host", host != null ? host : "localhost");
			sessionProps.put("mail.smtp.port", String.valueOf(port));
			this.session = Session.getInstance(sessionProps);
			return;
		}

		// 真实连接模式下，必须配置主机地址
		if (host == null || host.isEmpty()) {
			throw new IllegalStateException("SMTP Host must be configured before opening connection.");
		}

		// 避免重复连接
		if (this.transport != null && this.transport.isConnected()) {
			logger.info("SMTP 传输连接已建立，连接地址为 {}:{}", host, port);
			return;
		}

		logger.info("正在初始化 SMTP 会话并连接至 {}:{} (STARTTLS: {}, SSL: {})", host, port, useTls, useSsl);

		Properties sessionProps = new Properties();
		sessionProps.put("mail.smtp.host", host);
		sessionProps.put("mail.smtp.port", String.valueOf(port));

		// 只有在提供用户名时才启用 SMTP 认证
		boolean hasAuth = username != null && !username.isEmpty();
		sessionProps.put("mail.smtp.auth", String.valueOf(hasAuth));

		sessionProps.put("mail.smtp.starttls.enable", String.valueOf(useTls));
		sessionProps.put("mail.smtp.ssl.enable", String.valueOf(useSsl));

		// 设置连接超时、Socket 读取和写入超时，防止连接无限挂起
		sessionProps.put("mail.smtp.connectiontimeout", connectionTimeoutMs);
		sessionProps.put("mail.smtp.timeout", timeoutMs);
		sessionProps.put("mail.smtp.writetimeout", writeTimeoutMs);

		// 配置 SSL Socket 工厂参数以支持 SSL 连接
		if (useSsl) {
			sessionProps.put("mail.smtp.socketFactory.port", String.valueOf(port));
			sessionProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			sessionProps.put("mail.smtp.socketFactory.fallback", "false");
		}

		Authenticator authenticator = null;
		if (hasAuth) {
			authenticator = new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			};
		}

		this.session = Session.getInstance(sessionProps, authenticator);
		this.session.setDebug(this.debug);
		this.transport = session.getTransport("smtp");

		if (hasAuth) {
			this.transport.connect(host, port, username, password);
		} else {
			this.transport.connect();
		}

		logger.info("成功连接到 SMTP 服务器 {}:{}", host, port);
	}

	/**
	 * 发送邮件。
	 *
	 * @param recipientName  收件人姓名
	 * @param recipientEmail 收件人邮箱地址
	 * @param subject        邮件主题
	 * @param content        邮件正文内容（支持 HTML 格式）
	 * @throws MessagingException           构建邮件对象或发送邮件失败时抛出
	 * @throws UnsupportedEncodingException
	 * @throws IllegalStateException        如果客户端连接未建立
	 */
	public synchronized void sendMail(String recipientName, String recipientEmail, String subject, String content)
			throws MessagingException, UnsupportedEncodingException {

		// 状态校验：当为真实连接模式时，确保底层 transport 已经 open 并处于已连接状态
		if (!"0".equals(debugFlag) && (this.transport == null || !this.transport.isConnected())) {
			throw new IllegalStateException("SMTP 传输连接尚未建立，请先调用 open()。");
		}

		if (recipientEmail == null || recipientEmail.isEmpty()) {
			throw new IllegalArgumentException("收件人邮箱地址不能为空。");
		}

		// 拦截模式：仅在控制台输出待发送的邮件日志详情
		if ("0".equals(debugFlag)) {
			logger.info("【拦截模式】邮件发送详情：");
			logger.info("  - 发件人: {} <{}>", senderName, senderEmail);
			logger.info("  - 收件人: {} <{}>", recipientName, recipientEmail);
			logger.info("  - 主题: {}", subject);
			logger.info("  - 内容: {}", content);
		}

		// 基于当前 Session 构建符合 MIME 规范的标准邮件消息体
		MimeMessage message = new MimeMessage(this.session);

		// 设置发件人信息，使用指定的个人别名并使用 UTF-8 进行国际化编码
		message.setFrom(new InternetAddress(senderEmail, senderName, "UTF-8"));

		// 设置主收件人 (TO) 属性，同样支持接收人昵称的国际化编码
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail, recipientName, "UTF-8"));

		// 设置邮件标题主题，使用 UTF-8 编码防乱码
		message.setSubject(subject, "UTF-8");

		// 设置邮件正文内容为 text/html 格式，并且显式指明字符集为 UTF-8
		message.setContent(content, "text/html;charset=UTF-8");

		// 执行邮件的发送/模拟逻辑
		if ("0".equals(debugFlag)) {
			logger.info("【拦截模式】邮件模拟发送成功。【收件人：{} <{}>，主题：{}】", recipientName, recipientEmail, subject);
		} else {
			// 在已建立的 transport 通道上传输邮件消息
			this.transport.sendMessage(message, message.getAllRecipients());
			logger.info("邮件发送成功。【收件人：{} <{}>】", recipientName, recipientEmail);
		}
	}

	/**
	 * 关闭与 SMTP 服务器的连接，释放底层资源。
	 * 此方法实现了 {@link AutoCloseable#close()} 接口，以方便在 try-with-resources 中使用。
	 */
	@Override
	public synchronized void close() {
		// 拦截模式：仅重置 Session 并打印模拟日志
		if ("0".equals(debugFlag)) {
			logger.info("【拦截模式】关闭模拟连接");
			this.session = null;
			return;
		}

		// 真实连接模式下，安全关闭 Transport 网络通道并重置资源状态
		if (transport != null) {
			try {
				if (transport.isConnected()) {
					logger.info("正在关闭与 SMTP 服务器 {}:{} 的连接", host, port);
					this.transport.close();
				}
			} catch (MessagingException e) {
				logger.error("关闭与 {} 的 SMTP 传输连接时发生错误：{}", host, e.getMessage(), e);
			} finally {
				// 最终将 transport 和 session 变量置为 null，便于 JVM 垃圾回收
				this.transport = null;
				this.session = null;
			}
		}
	}

	/**
	 * 发送一个预先构建好的 MIME 邮件消息。
	 *
	 * @param message 预先构建好的 MimeMessage 实例
	 * @throws MessagingException    如果在发送邮件过程中发生异常
	 * @throws IllegalStateException 如果客户端连接未建立（在非拦截模式下）
	 */
	public synchronized void sendMimeMessage(MimeMessage message) throws MessagingException {
		if (message == null) {
			throw new IllegalArgumentException("邮件消息对象不能为 null。");
		}

		// 提取邮件元数据用于日志打印
		String subject = "";
		String recipientsStr = "";
		String fromStr = "";
		String contentStr = "";
		try {
			subject = message.getSubject();
			javax.mail.Address[] recipients = message.getAllRecipients();
			if (recipients != null) {
				java.util.List<String> rList = new java.util.ArrayList<>();
				for (javax.mail.Address r : recipients) {
					rList.add(r.toString());
				}
				recipientsStr = String.join(", ", rList);
			}
			javax.mail.Address[] from = message.getFrom();
			if (from != null && from.length > 0) {
				fromStr = from[0].toString();
			}
			Object contentObj = message.getContent();
			contentStr = contentObj != null ? contentObj.toString() : "";
		} catch (Exception e) {
			logger.debug("提取 MimeMessage 元数据失败: {}", e.getMessage());
		}

		// 拦截模式：仅打印详情并模拟发送
		if ("0".equals(debugFlag)) {
			logger.info("【拦截模式】邮件发送详情：");
			logger.info("  - 发件人: {}", fromStr);
			logger.info("  - 收件人: {}", recipientsStr);
			logger.info("  - 主题: {}", subject);
			logger.info("  - 内容: {}", contentStr);
			logger.info("【拦截模式】邮件模拟发送成功。【收件人：{}，主题：{}】", recipientsStr, subject);
			return;
		}

		// 状态校验：真实连接模式下，确保底层 transport 已经 open 并处于连接状态
		if (this.transport == null || !this.transport.isConnected()) {
			throw new IllegalStateException("SMTP 传输连接尚未建立，请先调用 open()。");
		}

		// 发送邮件数据包
		this.transport.sendMessage(message, message.getAllRecipients());
		logger.info("邮件发送成功。【收件人：{}】", recipientsStr);
	}

	/**
	 * 邮件服务器会话对象，用于存储连接配置
	 * 
	 * @return 邮件服务器会话对象，用于存储连接配置
	 */
	public Session getSession() {
		return session;
	}

	/**
	 * 发送邮件（支持 TO / CC / BCC 多收件人）。
	 *
	 * @param toList  收件人列表（必填，列表成员及列表本身均不能空）
	 * @param ccList  抄送收件人列表（可为 null 或空）
	 * @param bccList 密送收件人列表（可为 null 或空）
	 * @param subject 邮件主题
	 * @param content HTML 正文内容
	 * @throws MessagingException           如果在构建邮件或发送邮件过程中发生异常
	 * @throws UnsupportedEncodingException 如果编码不受支持
	 * @throws IllegalStateException        如果连接未建立且不在拦截模式下
	 */
	public synchronized void sendMail(
			List<String> toList,
			List<String> ccList,
			List<String> bccList,
			String subject,
			String content) throws MessagingException, UnsupportedEncodingException {

		// 参数及状态校验
		if (toList == null || toList.isEmpty()) {
			throw new IllegalArgumentException("收件人 (TO) 列表不能为空。");
		}

		if (!"0".equals(debugFlag) && (this.transport == null || !this.transport.isConnected())) {
			throw new IllegalStateException("SMTP 传输连接尚未建立，请先调用 open()。");
		}

		if (senderEmail == null || senderEmail.isEmpty()) {
			throw new IllegalStateException("发件人邮箱地址未配置，请检查 mail.properties 中的 mail.smtp.sender.email。");
		}

		// 拦截模式：输出详细模拟日志并提前返回
		if ("0".equals(debugFlag)) {
			logger.info("【拦截模式】模拟发送多收件人邮件详情：");
			logger.info("  - 发件人: {} <{}>", senderName, senderEmail);
			logger.info("  - 收件人 (TO): {}", toList);
			logger.info("  - 抄送 (CC): {}", ccList != null ? ccList : "[]");
			logger.info("  - 密送 (BCC): {}", bccList != null ? bccList : "[]");
			logger.info("  - 主题: {}", subject);
			logger.info("  - 内容: {}", content);
			logger.info("【拦截模式】多收件人邮件模拟发送成功。【TO: {}，主题：{}】", toList, subject);
			return;
		}

		MimeMessage message = new MimeMessage(this.session);

		// 设置发件人
		message.setFrom(new InternetAddress(senderEmail, senderName, "UTF-8"));

		// 设置收件人、抄送人和密送人
		setRecipients(message, Message.RecipientType.TO, toList);
		setRecipients(message, Message.RecipientType.CC, ccList);
		setRecipients(message, Message.RecipientType.BCC, bccList);

		// 设置邮件主题与内容
		message.setSubject(subject, "UTF-8");
		message.setContent(content, "text/html;charset=UTF-8");

		// 发送邮件数据包
		this.transport.sendMessage(message, message.getAllRecipients());
		
		logger.info("多收件人邮件发送成功。【TO: {} 个, CC: {} 个, BCC: {} 个，主题：{}】",
				toList.size(),
				ccList != null ? ccList.size() : 0,
				bccList != null ? bccList.size() : 0,
				subject);
	}

	/**
	 * 设置指定类型的收件人列表（TO / CC / BCC）。
	 *
	 * @param message MIME 邮件消息实例
	 * @param type    收件人类型 (TO / CC / BCC)
	 * @param emails  邮箱地址列表
	 * @throws MessagingException 在设置收件人时发生异常
	 */
	private void setRecipients(
			MimeMessage message,
			Message.RecipientType type,
			List<String> emails) throws MessagingException {

		if (emails == null || emails.isEmpty()) {
			return;
		}

		InternetAddress[] addresses = new InternetAddress[emails.size()];
		for (int i = 0; i < emails.size(); i++) {
			String email = emails.get(i);
			if (email == null || email.trim().isEmpty()) {
				throw new IllegalArgumentException("收件人邮箱地址不能为空。");
			}
			try {
				// 使用严格模式解析和校验单个邮箱地址
				InternetAddress[] parsed = InternetAddress.parse(email, true);
				if (parsed.length == 0) {
					throw new IllegalArgumentException("无效的邮箱地址: " + email);
				}
				addresses[i] = parsed[0];
			} catch (Exception e) {
				throw new IllegalArgumentException("邮箱地址解析失败: " + email, e);
			}
		}

		message.setRecipients(type, addresses);
	}

	/**
	 * 基于统一请求对象 {@link MailRequest} 发送邮件。
	 * 支持：
	 * 1. TO / CC / BCC 多群发。
	 * 2. 文本正文与 HTML 正文共存。
	 * 3. 动态 HTML 模板引擎渲染。
	 * 4. 多文件附件发送，且自动进行附件名中文编码以防止乱码。
	 *
	 * @param req 统一邮件请求对象，不能为 null
	 * @throws MessagingException 在邮件结构封装或发送过程中发生异常
	 * @throws IOException        在读取附件文件或模板文件时发生异常
	 */
	public synchronized void send(MailRequest req) throws MessagingException, IOException {
		if (req == null) {
			throw new IllegalArgumentException("MailRequest 请求参数不能为 null。");
		}

		if (req.to == null || req.to.isEmpty()) {
			throw new IllegalArgumentException("收件人 (TO) 列表不能为空。");
		}

		if (!"0".equals(debugFlag) && (this.transport == null || !this.transport.isConnected())) {
			throw new IllegalStateException("SMTP 传输连接尚未建立，请先调用 open()。");
		}

		if (senderEmail == null || senderEmail.isEmpty()) {
			throw new IllegalStateException("发件人邮箱地址未配置，请检查 mail.properties。");
		}

		// 1. 解析邮件正文内容 (若启用模板则渲染，否则使用 htmlContent)
		String html = req.htmlContent;
		if (req.useTemplate && req.templatePath != null) {
			html = TemplateEngine.render(req.templatePath, req.templateParams);
		}

		// 2. 拦截模式处理：打印详细的解析渲染元数据日志并提前返回
		if ("0".equals(debugFlag)) {
			logger.info("【拦截模式】模拟发送统一 API 邮件详情：");
			logger.info("  - 发件人: {} <{}>", senderName, senderEmail);
			logger.info("  - 收件人 (TO): {}", req.to);
			logger.info("  - 抄送 (CC): {}", req.cc != null ? req.cc : "[]");
			logger.info("  - 密送 (BCC): {}", req.bcc != null ? req.bcc : "[]");
			logger.info("  - 主题: {}", req.subject);
			logger.info("  - 纯文本内容: {}", req.textContent != null ? req.textContent : "(无)");
			logger.info("  - HTML内容: {}", html != null ? html : "(无)");
			if (req.attachments != null && !req.attachments.isEmpty()) {
				java.util.List<String> files = new java.util.ArrayList<>();
				for (File file : req.attachments) {
					if (file != null) {
						files.add(file.getName() + " (" + (file.exists() ? "存在" : "不存在!") + ")");
					}
				}
				logger.info("  - 附件列表: {}", files);
			} else {
				logger.info("  - 附件列表: (无)");
			}
			logger.info("【拦截模式】统一 API 邮件模拟发送成功。【TO: {}，主题：{}】", req.to, req.subject);
			return;
		}

		MimeMessage message = new MimeMessage(this.session);

		// 设置发件人
		message.setFrom(new InternetAddress(senderEmail, senderName, "UTF-8"));

		// 设置收件人列表
		setRecipients(message, Message.RecipientType.TO, req.to);
		setRecipients(message, Message.RecipientType.CC, req.cc);
		setRecipients(message, Message.RecipientType.BCC, req.bcc);

		// 设置主题
		message.setSubject(req.subject, "UTF-8");

		// --- 内容构建 ---
		boolean hasHtml = (html != null);
		boolean hasText = (req.textContent != null);
		boolean hasAttachments = (req.attachments != null && !req.attachments.isEmpty());

		if (hasAttachments) {
			// 外层容器：mixed (混合正文与附件)
			MimeMultipart mixedMultipart = new MimeMultipart("mixed");

			// 构建正文部分
			if (hasHtml && hasText) {
				// 正文内嵌容器：alternative (备用纯文本/HTML)
				MimeMultipart alternativeMultipart = new MimeMultipart("alternative");
				
				MimeBodyPart textPart = new MimeBodyPart();
				textPart.setText(req.textContent, "UTF-8");
				alternativeMultipart.addBodyPart(textPart); // 纯文本排在前面

				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, "text/html;charset=UTF-8");
				alternativeMultipart.addBodyPart(htmlPart); // HTML排在后面，优先显示

				MimeBodyPart bodyContentPart = new MimeBodyPart();
				bodyContentPart.setContent(alternativeMultipart);
				mixedMultipart.addBodyPart(bodyContentPart);
			} else if (hasHtml) {
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, "text/html;charset=UTF-8");
				mixedMultipart.addBodyPart(htmlPart);
			} else if (hasText) {
				MimeBodyPart textPart = new MimeBodyPart();
				textPart.setText(req.textContent, "UTF-8");
				mixedMultipart.addBodyPart(textPart);
			}

			// 添加附件部分
			for (File file : req.attachments) {
				if (file == null) {
					continue;
				}
				// 校验附件文件合法性
				if (!file.exists()) {
					throw new java.io.FileNotFoundException("附件文件不存在：" + file.getAbsolutePath());
				}
				if (file.isDirectory()) {
					throw new IllegalArgumentException("附件路径不能为目录：" + file.getAbsolutePath());
				}

				MimeBodyPart attachmentPart = new MimeBodyPart();
				attachmentPart.attachFile(file);
				// 解决中文附件名乱码
				attachmentPart.setFileName(javax.mail.internet.MimeUtility.encodeText(file.getName(), "UTF-8", null));
				mixedMultipart.addBodyPart(attachmentPart);
			}

			message.setContent(mixedMultipart);
		} else {
			// 无附件情况下的正文容器构建
			if (hasHtml && hasText) {
				MimeMultipart alternativeMultipart = new MimeMultipart("alternative");

				MimeBodyPart textPart = new MimeBodyPart();
				textPart.setText(req.textContent, "UTF-8");
				alternativeMultipart.addBodyPart(textPart);

				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, "text/html;charset=UTF-8");
				alternativeMultipart.addBodyPart(htmlPart);

				message.setContent(alternativeMultipart);
			} else if (hasHtml) {
				message.setContent(html, "text/html;charset=UTF-8");
			} else if (hasText) {
				message.setContent(req.textContent, "text/plain;charset=UTF-8");
			} else {
				message.setContent("", "text/plain;charset=UTF-8");
			}
		}

		// 真实发送邮件并记录日志
		this.transport.sendMessage(message, message.getAllRecipients());
		
		logger.info("统一 API 邮件发送成功。【TO: {} 个, CC: {} 个, BCC: {} 个，主题：{}】",
				req.to.size(),
				req.cc != null ? req.cc.size() : 0,
				req.bcc != null ? req.bcc.size() : 0,
				req.subject);
	}
}
