package com.sp.mail;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sp.common.FileManager;
import com.sp.common.MyUtil;

@Service("mail.myMailSender")
public class MailSender {
	@Autowired
	private FileManager fileManager;
	@Autowired
	private MyUtil myUtil;
	
	private String mailType; // 메일 타입
	private String encType;
	private String pathname;
	
	public MailSender() {
		this.encType = "utf-8";
		// this.encType = "euc-kr";
		this.mailType = "text/html; charset=utf-8";					//html 형식으로 저장
		// this.mailType = "text/html; charset=euc-kr";
		// this.mailType = "text/plain; charset=utf-8";
		// 임시로 저장하는 경로를 설정
		this.pathname = "c:"+File.separator+"temp"+File.separator+"mail";
	}

	public void setMailType(String mailType, String encType) {
		this.mailType = mailType;
		this.encType = encType;
	}
	
	public void setPathname(String pathname) {
		this.pathname = pathname;
	}

	// 네이버를 이용하여 메일을 보내는 경우 보내는사람의 이메일이 아래 계정(SMTP 권한 계정)과 다르면 메일 전송이 안된다. 
	// gmail은 기본적으로 <a href ...> 태그가 있으면 href를 제거한다.
	// SMTP 권한
	private class SMTPAuthenticator extends javax.mail.Authenticator {
		  @Override
	      public PasswordAuthentication getPasswordAuthentication() {  
         // 지메일은 경고메시지 전송 - 전송받은 메일에서 보안 수준을 낮추는 링크를 클릭하고 수준을 낮추면 메일 전송가능
         // gmail : 내계정 - 로그인 및 보안 => 아래부분 보안수준이 낮은 앱 사용  허용으로 변경
         // 네이버 : 메일 아래부분 환경설정 클릭후 POP3등을 허용
	          //String username =  "아이디@naver.com"; // 네이버 사용자는 주소 전체를 써야한다.;
	          String username =  "prolkh123"; // gmail 사용자는 아이디만 쓰면 된다.;  
	          String password = "dkffurhgkwlak3"; // 패스워드;  
	          return new PasswordAuthentication(username, password);  
	       }
	}
	
	// 첨부 파일이 있는 경우 MIME을 MultiMime로 파일을 전송 한다.
	private void makeMessage(Message msg, Mail dto) throws MessagingException {
		if(dto.getUpload()==null || dto.getUpload().isEmpty()) {
			// 파일을 첨부하지 않은 경우
			msg.setText(dto.getContent());
			msg.setHeader("Content-Type", mailType);
		} else {
			// 파일을 첨부하는 경우
			
			// 메일 내용
			MimeBodyPart mbp1 = new MimeBodyPart();
			mbp1.setText(dto.getContent());
			mbp1.setHeader("Content-Type", mailType);
			
			Multipart mp = new MimeMultipart();
			mp.addBodyPart(mbp1);
			
			// 첨부 파일
			for(MultipartFile mf:dto.getUpload()) {
				if(mf.isEmpty())
					continue;
				
				try {
					String saveFilename=fileManager.doFileUpload(mf, pathname);
					if(saveFilename!=null) {
						dto.getSavePathname().add(pathname+File.separator+saveFilename);
						
						String originalFilename=mf.getOriginalFilename();
						MimeBodyPart mbp2 = new MimeBodyPart();
						FileDataSource fds = new FileDataSource(pathname+File.separator+saveFilename);
						mbp2.setDataHandler(new DataHandler(fds));
						
						if(originalFilename == null || originalFilename.length()==0)
							mbp2.setFileName(MimeUtility.encodeWord(fds.getName()));
						else
							mbp2.setFileName(MimeUtility.encodeWord(originalFilename));
						mp.addBodyPart(mbp2);
					}
				} catch(UnsupportedEncodingException e) {
					System.out.println(e.toString());
				} catch (Exception e) {
					System.out.println(e.toString());
				}
			}
			
			msg.setContent(mp);
		}
	}
	
	public boolean mailSend(Mail dto) {
		boolean b=false;
		
		Properties p = new Properties();   
  
		// SMTP 서버의 계정 설정   
		// Naver와 연결할 경우 네이버 아이디
		// Gmail과 연결할 경우 Gmail 아이디
		p.put("mail.smtp.user", "prolkh123");   
  
		// SMTP 서버 정보 설정   
		// p.put("mail.smtp.host", "smtp.naver.com"); // 네이버   
		p.put("mail.smtp.host", "smtp.gmail.com"); // gmail
		       
		// 네이버와 지메일 동일   
		p.put("mail.smtp.port", "465");   
		p.put("mail.smtp.starttls.enable", "true");   
		p.put("mail.smtp.auth", "true");   
		// p.put("mail.smtp.debug", "true");   
		p.put("mail.smtp.socketFactory.port", "465");   
		p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");   
		p.put("mail.smtp.socketFactory.fallback", "false");  
		
		try {
			Authenticator auth = new SMTPAuthenticator();  
			Session session = Session.getDefaultInstance(p, auth);
			// 메일 전송시 상세 정보 콘솔에 출력 여부
			session.setDebug(true);
			
			Message msg = new MimeMessage(session);

			// 보내는 사람
			if(dto.getSenderName() == null || dto.getSenderName().equals(""))
				msg.setFrom(new InternetAddress(dto.getSenderEmail()));
			else
				msg.setFrom(new InternetAddress(dto.getSenderEmail(), dto.getSenderName(), encType));
			
			// 받는 사람
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(dto.getReceiverEmail()));
			
			// 제목
			msg.setSubject(dto.getSubject());
			
			// HTML 형식인 경우 \r\n을  <br>로 변환
			if(mailType.indexOf("text/html") != -1) {
				dto.setContent(myUtil.htmlSymbols(dto.getContent()));
			}
			makeMessage(msg, dto);
			msg.setHeader("X-Mailer", dto.getSenderName());
			
			// 메일 보낸 날짜
			msg.setSentDate(new Date());
			
			// 메일 전송
			Transport.send(msg);

			// 메일 전송후 서버에 저장된 첨부 파일 삭제
			if(dto.getSavePathname()!=null && dto.getSavePathname().size()>0) {
				for(String filename: dto.getSavePathname()) {
					File file = new File(filename);
					if(file.exists())
						file.delete();
				}
			}
			
			b=true;
						
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		
		return b;
	}
	

}
