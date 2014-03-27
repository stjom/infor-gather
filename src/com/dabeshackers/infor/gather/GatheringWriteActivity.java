package com.dabeshackers.infor.gather;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;

import us.feras.ecogallery.EcoGallery;
import us.feras.ecogallery.EcoGalleryAdapterView;
import us.feras.ecogallery.EcoGalleryAdapterView.OnItemClickListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.dabeshackers.infor.gather.application.AppMain;
import com.dabeshackers.infor.gather.entities.Gathering;
import com.dabeshackers.infor.gather.entities.Media;
import com.dabeshackers.infor.gather.entities.User;
import com.dabeshackers.infor.gather.helpers.BitmapHelper;
import com.dabeshackers.infor.gather.helpers.FileOperationsHelper;
import com.dabeshackers.infor.gather.helpers.GUIDHelper;
import com.dabeshackers.infor.gather.helpers.LocationHelper;
import com.dabeshackers.infor.gather.helpers.MediaCaptureHelper;
import com.dabeshackers.infor.gather.http.ApplicationWebService;
import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.util.Joiner;
import com.kpbird.chipsedittextlibrary.ChipsAdapter;
import com.kpbird.chipsedittextlibrary.ChipsItem;
import com.kpbird.chipsedittextlibrary.ChipsMultiAutoCompleteTextview;

public class GatheringWriteActivity extends SherlockActivity {

	EcoGallery ecoGallery;
	GridView gridView;
	LinearLayout gridViewContainer;
	Double lng = null;
	Double lat = null;

	private LatLng currentLatLng;
	private String currentLocationText;

	private static final int LOCATION_PICKER_REQUEST_CODE = 10;
	public static final int NEW_OFFER_REQUEST_CODE = 10001;

	private enum DateTimePickerType {
		FROM, TO
	}

	private Uri fileUri;

	private EditText title, subtitle, eventmaster, price, description, refurl, yturl, fb_url, gplus_url, twtrurl;
	private Button from, to;
	private TextView organizer, locationtext;
	private CheckBox publishonsave;

	ChipsMultiAutoCompleteTextview ch;
	ChipsAdapter chipsAdapter;

	private Gathering currentGathering;
	private List<Media> images;

	private long currentFromTime;
	private long currentToTime;

	private AppMain appMain;

