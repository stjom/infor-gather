package com.dabeshackers.infor.gather.entities;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Schedule implements Serializable, Cloneable {

	private static final long serialVersionUID = -4467711814241585735L;

	private String id;
	private String gathering_id;

	private String title;
	private String venue;
	private String host;
	private long timestart;
	private long timeend;
	private double duration;

	private String edited_by;
	private long created;
	private long updated;
	private int version;

	private Schedule snapShot;

	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public Schedule getSnapShot() {
		return snapShot;
	}

	public void createSnapShot() throws CloneNotSupportedException {
		this.snapShot = (Schedule) clone();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getGathering_id() {
		return gathering_id;
	}

	public void setGathering_id(String event_id) {
		this.gathering_id = event_id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getVenue() {
		return venue;
	}

	public void setVenue(String venue) {
		this.venue = venue;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public long getTimestart() {
		return timestart;
	}

	public void setTimestart(long timeStart) {
		this.timestart = timeStart;
	}

	public long getTimeend() {
		return timeend;
	}

	public void setTimeend(long timeEnd) {
		this.timeend = timeEnd;
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}

	public String getEdited_by() {
		return edited_by;
	}

	public void setEdited_by(String edited_by) {
		this.edited_by = edited_by;
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public long getUpdated() {
		return updated;
	}

	public void setUpdated(long updated) {
		this.updated = updated;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

}
