package com.dabeshackers.infor.gather.entities;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Attendee extends Person {
	private static final long serialVersionUID = -7279060574909410623L;

	public static final String STATUS_RESERVED = "Reserved";
	public static final String STATUS_CONFIRMED = "Confirmed";
	public static final String STATUS_ATTENDED = "Attended";
	public static final String STATUS_CANCELLED = "Cancelled";

	private String gathering_id;

	private String email;
	private String fullname;
	private String status;
	private long timeattended;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getGathering_id() {
		return gathering_id;
	}

	public void setGathering_id(String gathering_id) {
		this.gathering_id = gathering_id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public long getTimeattended() {
		return timeattended;
	}

	public void setTimeattended(long timeattended) {
		this.timeattended = timeattended;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

}
