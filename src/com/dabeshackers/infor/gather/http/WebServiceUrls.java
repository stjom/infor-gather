package com.dabeshackers.infor.gather.http;

public class WebServiceUrls {

	public static class User {
		public static final String PUSH_RECORD = "http://codesndbx.com/android/inforgather/user_insert_update.php";
		public static final String IS_EXISTS = "http://codesndbx.com/android/inforgather/user_isexists.php";
		public static final String IS_TRADENAME_UNIQUE = "http://codesndbx.com/android/inforgather/user_istradename_unique.php";
		public static final String SELECT_BY_ID = "http://codesndbx.com/android/inforgather/user_select_by_id.php";

		public static final String SELECT_BY_USERNAME = "http://codesndbx.com/android/chronicler/user_select_by_username.php";
	}

	public static class Gather {
		public static final String PUSH_RECORD = "http://codesndbx.com/android/inforgather/gathering_insert_update.php";
		public static final String UPDATE_STATUS = "http://codesndbx.com/android/inforgather/gathering_update_status.php";
		public static final String SELECT = "http://codesndbx.com/android/inforgather/gathering_select.php";
		public static final String SELECT_BY_USER_ID = "http://codesndbx.com/android/inforgather/gathering_select_by_user_id.php";
		public static final String SELECT_BY_USER_ID_DRAFTS = "http://codesndbx.com/android/inforgather/gathering_select_by_user_id_drafts.php";
	}

	public static class Attendee {
		public static final String PUSH_RECORD = "http://codesndbx.com/android/inforgather/attendee_write.php";
		public static final String UPDATE_STATUS = "http://codesndbx.com/android/inforgather/attendee_update_status.php";
		public static final String SELECT_BY_GATHERING_ID = "http://codesndbx.com/android/inforgather/attendee_select_by_gathering_id.php";
	}

	public static class Schedule {
		public static final String PUSH_RECORD = "http://codesndbx.com/android/inforgather/schedule_write.php";
		public static final String SELECT_BY_GATHERING_ID = "http://codesndbx.com/android/inforgather/schedule_select_by_gathering_id.php";
		public static final String DELETE_RECORD = "http://codesndbx.com/android/inforgather/schedule_delete.php";
	}

	public static class Media {
		public static final String PUSH_RECORD = "http://codesndbx.com/android/inforgather/media_insert_update.php";
		public static final String SELECT_BY_GATHERING_ID = "http://codesndbx.com/android/inforgather/media_select_by_gathering_id.php";
		public static final String IMAGES_URL = "http://codesndbx.com/android/inforgather/images/";
		public static final String CLEAR_BY_GATHERING_ID = "http://codesndbx.com/android/inforgather/media_clear_by_gathering_id.php";
	}

	public static class Tags {
		public static final String PUSH_RECORD = "http://codesndbx.com/android/inforgather/tags_insert_update.php";
		public static final String PUSH_RECORD_TO_INFOR_DISCUSS = "http://codesndbx.com/android/infordiscuss/tags_insert_update.php";
		public static final String SELECT_BY_GATHERING_ID = "http://codesndbx.com/android/inforgather/tags_select_by_gathering_id.php";
		public static final String SELECT_DISTINCT = "http://codesndbx.com/android/inforgather/tags_select_distinct.php";
		public static final String CLEAR_BY_GATHERING_ID = "http://codesndbx.com/android/inforgather/tags_clear_by_gathering_id.php";
	}

	public static class GCM {
		public static final String REGISTRAION = "http://codesndbx.com/android/chronicler/gcm_insert_update.php";
	}

	public static class Mailer {
		public static final String SEND_MAIL = "http://codesndbx.com/android/chronicler/mailer.php";
	}

}
