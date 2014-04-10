package com.dabeshackers.infor.gather;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration.Status;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.dabeshackers.infor.gather.application.AppMain;
import com.dabeshackers.infor.gather.entities.Attendee;
import com.dabeshackers.infor.gather.entities.Gathering;
import com.dabeshackers.infor.gather.entities.User;
import com.dabeshackers.infor.gather.helpers.GUIDHelper;
import com.dabeshackers.infor.gather.http.ApplicationWebService;
import com.kpbird.chipsedittextlibrary.ChipsAdapter;
import com.kpbird.chipsedittextlibrary.ChipsMultiAutoCompleteTextview;

public class AttendeesWriteActivity extends SherlockActivity {

	public static final int NEW_REQUEST_CODE = 10003;
	
	final SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.ENGLISH);


	private EditText fullName;
	private TextView gatheringTitle;
	private TextView gatheringSubtitle;
	private TextView gatheringDate;
//	private TextView gatheringStatus;
	private Spinner spinnerAttendanceStatus;

	ChipsMultiAutoCompleteTextview ch;
	ChipsAdapter chipsAdapter;

	private Attendee currentItem;
	private Gathering currentGathering;

	private long currentTime;

	private AppMain appMain;

	ProgressDialog pd;
	
	User currentUser;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_attendees_write);

		appMain = (AppMain) getApplication();

		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		currentTime = Calendar.getInstance().getTimeInMillis();

		if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey("gathering")) {
			currentGathering = (Gathering) getIntent().getExtras().getSerializable("gathering");
		}
		
		if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey("attendee")) {
			currentItem = (Attendee) getIntent().getExtras().getSerializable("attendee");
		}

		fullName = (EditText) findViewById(R.id.attendee_fullname);
		gatheringTitle = (TextView) findViewById(R.id.gathering_name);
		gatheringSubtitle = (TextView) findViewById(R.id.gathering_subtitle);
		gatheringDate = (TextView) findViewById(R.id.gathering_date);
//		gatheringStatus = (TextView) findViewById(R.id.gathering_status);

		currentUser = appMain.getCurrentUser();

		//populate fields if editing...
		if (currentUser != null && gatheringTitle!=null && currentGathering!=null) {
			populateFieldsForEditing();
		}

	}

	private void populateFieldsForEditing() {
		if (currentItem == null) {
			String fullNameString = currentUser.getFirstName() + " " + currentUser.getLastName();
			fullName.setText(fullNameString);
		} else {
			fullName.setText(currentItem.getFullname());
		}
	//	gatheringStatus.setText(currentGathering.getStatus());
		gatheringTitle.setText(currentGathering.getTitle());
		gatheringSubtitle.setText(currentGathering.getSubtitle());
		String date = "From " + df.format(currentGathering.getDatefrom())
				+ " to " + df.format(currentGathering.getDateto());
		gatheringDate.setText(date);
		
		
		/*spinner*/
		spinnerAttendanceStatus = (Spinner) findViewById(R.id.spinner_status);
		List<String> list = new ArrayList<String>();
		list.add(Attendee.STATUS_RESERVED);
		list.add(Attendee.STATUS_CONFIRMED);
		list.add(Attendee.STATUS_CANCELLED);
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
			android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAttendanceStatus.setAdapter(dataAdapter);

	}

	boolean hasValidationErrors = false;
	final StringBuilder errMsg = new StringBuilder();

	private boolean validate() {
		hasValidationErrors = false;
		errMsg.delete(0, errMsg.length());
		errMsg.append("Errors in the following fields were encountered. Please correct them then try again.\n\n");

		if (fullName.getText().toString() == null || fullName.getText().toString().length() == 0) {
			hasValidationErrors = true;
			errMsg.append("- Name should not be left blank.").append("\n");
		}

		if (gatheringTitle.getText().toString() == null || gatheringTitle.getText().toString().length() == 0) {
			hasValidationErrors = true;
			errMsg.append("- Error obtaining gathering title.").append("\n");
		}

		if (hasValidationErrors) {
			return false;
		}

		return true;
	}

	private void save() {
		pd = new ProgressDialog(AttendeesWriteActivity.this);
		pd.setIndeterminate(true);
		pd.setTitle("Saving to Cloud.");
		pd.setMessage("Please wait.");
		pd.setCancelable(false);
		pd.show();

		new Thread(new Runnable() {

			@Override
			public void run() {

				validate();
				if (hasValidationErrors) {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							pd.dismiss();
							AlertDialog.Builder alert = new AlertDialog.Builder(AttendeesWriteActivity.this);
							alert.setTitle("Unable to proceed.");
							alert.setMessage(errMsg);
							alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {

								}
							});
							alert.setIcon(android.R.drawable.ic_dialog_alert);
							alert.show();
						}

					});

				} else {

					if (currentItem == null) {
						currentItem = new Attendee();
						currentItem.setId(GUIDHelper.createGUID());
						currentItem.setGathering_id(currentGathering.getId());
						currentItem.setVersion(1);
						currentItem.setCreated(Calendar.getInstance().getTimeInMillis());
					} else {
						currentItem.setVersion(currentItem.getVersion() + 1);
					}

					currentItem.setFullname(fullName.getText().toString());
					currentItem.setStatus(spinnerAttendanceStatus.getSelectedItem().toString());
					//TODO status, time attended
				

					User currentUser = appMain.getCurrentUser();

					currentItem.setEdited_by(currentUser.getId());
					currentItem.setUpdated(Calendar.getInstance().getTimeInMillis());

					if (ApplicationWebService.Attendees.pushRecordToBackEnd(AttendeesWriteActivity.this, currentItem)) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								pd.dismiss();
								AlertDialog.Builder alert = new AlertDialog.Builder(AttendeesWriteActivity.this);
								alert.setTitle("Successful");
								alert.setMessage("Attendance record successfully saved!");
								alert.setCancelable(false);
								alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										setResult(RESULT_OK);
										finish();
									}
								});
								alert.setIcon(android.R.drawable.ic_dialog_alert);
								alert.show();
							}
						});

					} else {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								pd.dismiss();

								AlertDialog.Builder alert = new AlertDialog.Builder(AttendeesWriteActivity.this);
								alert.setTitle("Unable to save changes.");
								alert.setMessage("Unable to save to cloud at this time. Please try again.");
								alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {

									}
								});
								alert.setIcon(android.R.drawable.ic_dialog_alert);
								alert.show();
							}

						});
					}

				}
			}

		}).start();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Get the options menu view from menu.xml in menu folder
		getSupportMenuInflater().inflate(R.menu.attendees_write, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case android.R.id.home:
			this.finish();
			return true;

			// ***NOTE: Events for search is handled on onCreateOptionsMenu()

		case R.id.attendees_menu_proceed:
			save();

			break;

		}
		return true;
	}

	private boolean doubleBackToExitPressedOnce = false;

	@Override
	public void onBackPressed() {
		if (doubleBackToExitPressedOnce) {
			setResult(RESULT_CANCELED);
			AttendeesWriteActivity.super.onBackPressed();
			return;
		}
		this.doubleBackToExitPressedOnce = true;
		Toast.makeText(this, "Please click BACK again to cancel", Toast.LENGTH_SHORT).show();
		new Handler().postDelayed(new Runnable() {

			public void run() {
				doubleBackToExitPressedOnce = false;

			}
		}, 2000);

}
	
}
