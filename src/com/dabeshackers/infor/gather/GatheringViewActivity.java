package com.dabeshackers.infor.gather;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.feras.ecogallery.EcoGallery;
import us.feras.ecogallery.EcoGalleryAdapterView;
import us.feras.ecogallery.EcoGalleryAdapterView.OnItemClickListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.dabeshackers.infor.gather.application.AppMain;
import com.dabeshackers.infor.gather.application.ApplicationUtils;
import com.dabeshackers.infor.gather.entities.Attendee;
import com.dabeshackers.infor.gather.entities.Gathering;
import com.dabeshackers.infor.gather.entities.Media;
import com.dabeshackers.infor.gather.entities.Schedule;
import com.dabeshackers.infor.gather.entities.User;
import com.dabeshackers.infor.gather.helpers.BitmapHelper;
import com.dabeshackers.infor.gather.helpers.DownloadManagerHelper;
import com.dabeshackers.infor.gather.helpers.GUIDHelper;
import com.dabeshackers.infor.gather.helpers.LocationHelper;
import com.dabeshackers.infor.gather.helpers.MediaCaptureHelper;
import com.dabeshackers.infor.gather.helpers.ToastHelper;
import com.dabeshackers.infor.gather.helpers.ZXingHelper;
import com.dabeshackers.infor.gather.http.ApplicationWebService;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

public class GatheringViewActivity extends YouTubeFailureRecoveryActivity implements OnTabChangeListener, OnPageChangeListener {
	private static final String TAG = GatheringViewActivity.class.getSimpleName();
	TextView title;
	TextView subtitle;
	TextView description;
	TextView expiration;
	TextView organizer;
	TextView eventmaster;

	TextView ref_url;
	TextView fb_url;
	TextView gplus_url;
	TextView twtr_url;

	LinearLayout header;

	Button programme, reserve, attachment;

	ListView scheduleView, attendeesView, attachmentView;
	List<Schedule> schedules;
	ScheduleAdapter scheduleAdapter;
	AttachmentAdapter attachmentAdapter;
	AttendeesAdapter attendeesAdapter;

	List<Attendee> attendees;

	ImageView img, share, navigate;
	ImageButton img_visibility, confirm_attendance, ask_question;
	EcoGallery ecoGallery;

	ProgressDialog pd;

	private Gathering item;

	private TabHost host;
	private ViewPager pager;

	private Uri fileUri;

	BitmapDrawable[] layers;
	TransitionDrawable transDraw;

	List<Media> images;
	List<Media> attachmentsList;
	ImageAdapter adapter;

