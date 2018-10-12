package email;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import com.sun.mail.util.MailSSLSocketFactory;

public class POP3ReceiveMailTest {
	public static void main(String[] args) throws Exception {
		// 定义连接POP3服务器的属性信息
		String host = "pop.qq.com";
		//定义收件人的邮箱帐号
		String user = "1455973223@qq.com";
		// QQ邮箱的SMTP的授权码
		String pwd = "pqtsbpnvjpnmbafc";
		String protocol = "pop3";
		Properties props = new Properties();
		// 使用的协议（JavaMail规范要求）
		props.setProperty("mail.store.protocal", protocol);
		// 发件人的邮箱的 SMTP服务器地址
		props.setProperty("mail.pop3.host", host);
		// QQ邮箱，设置SSL加密(必须要加密)
		MailSSLSocketFactory sf = new MailSSLSocketFactory();
		sf.setTrustAllHosts(true);
		props.put("mail.pop3.ssl.enable", "true");
		props.put("mail.pop3.ssl.socketFactory", sf);
		// 获取默认session对象
		Session session = Session.getDefaultInstance(props);
		session.setDebug(false);
		// 获取Store对象
		Store store = session.getStore(protocol);
		// POP3服务器的登陆认证
		store.connect(host, user, pwd);
		// 获得收件箱
		Folder folder = store.getFolder("INBOX");
		//设置邮件状态
		folder.open(Folder.READ_WRITE); // 打开收件箱

		// 获得收件箱中的邮件总数
		System.out.println("邮件总数: " + folder.getMessageCount());

		// 得到收件箱中的所有邮件,并解析
		Message[] messages = folder.getMessages();
		parseMessage(messages);
		// 释放资源
		folder.close(true);
		store.close();
	}
	/**
	 * 解析邮件
	 * @param messages 要解析的邮件列表
	 */
	public static void parseMessage(Message... messages) throws MessagingException, IOException {
		if (messages == null || messages.length < 1)
			throw new MessagingException("未找到要解析的邮件!");

		// 解析所有邮件
		for (int i = 0, count = messages.length; i < count; i++) {
			MimeMessage msg = (MimeMessage) messages[i];
			String from = MimeUtility.decodeText(messages[i].getFrom()[0].toString());
			InternetAddress ia = new InternetAddress(from);
			System.out.println("------------------解析第" + msg.getMessageNumber() + "封邮件-------------------- ");
			System.out.println("主题: " + msg.getSubject());
			System.out.println("发件人：" + ia.getPersonal() + '<' + ia.getAddress() + '>');
			System.out.println("发送时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(msg.getSentDate()));
			System.out.println("邮件大小：" + msg.getSize() * 1024 + "kb");
			//判断是否存在附件
			boolean isContainerAttachment = isContainAttachment(msg);
			if (isContainerAttachment) {
				// 保存附件，获取附件名
				System.out.println("附件：" + saveAttachment(msg, "E:\\mailtmp\\"));
			}
			StringBuffer content = new StringBuffer(30);
			getMailTextContent(msg, content);
			System.out.println("邮件正文：" + msg.getSubject()+(content.length() > 100 ? content.substring(0, 100) + "..." : content));
			System.out.println("------------------第" + msg.getMessageNumber() + "封邮件解析结束-------------------- ");
			System.out.println();
		}
	}
	/**
	 * 判断邮件中是否包含附件
	 * @param msg 邮件内容
	 * @return 邮件中存在附件返回true，不存在返回false
	 */
	public static boolean isContainAttachment(Part part) throws MessagingException, IOException {
		boolean flag = false;
		if (part.isMimeType("multipart/*")) {
			MimeMultipart multipart = (MimeMultipart) part.getContent();
			int partCount = multipart.getCount();
			for (int i = 0; i < partCount; i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				String disp = bodyPart.getDisposition();
				if (disp != null && (disp.equalsIgnoreCase(Part.ATTACHMENT) || disp.equalsIgnoreCase(Part.INLINE))) {
					flag = true;
				} else if (bodyPart.isMimeType("multipart/*")) {
					flag = isContainAttachment(bodyPart);
				} else {
					String contentType = bodyPart.getContentType();
					if (contentType.indexOf("application") != -1) {
						flag = true;
					}

					if (contentType.indexOf("name") != -1) {
						flag = true;
					}
				}

				if (flag)
					break;
			}
		} else if (part.isMimeType("message/rfc822")) {
			flag = isContainAttachment((Part) part.getContent());
		}
		return flag;
	}
	/**
	 * 获得邮件文本内容
	 * @param part 邮件体
	 * @param content 存储邮件文本内容的字符串
	 */
	public static void getMailTextContent(Part part, StringBuffer content) throws MessagingException, IOException {
		// 如果是文本类型的附件，通过getContent方法可以取到文本内容，但这不是我们需要的结果，所以在这里要做判断
		boolean isContainTextAttach = part.getContentType().indexOf("name") > 0;
		if (part.isMimeType("text/*") && !isContainTextAttach) {
			content.append(part.getContent().toString());
		} else if (part.isMimeType("message/rfc822")) {
			getMailTextContent((Part) part.getContent(), content);
		} else if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			int partCount = multipart.getCount();
			for (int i = 0; i < partCount; i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				getMailTextContent(bodyPart, content);
			}
		}
	}

