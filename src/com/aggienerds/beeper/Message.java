package com.aggienerds.beeper;

import java.util.Date;

public class Message {
	private String address;
	private Integer id;
	private Integer thread_id;
	private Date date;
	private String subject;
	private String body;
	
	public Message()
	{
		setAddress(new String());
		setId(new Integer(-1));
		setThread_id(new Integer(-1));
		setSubject(new String());
		setBody(new String());
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getThread_id() {
		return thread_id;
	}

	public void setThread_id(Integer thread_id) {
		this.thread_id = thread_id;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	public void setDate(long milliseconds)
	{
		Date d = new Date();
		d.setTime(milliseconds);
		this.date = d;
	}
}
