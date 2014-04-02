package com.dabeshackers.infor.gather;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.feras.ecogallery.EcoGallery;
import us.feras.ecogallery.EcoGalleryAdapterView;
import us.feras.ecogallery.EcoGalleryAdapterView.OnItemClickListener;
import android.app.AlertDialog;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.dabeshackers.infor.gather.application.AppMain;
import com.dabeshackers.infor.gather.entities.Gathering;
import com.dabeshackers.infor.gather.entities.Media;
import com.dabeshackers.infor.gather.entities.User;
import com.dabeshackers.infor.gather.helpers.BitmapHelper;
import com.dabeshackers.infor.gather.helpers.LocationHelper;
import com.dabeshackers.infor.gather.helpers.ToastHelper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

public class GatheringViewActivity extends YouTubeFailureRecoveryActivity implements OnTabChangeListener, OnPageChangeListener {

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

	Button programme, reserve;

	ImageView img, share, navigate;
	EcoGallery ecoGallery;

	private Gathering item;

	private TabHost host;
	private ViewPager pager;

	BitmapDrawable[] layers;
	TransitionDrawable transDraw;

	List<Media> images;
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
			reserve = (Button) findViewById(R.id.reserve);

			title.setText(item.getTitle());
			organizer.setText("Organized By: " + item.getOrganizer());
			eventmaster.setText("Event Master: " + item.getEventMaster());
			description.setText(item.getDescription());
			expiration.setText("Starts on: " + new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.ENGLISH).format(item.getDatefrom()));

			ref_url.setText(item.getRef_url());
			fb_url.setText(item.getFacebook_url());
			gplus_url.setText(item.getGplus_url());
			twtr_url.setText(item.getTwitter_url());

			if (currentUser.getId().equals(item.getEdited_by())) {
				programme.setVisibility(View.VISIBLE);
				programme.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent intent = new Intent(GatheringViewActivity.this, ScheduleWriteActivity.class);
						startActivityForResult(intent, ScheduleWriteActivity.NEW_REQUEST_CODE);
					}
				});
			} else {
				programme.setVisibility(View.GONE);
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
						//						ecoGallery.setSelection(item.get, true);
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

	private void setupTabs() {
		host = (TabHost) findViewById(android.R.id.tabhost);
		pager = (ViewPager) findViewById(R.id.pager);

		host.setup();
		TabSpec spec = host.newTabSpec("tab1");
		spec.setContent(R.id.tab1);
		spec.setIndicator("Details");
		host.addTab(spec);

		spec = host.newTabSpec("tab2");
		spec.setContent(R.id.tab2);
		spec.setIndicator("Youtube");
		host.addTab(spec);

		spec = host.newTabSpec("tab3");
		spec.setContent(R.id.tab3);
		spec.setIndicator("Web");
		host.addTab(spec);

		spec = host.newTabSpec("tab4");
		spec.setContent(R.id.tab4);
		spec.setIndicator("Programme");
		host.addTab(spec);

		spec = host.newTabSpec("tab5");
		spec.setContent(R.id.tab5);
		spec.setIndicator("Attendees");
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
		} else {
			pageNumber = 5;
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

			break;

		}
		return true;
	}

}