	/**
	 * 保存附件
	 * @param part 邮件中多个组合体中的其中一个组合体
	 * @param destDir 附件保存目录
	 */
	public static String saveAttachment(Part part, String destDir)
			throws UnsupportedEncodingException, MessagingException, FileNotFoundException, IOException {
		BodyPart bodyPart=null;
		if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent(); // 复杂体邮件
			// 复杂体邮件包含多个邮件体
			int partCount = multipart.getCount();
			for (int i = 0; i < partCount; i++) {
				// 获得复杂体邮件中其中一个邮件体
				bodyPart = multipart.getBodyPart(i);
				// 某一个邮件体也有可能是由多个邮件体组成的复杂体
				String disp = bodyPart.getDisposition();
				if (disp != null && (disp.equalsIgnoreCase(Part.ATTACHMENT) || disp.equalsIgnoreCase(Part.INLINE))) {
					InputStream is = bodyPart.getInputStream();
					saveFile(is, destDir, decodeText(bodyPart.getFileName()));
				} else if (bodyPart.isMimeType("multipart/*")) {
					saveAttachment(bodyPart, destDir);
				} else {
					String contentType = bodyPart.getContentType();
					if (contentType.indexOf("name") != -1 || contentType.indexOf("application") != -1) {
						saveFile(bodyPart.getInputStream(), destDir, decodeText(bodyPart.getFileName()));
					}
				}
			}
		} else if (part.isMimeType("message/rfc822")) {
			saveAttachment((Part) part.getContent(), destDir);
		}
		return decodeText(bodyPart.getFileName());
	}

	/**
	 * 读取输入流中的数据保存至指定目录
	 * @param is 输入流
	 * @param fileName 文件名
	 * @param destDir 文件存储目录
	 */
	private static void saveFile(InputStream is, String destDir, String fileName)
			throws FileNotFoundException, IOException {
		BufferedInputStream bis = new BufferedInputStream(is);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(destDir + fileName)));
		int len = -1;
		while ((len = bis.read()) != -1) {
			bos.write(len);
			bos.flush();
		}
		bos.close();
		bis.close();
	}

	/**
	 * 文本解码
	 * @param encodeText 解码MimeUtility.encodeText(String text)方法编码后的文本
	 * @return 解码后的文本
	 */
	public static String decodeText(String encodeText) throws UnsupportedEncodingException {
		if (encodeText == null || "".equals(encodeText)) {
			return "";
		} else {
			return MimeUtility.decodeText(encodeText);
		}
	}
}
