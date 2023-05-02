package com.smart.service;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;

@Service
public class EmailService {
	
	public boolean sendEmail(String subject,String message,String to)
	{
		//rest of the code
		
		boolean f = false;
		
		String from ="20ituos106@ddu.ac.in";
		
		//variable for gmail host
		String host = "smtp.gmail.com";
				
		//get the system properties
		Properties properties = System.getProperties();
				
		//setting important information through properties object
				
		//host set
		properties.put("mail.smtp.host",host);
		properties.put("mail.smtp.port","465");
		properties.put("mail.smtp.ssl.enable","true");
		properties.put("mail.smtp.auth","true");
		
		//Step 1: Get the session object.
		Session session = Session.getInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("20ituos106@ddu.ac.in", "het@1919");
			}
		} );
		
		session.setDebug(true);
		
		//Step 2: Compose the message [text,multimedia,..]
		MimeMessage mimeMessage = new MimeMessage(session);
	
		try {
			//from email id
			mimeMessage.setFrom(from);
			
			//adding recepient to message
			mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			
			//adding subject to message
			mimeMessage.setSubject(subject);
			
			//adding text to message
			//mimeMessage.setText(message);
			mimeMessage.setContent(message,"text/html");
			
			//send
			
			//Step 3: send the message using Transport class
			Transport.send(mimeMessage);
			System.out.println("Message Sent Successfully....");
			f = true;
			
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return f;
	}
	
}