	AppMain appMain;
	private User currentUser;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_gathering_view);

		appMain = (AppMain) getApplication();
		currentUser = appMain.getCurrentUser();

		if (getIntent().hasExtra("item")) {
			item = (Gathering) getIntent().getExtras().getSerializable("item");
			item.setContext(GatheringViewActivity.this);
			item.processImagesList();

			title = (TextView) findViewById(R.id.title);
			subtitle = (TextView) findViewById(R.id.subtitle);
			organizer = (TextView) findViewById(R.id.organizer);
			description = (TextView) findViewById(R.id.description);
			eventmaster = (TextView) findViewById(R.id.eventmaster);
			expiration = (TextView) findViewById(R.id.expiration);
			img = (ImageView) findViewById(R.id.img);

			ref_url = (TextView) findViewById(R.id.biz_url);
			fb_url = (TextView) findViewById(R.id.fb_url);
			gplus_url = (TextView) findViewById(R.id.gplus_url);
			twtr_url = (TextView) findViewById(R.id.twtr_url);

			programme = (Button) findViewById(R.id.programme);
			attachment = (Button) findViewById(R.id.attachment);
			reserve = (Button) findViewById(R.id.reserve);

			header = (LinearLayout) findViewById(R.id.header);

			img_visibility = (ImageButton) findViewById(R.id.img_visibility);
			img_visibility.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (header.getVisibility() == View.VISIBLE) {
						header.setVisibility(View.GONE);
					} else {
						header.setVisibility(View.VISIBLE);
					}
				}
			});
			confirm_attendance = (ImageButton) findViewById(R.id.confirm_attendance);
			confirm_attendance.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					try {
						Intent intent = new Intent("com.google.zxing.client.android.SCAN");
						intent.putExtra("SCAN_MODE", "QR_CODE_MODE,PRODUCT_MODE");
						startActivityForResult(intent, ZXingHelper.REQUEST_CODE);
					} catch (Exception e) {
						// TODO Auto-generated catch block     
						e.printStackTrace();
						ToastHelper.toast(GatheringViewActivity.this, "ERROR:" + e, Toast.LENGTH_SHORT);
					}
				}
			});
			ask_question = (ImageButton) findViewById(R.id.ask_question);
			ask_question.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					try {
						String url = "http://www.infordiscuss.codesndbx.com/questions/ask";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
					} catch (Exception e) {
						// TODO Auto-generated catch block     
						e.printStackTrace();

					}
				}
			});
			title.setText(item.getTitle());
			organizer.setText("Organized By: " + item.getOrganizer());
			eventmaster.setText("Event Master: " + item.getEventMaster());
			description.setText(item.getDescription());
			expiration.setText("Starts on: " + new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.ENGLISH).format(item.getDatefrom()));

			ref_url.setText(item.getRef_url());
			fb_url.setText(item.getFacebook_url());
			gplus_url.setText(item.getGplus_url());
			twtr_url.setText(item.getTwitter_url());

			//Launch new thread to retrieve schedules / attendees / attachments
			retrieveSchedules();
			retrieveAttendees();
			retrieveAttachments();
			
			reserve.setVisibility(View.VISIBLE);
			reserve.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(GatheringViewActivity.this, AttendeesWriteActivity.class);
					intent.putExtra("gathering", item);
					startActivityForResult(intent, AttendeesWriteActivity.NEW_REQUEST_CODE);
					//Toast.makeText(GatheringViewActivity.this, "Clicked Reserve", Toast.LENGTH_SHORT).show();
				}
			});


			if (currentUser.getId().equals(item.getEdited_by())) {
				programme.setVisibility(View.VISIBLE);
				programme.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent intent = new Intent(GatheringViewActivity.this, ScheduleWriteActivity.class);
						intent.putExtra("gathering_id", item.getId());
						startActivityForResult(intent, ScheduleWriteActivity.NEW_REQUEST_CODE);
					}
				});

				attachment.setVisibility(View.VISIBLE);
				attachment.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						fileUri = MediaCaptureHelper.pickFile(GatheringViewActivity.this, fileUri);
					}
				});

			} else {
				programme.setVisibility(View.GONE);
				attachment.setVisibility(View.GONE);
			}

			setupTabs();

			// Initialize YouTube Player
			YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
			youTubeView.initialize(YouTubeDeveloperKey.DEVELOPER_KEY, this);

			ecoGallery = (EcoGallery) findViewById(R.id.images);
			ecoGallery.setAnimationDuration(3000);
			if (item.getImagesList() != null && item.getImagesList().size() > 0) {
				images = item.getImagesList();
				adapter = new ImageAdapter(GatheringViewActivity.this);
				ecoGallery.setAdapter(adapter);
				ecoGallery.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(EcoGalleryAdapterView<?> parent, View view, int position, long id) {
						File f = new File(images.get(position).getLocalFilePath());
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse("file://" + f.getAbsolutePath()), "image/*");
						startActivity(intent);
					}
				});
				Runnable timedRunnable = new Runnable() {

					@Override
					public void run() {
						final int selection = (ecoGallery.getSelectedItemPosition() + 1) > adapter.getCount() - 1 ? 0 : ecoGallery.getSelectedItemPosition() + 1;
						ecoGallery.post(new Runnable() {

							@Override
							public void run() {

								//								ecoGallery.setSelection(selection, false);
								ecoGallery.setSelection(selection, true);
							}

						});

						ecoGallery.postDelayed(this, 5000);
					}

				};
				ecoGallery.postDelayed(timedRunnable, 5000);

			} else {
				ecoGallery.setVisibility(View.GONE);
			}
			// End Load image

			share = (ImageView) findViewById(R.id.share);
			share.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean withImage = false;
					File myFile = null;

					if (item.getImagesList() != null && item.getImagesList().size() > 0) {
						withImage = true;
						myFile = new File(images.get(0).getLocalFilePath());
					}

					final StringBuilder msg = new StringBuilder();
					msg.append("Hey, I've come across this offer. Check it out!\n");
					msg.append(item.getTitle() + " by " + item.getEventMaster() + "\n");
					msg.append(item.getDescription() + "\n");
					msg.append("located at " + item.getLoc_text() + "\n");
					msg.append("visit " + item.getRef_url() + " for more info.");

					final Intent intent = new Intent(Intent.ACTION_SEND);
					if (withImage) {
						//						intent.setType(type);
						intent.setType("image/jpeg");
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						intent.putExtra("android.intent.extra.STREAM", Uri.fromFile(myFile));
					} else {
						intent.setType("text/plain");
					}

					ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("label", msg.toString());
					clipboard.setPrimaryClip(clip);

					intent.putExtra(Intent.EXTRA_TEXT, msg.toString());
					intent.putExtra(Intent.EXTRA_SUBJECT, "A cool offer via lokal.ph");
					intent.putExtra(Intent.EXTRA_TITLE, msg.toString());

					try {
						AlertDialog.Builder alert = new AlertDialog.Builder(GatheringViewActivity.this);
						alert.setTitle("Warning");
						alert.setMessage("Sharing to Facebook does not allow us to put anything on the caption field. Thus, we copied the share message to your clipboard. You can long press the caption field and select PASTE to share this offer to your friends and family.\n\nOther services are not affected by this limitation.\n\nFor more information, visit:\nhttps://developers.facebook.com/policy/");
						alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								startActivity(Intent.createChooser(intent, "Share using"));
							}
						});
						alert.setIcon(android.R.drawable.ic_dialog_alert);
						alert.show();

					} catch (android.content.ActivityNotFoundException ex) {
						// (handle error)
					}
				}
			});

			navigate = (ImageView) findViewById(R.id.navigate);
			navigate.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					LatLng currentLoc = LocationHelper.getLastKnownLocationLatLng(GatheringViewActivity.this);
					final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?" + "saddr=" + currentLoc.latitude + "," + currentLoc.longitude + "&daddr=" + item.getLoc_lat() + "," + item.getLoc_lng()));
					intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");

					try {
						startActivity(intent);
					} catch (android.content.ActivityNotFoundException ex) {
						ToastHelper.toast(GatheringViewActivity.this, "No application could handle your navigation request. Please install Google Maps and try again.", Toast.LENGTH_LONG);
					}

				}
			});

		}

	}

	private void retrieveAttendees() {
		attendeesView = (ListView) findViewById(R.id.attendees);
		new Thread(new Runnable() {

			@Override
			public void run() {
				attendees = ApplicationWebService.Attendees.fetchRecordsByGatheringId(GatheringViewActivity.this, item.getId());
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (attendees != null && attendees.size() > 0) {
							attendeesAdapter = new AttendeesAdapter(GatheringViewActivity.this, R.layout.attendees_list, attendees);
							attendeesView.setAdapter(attendeesAdapter);
						} else {
							attendeesView.setAdapter(null);
						}
					}

				});

			}

		}).start();
	}

	private void retrieveSchedules() {
		scheduleView = (ListView) findViewById(R.id.schedule);
		new Thread(new Runnable() {

			@Override
			public void run() {
				schedules = ApplicationWebService.Schedules.fetchRecordsByGatheringId(GatheringViewActivity.this, item.getId());
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (schedules != null && schedules.size() > 0) {
							scheduleAdapter = new ScheduleAdapter(GatheringViewActivity.this, R.layout.schedule_list_row, schedules);
							scheduleView.setAdapter(scheduleAdapter);
						} else {
							scheduleView.setAdapter(null);
						}
					}

				});

			}

		}).start();
	}

	private void retrieveAttachments() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				attachmentsList = ApplicationWebService.Attachments.fetchRecordsByGatheringId(GatheringViewActivity.this, item.getId());
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						populateAttachments();
					}

				});

			}

		}).start();
	}

	private void populateAttachments() {
		attachmentView = (ListView) findViewById(R.id.attachments);
		if (attachmentsList != null && attachmentsList.size() > 0) {
			attachmentAdapter = new AttachmentAdapter(GatheringViewActivity.this, R.layout.attachment_list_row, attachmentsList);
			attachmentView.setAdapter(attachmentAdapter);
		} else {
			attachmentView.setAdapter(null);
		}
	}

	private void setupTabs() {
		host = (TabHost) findViewById(android.R.id.tabhost);
		pager = (ViewPager) findViewById(R.id.pager);

		View tabIndicator;
		TextView title;

		host.setup();
		TabSpec spec = host.newTabSpec("tab1");
		spec.setContent(R.id.tab1);
		tabIndicator = LayoutInflater.from(this).inflate(R.layout.apptheme_tab_indicator_holo, host.getTabWidget(), false);
		title = (TextView) tabIndicator.findViewById(android.R.id.title);
		title.setText("DETAILS");
		spec.setIndicator(tabIndicator);
		host.addTab(spec);

		spec = host.newTabSpec("tab2");
		spec.setContent(R.id.tab2);
		tabIndicator = LayoutInflater.from(this).inflate(R.layout.apptheme_tab_indicator_holo, host.getTabWidget(), false);
		title = (TextView) tabIndicator.findViewById(android.R.id.title);
		title.setText("VIDEO");
		spec.setIndicator(tabIndicator);
		host.addTab(spec);

		spec = host.newTabSpec("tab3");
		spec.setContent(R.id.tab3);
		tabIndicator = LayoutInflater.from(this).inflate(R.layout.apptheme_tab_indicator_holo, host.getTabWidget(), false);
		title = (TextView) tabIndicator.findViewById(android.R.id.title);
		title.setText("WEB");
		spec.setIndicator(tabIndicator);
		host.addTab(spec);

		spec = host.newTabSpec("tab4");
		spec.setContent(R.id.tab4);
		tabIndicator = LayoutInflater.from(this).inflate(R.layout.apptheme_tab_indicator_holo, host.getTabWidget(), false);
		title = (TextView) tabIndicator.findViewById(android.R.id.title);
		title.setText("PROGRAMME");
		spec.setIndicator(tabIndicator);
		host.addTab(spec);

		spec = host.newTabSpec("tab5");
		spec.setContent(R.id.tab5);
		tabIndicator = LayoutInflater.from(this).inflate(R.layout.apptheme_tab_indicator_holo, host.getTabWidget(), false);
		title = (TextView) tabIndicator.findViewById(android.R.id.title);
		title.setText("ATTENDEES");
		spec.setIndicator(tabIndicator);
		host.addTab(spec);

		spec = host.newTabSpec("tab6");
		spec.setContent(R.id.tab6);
		tabIndicator = LayoutInflater.from(this).inflate(R.layout.apptheme_tab_indicator_holo, host.getTabWidget(), false);
		title = (TextView) tabIndicator.findViewById(android.R.id.title);
		title.setText("ATTACHMENTS");
		spec.setIndicator(tabIndicator);
		host.addTab(spec);

		// pager.setAdapter(new MyPagerAdapter(ViewOfferActivity.this));
		pager.setOnPageChangeListener(this);
		host.setOnTabChangedListener(this);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onTabChanged(String tabId) {
		int pageNumber = 0;
		if (tabId.equals("tab1")) {
			pageNumber = 0;
		} else if (tabId.equals("tab2")) {
			pageNumber = 1;
		} else if (tabId.equals("tab3")) {
			pageNumber = 2;
		} else if (tabId.equals("tab4")) {
			pageNumber = 3;
		} else if (tabId.equals("tab5")) {
			pageNumber = 4;
		} else if (tabId.equals("tab6")) {
			pageNumber = 5;
		} else {
			pageNumber = 0;
		}
		pager.setCurrentItem(pageNumber);
	}

	@Override
	public void onPageSelected(int pageNumber) {
		host.setCurrentTab(pageNumber);
	}

	public class MyPagerAdapter extends PagerAdapter {
		private Context ctx;

		public MyPagerAdapter(Context ctx) {
			this.ctx = ctx;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			TextView tView = new TextView(ctx);
			position++;
			tView.setText("Page number: " + position);
			tView.setTextColor(Color.RED);
			tView.setTextSize(20);
			container.addView(tView);
			return tView;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return (view == object);
		}
	}

	private final String YOUTUBE_DEFAULT_ID = "0s7iKlAycfo";

	@Override
	public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
		if (!wasRestored) {

			if (item.getYoutube_url() == null || item.getYoutube_url().length() == 0) {
				player.cueVideo(YOUTUBE_DEFAULT_ID);
			} else {
				// extract ID from URL
				String url = item.getYoutube_url();
				Pattern p = Pattern.compile("http.*\\?v=([a-zA-Z0-9_\\-]+)(?:&.)*");
				Matcher m = p.matcher(url);

				if (m.matches()) {
					url = m.group(1);
				}

				player.cueVideo(url);
			}
		}
	}

	@Override
	protected YouTubePlayer.Provider getYouTubePlayerProvider() {
		return (YouTubePlayerView) findViewById(R.id.youtube_view);
	}

	public class ImageAdapter extends BaseAdapter {
		Context context;

		public ImageAdapter(Context context) {
			this.context = context;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			ImageView imageView;
			imageView = new ImageView(GatheringViewActivity.this);

			final String imageString = images.get(position).getLocalFilePath();
			Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromFile(imageString, 200, 200);
			imageView.setImageBitmap(bitmap);
			imageView.setBackgroundColor(Color.RED);
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

	public boolean onCreateOptionsMenu(Menu menu) {
		if (currentUser.getId().equals(item.getEdited_by())) {
			android.view.MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.gathering_view, menu);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			this.finish();
			return true;

		case R.id.menu_qr:
			String content = "?id=" + item.getItemId() + "&user_id=" + currentUser.getId();
			Bitmap qr = ZXingHelper.generateQRCode(content, 200, 200);
			File f = new File(ApplicationUtils.Paths.getLocalAppImagesFolder(GatheringViewActivity.this), "qrtemp.jpg");
			try {
				BitmapHelper.saveBitmapToSD(qr, f.getAbsolutePath());

				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse("file://" + f.getAbsolutePath()), "image/*");
				startActivity(intent);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;

		}
		return true;
	}

	public class ScheduleAdapter extends ArrayAdapter<Schedule> {

		List<Schedule> objects;

		public ScheduleAdapter(Context context, int textViewResourceId, List<Schedule> objects) {
			super(context, textViewResourceId, objects);
			this.objects = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			LayoutInflater inflater = getLayoutInflater();

			final Schedule item = objects.get(position);

			convertView = inflater.inflate(R.layout.schedule_list_row, parent, false);

			final Button overflow = (Button) convertView.findViewById(R.id.overflow);
			final TextView txtTitle = (TextView) convertView.findViewById(R.id.title);
			final TextView txtVenue = (TextView) convertView.findViewById(R.id.venue);
			final TextView txtOrganizer = (TextView) convertView.findViewById(R.id.host);
			final TextView txtDate = (TextView) convertView.findViewById(R.id.date);

			txtTitle.setText(item.getTitle());
			txtVenue.setText(item.getVenue());
			txtOrganizer.setText("Hosted By: " + item.getHost());
			txtDate.setText(new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.ENGLISH).format(item.getTimestart()) + " To " + new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.ENGLISH).format(item.getTimeend()));

			if (currentUser.getId().equals(item.getEdited_by())) {
				//Show overflow
				overflow.setVisibility(View.VISIBLE);
			} else {
				//Hide overflow
				overflow.setVisibility(View.GONE);
			}

			final PopupMenu popup = new PopupMenu(GatheringViewActivity.this, overflow);
			//set overflow options
			popup.getMenuInflater().inflate(R.menu.schedule_list_overflow_menu, popup.getMenu());
			popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(android.view.MenuItem menu) {
					switch (menu.getItemId()) {

					case R.id.menu_edit:
						editSchedule(item);

						break;

					case R.id.menu_delete:
						AlertDialog.Builder alert = new AlertDialog.Builder(GatheringViewActivity.this);
						alert.setTitle("Confirm removal");
						alert.setMessage("Are you sure that you want to delete this entry?");
						alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								deleteSchedule(item);
							}
						});
						alert.setIcon(android.R.drawable.ic_dialog_alert);
						alert.show();
						break;

					default:
						break;
					}

					return true;
				}

			});

			overflow.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popup.show();
				}
			});

			return convertView;
		}
	}

	public class AttachmentAdapter extends ArrayAdapter<Media> {

		List<Media> objects;

		public AttachmentAdapter(Context context, int textViewResourceId, List<Media> objects) {
			super(context, textViewResourceId, objects);
			this.objects = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			LayoutInflater inflater = getLayoutInflater();

			final Media item = objects.get(position);

			convertView = inflater.inflate(R.layout.attachment_list_row, parent, false);

			SimpleDateFormat df = new SimpleDateFormat("MMMM dd yyyy hh:mm a", Locale.ENGLISH);

			final Button overflow = (Button) convertView.findViewById(R.id.overflow);
			final ImageButton download = (ImageButton) convertView.findViewById(R.id.download);
			final ImageButton delete = (ImageButton) convertView.findViewById(R.id.delete);
			final TextView txtTitle = (TextView) convertView.findViewById(R.id.title);
			final TextView txtSubtitle = (TextView) convertView.findViewById(R.id.subtitle);

			txtTitle.setText(item.getName());
			txtSubtitle.setText(df.format(item.getCreated()));

			download.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (DownloadManagerHelper.isDownloadManagerAvailable(GatheringViewActivity.this)) {
						StringBuilder sb = new StringBuilder();
						sb.append("http://codesndbx.com/android/inforgather/files/");
						sb.append(item.getOwner_id() + "/" + item.getName());
						DownloadManagerHelper.downloadFromURL(GatheringViewActivity.this, sb.toString(), item.getName());
					} else {
						ToastHelper.toast(GatheringViewActivity.this, "Unsupported Android version found. This feature is only supported in Android 2.3 and up.", Toast.LENGTH_LONG);
					}
				}

			});

			if (currentUser.getId().equals(item.getEdited_by())) {
				//Show delete
				delete.setVisibility(View.VISIBLE);
				delete.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						showProgressDialog("Hang on", "Processing...", false);
						new Thread(new Runnable() {

							@Override
							public void run() {
								if (ApplicationWebService.Attachments.deleteRecordById(GatheringViewActivity.this, item)) {
									runOnUiThread(new Runnable() {

										@Override
										public void run() {
											attachmentsList.remove(item);
											populateAttachments();
											dismissProgressDialog();
										}

									});

								}
							}

						}).start();
					}
				});
			} else {
				//Hide delete
				delete.setVisibility(View.GONE);
			}

			return convertView;
		}
	}

	
	public class AttendeesAdapter extends ArrayAdapter<Attendee> {

		List<Attendee> objects;

		public AttendeesAdapter(Context context, int textViewResourceId, List<Attendee> objects) {
			super(context, textViewResourceId, objects);
			this.objects = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			LayoutInflater inflater = getLayoutInflater();

			final Attendee item = objects.get(position);

			convertView = inflater.inflate(R.layout.attendees_list, parent, false);
			
			final TextView attendeeName = (TextView) convertView.findViewById(R.id.attendee_name);
			final TextView attendeeStatus = (TextView) convertView.findViewById(R.id.attendee_status);
			attendeeName.setText(item.getFullname());
			attendeeStatus.setText(item.getStatus());
			
			//TODO set permissions based on user-created
			//if (currentUser.getId().equals(item.get))

			/*final Button overflow = (Button) convertView.findViewById(R.id.overflow);
			final TextView txtTitle = (TextView) convertView.findViewById(R.id.title);

			txtTitle.setText(item.getTitle());
			if (currentUser.getId().equals(item.getEdited_by())) {
				//Show overflow
				overflow.setVisibility(View.VISIBLE);
			} else {
				//Hide overflow
				overflow.setVisibility(View.GONE);
			}

			final PopupMenu popup = new PopupMenu(GatheringViewActivity.this, overflow);
			//set overflow options
			popup.getMenuInflater().inflate(R.menu.schedule_list_overflow_menu, popup.getMenu());
			popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(android.view.MenuItem menu) {
					switch (menu.getItemId()) {

					case R.id.menu_edit:
						editSchedule(item);

						break;

					case R.id.menu_delete:
						AlertDialog.Builder alert = new AlertDialog.Builder(GatheringViewActivity.this);
						alert.setTitle("Confirm removal");
						alert.setMessage("Are you sure that you want to delete this entry?");
						alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								deleteSchedule(item);
							}
						});
						alert.setIcon(android.R.drawable.ic_dialog_alert);
						alert.show();
						break;

					default:
						break;
					}

					return true;
				}

			});

			overflow.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popup.show();
				}
			});*/

			return convertView;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == ScheduleWriteActivity.NEW_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {

				retrieveSchedules();
			}
		} else if (requestCode == ZXingHelper.REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {

				String result = data.getStringExtra("SCAN_RESULT");
				if (result != null && result.length() > 0) {

					//					initiatePaymentPreparation(result);
					Log.v(TAG, "Update attendance status here... " + result);
					ToastHelper.toast(GatheringViewActivity.this, "Attendance confirmed.", Toast.LENGTH_LONG);
				}

			} else if (resultCode == Activity.RESULT_CANCELED) {
				Toast.makeText(GatheringViewActivity.this, "Scanning cancelled", Toast.LENGTH_SHORT).show();
			}
		} else if (requestCode == MediaCaptureHelper.PICKER_ANY_FILE_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				showProgressDialog("Hang on", "Attaching and updating back-end.", false);
				if (attachmentsList == null) {
					attachmentsList = new ArrayList<Media>();
				}

				File f = new File(data.getData().getPath());

				final Media media = new Media(GatheringViewActivity.this);
				media.setId(GUIDHelper.createGUID());
				media.setOwner_id(item.getId());
				media.setType(Media.MEDIA_TYPE_DOCUMENT);
				media.setLocalFilePath(data.getData().getPath());
				media.setName(f.getName());
				media.setStatus(Media.MEDIA_STATUS_NEW);

				User currentUser = appMain.getCurrentUser();

				media.setEdited_by(currentUser.getId());
				media.setCreated(Calendar.getInstance().getTimeInMillis());
				media.setUpdated(Calendar.getInstance().getTimeInMillis());
				media.setVersion(1);

				new Thread(new Runnable() {

					@Override
					public void run() {
						if (ApplicationWebService.Attachments.pushFileToBackEnd(GatheringViewActivity.this, media)) {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									attachmentsList.add(media);
									populateAttachments();
									dismissProgressDialog();
								}

							});

						}
					}

				}).start();

			}
		}
	}

	private void editSchedule(Schedule schedule) {
		Intent intent = new Intent(GatheringViewActivity.this, ScheduleWriteActivity.class);
		intent.putExtra("item", schedule);
		startActivityForResult(intent, ScheduleWriteActivity.NEW_REQUEST_CODE);
	}

	private void deleteSchedule(final Schedule schedule) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				if (ApplicationWebService.Schedules.deleteFromBackEnd(GatheringViewActivity.this, schedule)) {
					retrieveSchedules();
				}
			}
		}).start();

	}

	private void showProgressDialog(String title, String msg, boolean cancelable) {
		pd = new ProgressDialog(GatheringViewActivity.this);
		pd.setTitle(title);
		pd.setMessage(msg);
		pd.setCancelable(cancelable);
		pd.show();
	}

	private void dismissProgressDialog() {
		if (pd != null) {
			pd.dismiss();
		}
	}

}