	ProgressDialog pd;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_gathering_write);

		appMain = (AppMain) getApplication();

		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		ecoGallery = (EcoGallery) findViewById(R.id.images);
		//		ecoGallery.setAdapter(new ImageAdapter(this));

		gridView = (GridView) findViewById(R.id.gridView);
		gridViewContainer = (LinearLayout) findViewById(R.id.gridViewContainer);
		gridViewContainer.setVisibility(View.GONE);

		currentFromTime = Calendar.getInstance().getTimeInMillis();
		currentToTime = Calendar.getInstance().getTimeInMillis();

		if (getIntent().getExtras().containsKey("lng")) {
			lng = getIntent().getExtras().getDouble("lng");
		}

		if (getIntent().getExtras().containsKey("lat")) {
			lat = getIntent().getExtras().getDouble("lat");
		}

		if (getIntent().getExtras().containsKey("loc_text")) {
			currentLocationText = getIntent().getExtras().getString("loc_text");
		}

		if (getIntent().getExtras().containsKey("item")) {
			currentGathering = (Gathering) getIntent().getExtras().getSerializable("item");
			currentFromTime = currentGathering.getDatefrom();
			currentToTime = currentGathering.getDateto();
		}

		currentLatLng = new LatLng(lat, lng);

		title = (EditText) findViewById(R.id.title);
		subtitle = (EditText) findViewById(R.id.subtitle);
		//		price = (EditText) findViewById(R.id.eventmaster);
		description = (EditText) findViewById(R.id.description);
		refurl = (EditText) findViewById(R.id.refurl);
		yturl = (EditText) findViewById(R.id.yturl);
		fb_url = (EditText) findViewById(R.id.fb_url);
		gplus_url = (EditText) findViewById(R.id.gplus_url);
		twtrurl = (EditText) findViewById(R.id.twtrurl);
		eventmaster = (EditText) findViewById(R.id.eventmaster);
		subtitle = (EditText) findViewById(R.id.mobile);
		organizer = (TextView) findViewById(R.id.organizer);
		locationtext = (TextView) findViewById(R.id.locationtext);
		publishonsave = (CheckBox) findViewById(R.id.publishonsave);

		final Button from = (Button) findViewById(R.id.from);

		from.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showDateTimePicker(DateTimePickerType.FROM);
			}
		});
		final Button to = (Button) findViewById(R.id.to);
		to.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showDateTimePicker(DateTimePickerType.TO);
			}
		});

		refurl.setOnFocusChangeListener(urlFocusListener);
		yturl.setOnFocusChangeListener(youtubeUrlFocusListener);
		fb_url.setOnFocusChangeListener(urlFocusListener);
		gplus_url.setOnFocusChangeListener(urlFocusListener);
		twtrurl.setOnFocusChangeListener(urlFocusListener);

		User currentUser = appMain.getCurrentUser();
		organizer.setText("Organized By: " + currentUser.getTradeName());
		refurl.setText(currentUser.getBusinessURL());
		fb_url.setText(currentUser.getFacebookURL());
		twtrurl.setText(currentUser.getTwitterURL());
		gplus_url.setText(currentUser.getGplusURL());

		//		eventmaster.setText(currentUser.getLandline());
		//		subtitle.setText(currentUser.getMobile());

		if (currentLocationText != null && !currentLocationText.equals(LocationHelper.LOCATION_UNAVAILABLE)) {
			locationtext.setText("near " + currentLocationText == null ? "Anywhere" : currentLocationText);
		} else {
			locationtext.setText("Location Unavailable");
		}

		//setup tags
		final ArrayList<ChipsItem> defaultCategories = new ArrayList<ChipsItem>();
		ChipsItem chip = new ChipsItem();
		chip.setTitle("");
		defaultCategories.add(chip);

		chipsAdapter = new ChipsAdapter(GatheringWriteActivity.this, defaultCategories);

		ch = (ChipsMultiAutoCompleteTextview) findViewById(R.id.tags);
		new Thread(new Runnable() {

			@Override
			public void run() {
				List<String> webTags = ApplicationWebService.Tags.fetchDistinctRecords(GatheringWriteActivity.this);
				final ArrayList<ChipsItem> presetCategories = new ArrayList<ChipsItem>();

				for (String tag : webTags) {
					ChipsItem chip = new ChipsItem();
					chip.setTitle(tag);
					presetCategories.add(chip);
				}

				//APPLY TO TAGS FIELD
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						chipsAdapter = new ChipsAdapter(GatheringWriteActivity.this, presetCategories);
						ch.setAdapter(chipsAdapter);
					}

				});

			}

		}).start();

		//populate fields if editing...
		if (currentGathering != null) {
			populateFieldsForEditing();
		}

	}

	private void populateFieldsForEditing() {

		DecimalFormat df = new DecimalFormat("###0.00");

		title.setText(currentGathering.getTitle());
		description.setText(currentGathering.getDescription());
		organizer.setText(currentGathering.getEventMaster());
		locationtext.setText(currentGathering.getLoc_text());

		refurl.setText(currentGathering.getRef_url());
		yturl.setText(currentGathering.getYoutube_url());
		fb_url.setText(currentGathering.getFacebook_url());
		gplus_url.setText(currentGathering.getGplus_url());
		twtrurl.setText(currentGathering.getTwitter_url());

		if (currentGathering.getTagsList() != null && currentGathering.getTagsList().size() > 0) {
			String tags = Joiner.on(',').join(currentGathering.getTagsList());
			ch.setText(tags);
			ch.setChips();
		}
		images = currentGathering.getImagesList();
		populatePhotos();

	}

	OnFocusChangeListener urlFocusListener = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			EditText text = (EditText) v;
			if (!hasFocus) {
				if ((!text.getText().toString().startsWith("http://") && (!text.getText().toString().startsWith("https://"))) && text.getText().toString().length() > 0) {
					text.setText("http://" + text.getText());
				}
			}
		}
	};

	OnFocusChangeListener youtubeUrlFocusListener = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			EditText text = (EditText) v;
			if (!hasFocus) {

				if (text.getText().toString().length() > 0) {
					//if user only puts video id
					if (!text.getText().toString().startsWith("https://www.youtube.com/watch?v=")) {
						text.setText("https://www.youtube.com/watch?v=" + text.getText());
					}

					//If user did not put anything
					if (text.getText().toString().trim().equals("https://www.youtube.com/watch?v=")) {
						text.setText("");
					}
				}

			} else {
				if (text.getText().toString().length() == 0) {
					text.setText("https://www.youtube.com/watch?v=" + text.getText());
				}
			}
		}
	};

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

		//		if (price.getText().toString() == null || price.getText().toString().length() == 0) {
		//			hasValidationErrors = true;
		//			errMsg.append("- Price should not be left blank.").append("\n");
		//		}

		if (description.getText().toString() == null || description.getText().toString().length() == 0) {
			hasValidationErrors = true;
			errMsg.append("- Description should not be left blank.").append("\n");
		}

		if (images == null || images.size() == 0) {
			hasValidationErrors = true;
			errMsg.append("- At least 1 (one) photo is required.").append("\n");
		}

		if (hasValidationErrors) {
			return false;
		}

		return true;
	}

	private void save() {
		pd = new ProgressDialog(GatheringWriteActivity.this);
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
							AlertDialog.Builder alert = new AlertDialog.Builder(GatheringWriteActivity.this);
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

					if (currentGathering == null) {
						currentGathering = new Gathering();
						currentGathering.setContext(GatheringWriteActivity.this);
						currentGathering.setId(GUIDHelper.createGUID());
						currentGathering.setStatus(Gathering.STATUS_PUBLISHED);
						currentGathering.setVersion(1);
						currentGathering.setCreated(Calendar.getInstance().getTimeInMillis());
					} else {
						currentGathering.setVersion(currentGathering.getVersion() + 1);
					}

					currentGathering.setTitle(title.getText().toString());
					currentGathering.setDescription(description.getText().toString());
					currentGathering.setRef_url(refurl.getText().toString());
					currentGathering.setYoutube_url(yturl.getText().toString());
					currentGathering.setFacebook_url(fb_url.getText().toString());
					currentGathering.setTwitter_url(twtrurl.getText().toString());
					currentGathering.setGplus_url(gplus_url.getText().toString());
					currentGathering.setLoc_text(locationtext.getText().toString());
					currentGathering.setLoc_lat(currentLatLng.latitude);
					currentGathering.setLoc_lng(currentLatLng.longitude);

					if (publishonsave.isChecked()) {
						currentGathering.setStatus(Gathering.STATUS_PUBLISHED);
					} else {
						currentGathering.setStatus(Gathering.STATUS_DRAFT);
					}

					String[] chips = ch.getText().toString().trim().split(",");
					List<String> tags = new ArrayList<String>(Arrays.asList(chips));

					if (images != null) {
						currentGathering.setImagesList(images);
					}

					if (tags != null) {
						currentGathering.setTagsList(tags);
					}

					User currentUser = appMain.getCurrentUser();

					currentGathering.setEventMaster(currentUser.getTradeName());
					currentGathering.setEdited_by(currentUser.getId());
					currentGathering.setUpdated(Calendar.getInstance().getTimeInMillis());

					if (ApplicationWebService.Gatherings.pushRecordToBackEnd(GatheringWriteActivity.this, currentGathering)) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								pd.dismiss();
								AlertDialog.Builder alert = new AlertDialog.Builder(GatheringWriteActivity.this);
								alert.setTitle("Successful");
								alert.setMessage("Offer successfully saved!");
								alert.setCancelable(false);
								alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										//Clear temp folder
										for (Media media : images) {
											File f = new File(media.getLocalFilePath());
											if (f.exists()) {
												if (!f.delete()) {
													f.deleteOnExit();
												}
											}
										}

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

								AlertDialog.Builder alert = new AlertDialog.Builder(GatheringWriteActivity.this);
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// if the result is capturing Image
		if (requestCode == MediaCaptureHelper.CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// successfully captured the image
				// display it in image view
				if (images == null) {
					images = new ArrayList<Media>();
				}

				File f = new File(fileUri.getPath());

				Media media = new Media(GatheringWriteActivity.this);
				media.setId(GUIDHelper.createGUID());
				media.setType(Media.MEDIA_TYPE_BITMAP);
				media.setLocalFilePath(fileUri.getPath());
				media.setName(f.getName());
				media.setStatus(Media.MEDIA_STATUS_NEW);

				User currentUser = appMain.getCurrentUser();

				media.setEdited_by(currentUser.getId());
				media.setCreated(Calendar.getInstance().getTimeInMillis());
				media.setUpdated(Calendar.getInstance().getTimeInMillis());
				media.setVersion(1);

				images.add(media);

				BitmapHelper.resize(fileUri.getPath(), BitmapHelper.SIZE_300);

				// Populate GridView
				populatePhotos();

			} else if (resultCode == RESULT_CANCELED) {
				// user) cancelled Image capture
				// Toast.makeText(getApplicationContext(),"User cancelled image capture",
				// Toast.LENGTH_SHORT).show();
			} else {
				// failed to capture image
				Toast.makeText(getApplicationContext(), "Sorry! Failed to capture image", Toast.LENGTH_SHORT).show();
			}
		} else if (requestCode == MediaCaptureHelper.CAMERA_CAPTURE_VIDEO_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// video successfully recorded
				// preview the recorded video
				// previewVideo();
			} else if (resultCode == RESULT_CANCELED) {
				// user cancelled recording
				Toast.makeText(getApplicationContext(), "User cancelled video recording", Toast.LENGTH_SHORT).show();
			} else {
				// failed to record video
				Toast.makeText(getApplicationContext(), "Sorry! Failed to record video", Toast.LENGTH_SHORT).show();
			}
		} else if (requestCode == MediaCaptureHelper.GALLERY_PICK_IMAGE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// successfully captured the image
				// display it in image view
				if (images == null) {
					images = new ArrayList<Media>();
				}

				File selectedImage = new File(FileOperationsHelper.getRealPathFromURI(GatheringWriteActivity.this, data.getData()));
				File f = new File(fileUri.getPath());

				try {
					FileUtils.copyFile(selectedImage, f);
				} catch (IOException e) {
					e.printStackTrace();
				}

				Media media = new Media(GatheringWriteActivity.this);
				media.setId(GUIDHelper.createGUID());
				media.setType(Media.MEDIA_TYPE_BITMAP);
				media.setLocalFilePath(fileUri.getPath());
				media.setName(f.getName());
				media.setStatus(Media.MEDIA_STATUS_NEW);

				User currentUser = appMain.getCurrentUser();

				media.setEdited_by(currentUser.getId());
				media.setCreated(Calendar.getInstance().getTimeInMillis());
				media.setUpdated(Calendar.getInstance().getTimeInMillis());
				media.setVersion(1);

				images.add(media);

				BitmapHelper.resize(fileUri.getPath(), BitmapHelper.SIZE_300);

				// Populate GridView
				populatePhotos();

			} else if (resultCode == RESULT_CANCELED) {
				// user) cancelled Image capture
				// Toast.makeText(getApplicationContext(),"User cancelled image capture",
				// Toast.LENGTH_SHORT).show();
			} else {
				// failed to capture image
				Toast.makeText(getApplicationContext(), "Sorry! Failed to pick image", Toast.LENGTH_SHORT).show();
			}
		} else if (requestCode == LOCATION_PICKER_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				double lat = (Double) data.getExtras().get(LocationPicker.EXTRAS_RESULT_LATITUDE);
				double lng = (Double) data.getExtras().get(LocationPicker.EXTRAS_RESULT_LONGITUDE);
				currentLatLng = new LatLng(lat, lng);

				Location loc = new Location("");
				loc.setLatitude(lat);
				loc.setLongitude(lng);
				currentLocationText = LocationHelper.getReadableAddressFromLocation(GatheringWriteActivity.this, loc, false);

				TextView locationtext = (TextView) findViewById(R.id.locationtext);
				locationtext.setText(currentLocationText == null ? "Anywhere" : currentLocationText);

			} else if (resultCode == RESULT_CANCELED) {
				// user cancelled
			} else {
				// failed
			}
		}
	}

	private void populatePhotos() {
		TextView imageslabel = (TextView) findViewById(R.id.imageslabel);
		imageslabel.setText("Photos (" + images.size() + ")");
		if (images.size() > 0) {

			ecoGallery.setAdapter(new ImageAdapter(GatheringWriteActivity.this));
			ecoGallery.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(EcoGalleryAdapterView<?> parent, View view, final int position, long id) {
					final File f = new File(images.get(position).getLocalFilePath());

					final PopupMenu popup = new PopupMenu(GatheringWriteActivity.this, view);
					popup.getMenuInflater().inflate(R.menu.image_options, popup.getMenu());
					popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(android.view.MenuItem menu) {
							switch (menu.getItemId()) {

							case R.id.menu_view:
								Intent intent = new Intent();
								intent.setAction(Intent.ACTION_VIEW);
								intent.setDataAndType(Uri.parse("file://" + f.getAbsolutePath()), "image/*");
								startActivity(intent);

								break;

							case R.id.menu_remove:
								images.remove(position);
								populatePhotos();

								break;

							default:
								break;
							}

							return true;
						}

					});

					popup.show();
				}
			});
			ecoGallery.setSelection(images.size() - 1);
			gridViewContainer.setVisibility(View.VISIBLE);

		} else {
			gridViewContainer.setVisibility(View.GONE);
		}

	}

	public class ImageAdapter extends BaseAdapter {
		Context context;

		public ImageAdapter(Context context) {
			this.context = context;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			ImageView imageView;
			imageView = new ImageView(GatheringWriteActivity.this);

			final String imageString = images.get(position).getLocalFilePath();
			Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromFile(imageString, 200, 200);
			imageView.setImageBitmap(bitmap);
			imageView.setBackgroundColor(Color.RED);
			//				imageView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, 200));
			imageView.setLayoutParams(new EcoGallery.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT));

			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

			return imageView;

		}

		@Override
		public int getCount() {
			return images.size();
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Get the options menu view from menu.xml in menu folder
		getSupportMenuInflater().inflate(R.menu.activity_offer_add, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case android.R.id.home:
			this.finish();
			return true;

			// ***NOTE: Events for search is handled on onCreateOptionsMenu()

		case R.id.menu_location:

			Intent intent = new Intent(GatheringWriteActivity.this, LocationPicker.class);
			intent.putExtra("lat", currentLatLng.latitude);
			intent.putExtra("lng", currentLatLng.longitude);
			startActivityForResult(intent, LOCATION_PICKER_REQUEST_CODE);

			break;

		case R.id.menu_camera:

			fileUri = MediaCaptureHelper.captureImage(GatheringWriteActivity.this, fileUri);

			break;

		case R.id.menu_gallery:

			fileUri = MediaCaptureHelper.pickPhoto(GatheringWriteActivity.this, fileUri);

			break;

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
			GatheringWriteActivity.super.onBackPressed();
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
		final SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.ENGLISH);
		final Dialog dialog = new Dialog(GatheringWriteActivity.this);

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

		//		tp.setOnTimeChangedListener(timeChangedListener);
		//

		//		dp.setOnDateChangedListener(dateChangedListener);
	}
}
