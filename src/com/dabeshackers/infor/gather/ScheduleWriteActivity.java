package com.dabeshackers.infor.gather;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.dabeshackers.infor.gather.application.AppMain;
import com.dabeshackers.infor.gather.entities.Schedule;
import com.dabeshackers.infor.gather.entities.User;
import com.dabeshackers.infor.gather.helpers.GUIDHelper;
import com.dabeshackers.infor.gather.http.ApplicationWebService;
import com.kpbird.chipsedittextlibrary.ChipsAdapter;
import com.kpbird.chipsedittextlibrary.ChipsMultiAutoCompleteTextview;

public class ScheduleWriteActivity extends SherlockActivity {

	final SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.ENGLISH);

	public static final int NEW_REQUEST_CODE = 10002;

	private enum DateTimePickerType {
		FROM, TO
	}

	private EditText title, venue, host;
	private Button from, to;

	ChipsMultiAutoCompleteTextview ch;
	ChipsAdapter chipsAdapter;

	private Schedule currentItem;
	private String gathering_Id;

	private long currentFromTime;
	private long currentToTime;

	private AppMain appMain;

	ProgressDialog pd;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.schedule_write);

		appMain = (AppMain) getApplication();

		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		currentFromTime = Calendar.getInstance().getTimeInMillis();
		currentToTime = Calendar.getInstance().getTimeInMillis();

		if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey("item")) {
			currentItem = (Schedule) getIntent().getExtras().getSerializable("item");
			currentFromTime = currentItem.getTimestart();
			currentToTime = currentItem.getTimeend();
		}

		if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey("gathering_id")) {
			gathering_Id = getIntent().getExtras().getString("gathering_id");
		}

		title = (EditText) findViewById(R.id.title);
		venue = (EditText) findViewById(R.id.subtitle);
		host = (EditText) findViewById(R.id.host);

		from = (Button) findViewById(R.id.from);
		to = (Button) findViewById(R.id.to);

		//		duration = (TextView) findViewById(R.id.organizer);

		from.setText(df.format(currentFromTime));
		from.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showDateTimePicker(DateTimePickerType.FROM);
			}
		});
		to.setText(df.format(currentToTime));
		to.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showDateTimePicker(DateTimePickerType.TO);
			}
		});

		User currentUser = appMain.getCurrentUser();
		//		duration.setText("Organized By: " + currentUser.getTradeName());

		//populate fields if editing...
		if (currentItem != null) {
			populateFieldsForEditing();
		}

	}

	private void populateFieldsForEditing() {

		DecimalFormat df = new DecimalFormat("###0.00");

		title.setText(currentItem.getTitle());
		venue.setText(currentItem.getVenue());
		host.setText(currentItem.getHost());
		//		duration.setText(currentItem.getOrganizer());

	}

	boolean hasValidationErrors = false;
	final StringBuilder errMsg = new StringBuilder();

	private boolean validate() {
		hasValidationErrors = false;
		errMsg.delete(0, errMsg.length());
		errMsg.append("Errors in the following fields were encountered. Please correct them then try again.\n\n");

		if (title.getText().toString() == null || title.getText().toString().length() == 0) {
			hasValidationErrors = true;
			errMsg.append("- Title should not be left blank.").append("\n");
		}

		if (venue.getText().toString() == null || venue.getText().toString().length() == 0) {
			hasValidationErrors = true;
			errMsg.append("- Venue should not be left blank.").append("\n");
		}

		if (hasValidationErrors) {
			return false;
		}

		return true;
	}

	private void save() {
		pd = new ProgressDialog(ScheduleWriteActivity.this);
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
							AlertDialog.Builder alert = new AlertDialog.Builder(ScheduleWriteActivity.this);
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
						currentItem = new Schedule();
						currentItem.setId(GUIDHelper.createGUID());
						currentItem.setGathering_id(gathering_Id);
						currentItem.setVersion(1);
						currentItem.setCreated(Calendar.getInstance().getTimeInMillis());
					} else {
						currentItem.setVersion(currentItem.getVersion() + 1);
					}

					currentItem.setTitle(title.getText().toString());
					currentItem.setVenue(venue.getText().toString());
					currentItem.setHost(host.getText().toString());

					currentItem.setTimestart(currentFromTime);
					currentItem.setTimeend(currentToTime);

					User currentUser = appMain.getCurrentUser();

					currentItem.setEdited_by(currentUser.getId());
					currentItem.setUpdated(Calendar.getInstance().getTimeInMillis());

					if (ApplicationWebService.Schedules.pushRecordToBackEnd(ScheduleWriteActivity.this, currentItem)) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								pd.dismiss();
								AlertDialog.Builder alert = new AlertDialog.Builder(ScheduleWriteActivity.this);
								alert.setTitle("Successful");
								alert.setMessage("Schedule successfully saved!");
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

								AlertDialog.Builder alert = new AlertDialog.Builder(ScheduleWriteActivity.this);
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
		getSupportMenuInflater().inflate(R.menu.schedule_write, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case android.R.id.home:
			this.finish();
			return true;

			// ***NOTE: Events for search is handled on onCreateOptionsMenu()

		case R.id.menu_proceed:
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
			ScheduleWriteActivity.super.onBackPressed();
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

	private void showDateTimePicker(final DateTimePickerType type) {
		final Calendar selectedDateTime = Calendar.getInstance();
		final Calendar c = Calendar.getInstance();
		final Dialog dialog = new Dialog(ScheduleWriteActivity.this);

		dialog.setContentView(R.layout.datetime_picker);
		dialog.setTitle("Select date and time");

		final Button from = (Button) findViewById(R.id.from);
		final Button to = (Button) findViewById(R.id.to);

		final TimePicker tp = (TimePicker) dialog.findViewById(R.id.tm_p);
		final DatePicker dp = (DatePicker) dialog.findViewById(R.id.dt_p);

		switch (type) {
		case FROM:
			c.setTimeInMillis(currentFromTime);
			break;

		case TO:
			c.setTimeInMillis(currentToTime);
			break;

		}

		tp.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
		tp.setCurrentMinute(c.get(Calendar.MINUTE));
		dp.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

		Button positive = (Button) dialog.findViewById(R.id.positive);
		Button negative = (Button) dialog.findViewById(R.id.negative);

		positive.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				selectedDateTime.set(Calendar.HOUR_OF_DAY, tp.getCurrentHour());
				selectedDateTime.set(Calendar.MINUTE, tp.getCurrentMinute());
				selectedDateTime.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());

				switch (type) {
				case FROM:
					currentFromTime = selectedDateTime.getTimeInMillis();
					from.setText(df.format(currentFromTime));
					break;

				case TO:
					currentToTime = selectedDateTime.getTimeInMillis();
					to.setText(df.format(currentToTime));
					break;

				default:
					break;
				}

				dialog.dismiss();

			}
		});
		negative.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});

		dialog.show();

	}
}
