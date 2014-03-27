package com.dabeshackers.infor.gather;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.dabeshackers.infor.gather.application.AppMain;
import com.dabeshackers.infor.gather.entities.Gathering;
import com.dabeshackers.infor.gather.entities.User;
import com.dabeshackers.infor.gather.helpers.FileOperationsHelper;
import com.dabeshackers.infor.gather.helpers.LocationHelper;
import com.dabeshackers.infor.gather.helpers.LocationHelper.LocationResult;
import com.dabeshackers.infor.gather.http.ApplicationWebService;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.markupartist.android.widget.PullToRefreshListView;
import com.markupartist.android.widget.PullToRefreshListView.OnRefreshListener;

public class GatheringListFragment extends SherlockListFragment {
	private static final String TAG = GatheringListFragment.class.getSimpleName();
	public static final String TITLE = "Gatherings";

	private Activity activity;

	private List<Gathering> items = new ArrayList<Gathering>();
	private List<Gathering> filtered_items = new ArrayList<Gathering>();

	private AppMain appMain;

	private boolean isSearchEnabled;
	private ProgressDialog pd;

	private String currentLocationText;
	private LatLng currentLatLng;

	private User currentUser;

	private boolean isLoading;
	private boolean isLoadingPrevious;

	private int dbCurrentRow = 0;
	private static final int DB_ROW_LIMIT = 5;

	private MyCustomAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		return inflater.inflate(R.layout.fragment_gatherings_list, container, false);

	}

	@Override
	public void onAttach(Activity activity) {
		this.activity = activity;
		super.onAttach(activity);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		Handler handler = new Handler();
		handler.post(new Runnable() {

			@Override
			public void run() {
				// Show Progress Dialog
				pd = new ProgressDialog(activity);
				pd.setIndeterminate(true);
				pd.setMessage("Waiting for location...");
				pd.setTitle("Please wait.");
				pd.setCancelable(false);
				pd.show();
			}

		});

		appMain = (AppMain) activity.getApplication();
		currentUser = appMain.getCurrentUser();

		View footerView = ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listfooterview, null, false);
		final ImageView animImageView = (ImageView) footerView.findViewById(R.id.loading);
		animImageView.setBackgroundResource(R.drawable.loader);
		animImageView.post(new Runnable() {
			@Override
			public void run() {
				AnimationDrawable frameAnimation = (AnimationDrawable) animImageView.getBackground();
				frameAnimation.start();
			}
		});
		getListView().addFooterView(footerView);

		//Add bottom scrolling
		getListView().setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				//what is the bottom item that is visible
				int lastInScreen = firstVisibleItem + visibleItemCount;
				//is the bottom item visible & not loading more already ? Load more !
				if ((lastInScreen == totalItemCount) && !isLoadingPrevious) {
					// Perform sync then refresh
					new Thread(new Runnable() {

						public void run() {
							isLoadingPrevious = true;

							fillData(FillDataTypeEnum.FILLDATA_GET_NEXT_SET);

							//Inform UI thread of the changes and update UI as needed
							activity.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									adapter.notifyDataSetChanged();
									//									loadingMore = false;
									isLoadingPrevious = false;
								}

							});
						}

					}).start();
				}
			}
		});

		//Pull to refresh enablement
		((PullToRefreshListView) getListView()).setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				if (isLoading) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							((PullToRefreshListView) getListView()).onRefreshComplete();
						}
					});
					return;

				}
				//Do work to refresh the list here.
				//Peform retrieval from DB
				activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
				isLoading = true;

				new Thread(new Runnable() {

					public void run() {
						dbCurrentRow = 0; //reset row counter
						fillData(FillDataTypeEnum.FILLDATA_REQUERY);

						//Inform UI thread of the changes and update UI as needed
						activity.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								adapter.notifyDataSetChanged();
								((PullToRefreshListView) getListView()).onRefreshComplete();
							}

						});
					}

				}).start();
			}
		});

		setHasOptionsMenu(true);
		locate();
	}

	private void locate() {
		// Set current location
		LocationHelper myLocation = new LocationHelper();
		//		updateProgressDialog("Waiting for location...");
		myLocation.getLocation(activity, new LocationResult() {

			@Override
			public void gotLocation(LatLng latLng) {
				if (latLng != null) {
					currentLatLng = latLng;
				} else {
					currentLatLng = LocationHelper.getDefaultLocationLatLng(activity);
				}
				currentLocationText = LocationHelper.getReadableAddressFromLatLng(activity, currentLatLng, false);

				// Determine if first time user or not -- letting user modify and pushing it to host if so.
				// currentUser = appMain.getCurrentUser(); this is set on the earlier part of this method
				new Thread(new Runnable() {

					@Override
					public void run() {
						if (ApplicationWebService.Users.isUserExists(activity, currentUser.getId())) {
							currentUser = ApplicationWebService.Users.getUser(activity, currentUser);

						} else {
							((MainContainerActivity) activity).isUpdateProfileRequired = true;
							Intent intent = new Intent(activity, EditUserActivity.class);
							intent.putExtra("lat", currentLatLng.latitude);
							intent.putExtra("lng", currentLatLng.longitude);
							intent.putExtra("loc_text", currentLocationText);
							startActivityForResult(intent, EditUserActivity.REQUESTCODE);
						}

						activity.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								//Finally, start getting some items
								if (items.isEmpty() && !isLoading) {
									Log.i(TAG, "Retrieving records");
									activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
									isLoading = true;
									fillData(FillDataTypeEnum.FILLDATA_REQUERY);

								}

								pd.dismiss();

							}
						});

					}

				}).start();

			}
		});
	}

	private enum FillDataTypeEnum {
		FILLDATA_REQUERY, FILLDATA_GET_NEXT_SET, FILLDATA_USE_FILTERED_ITEMS, FILLDATA_USE_UNFILTERED_ITEMS
	}

	private void fillData(FillDataTypeEnum fillType) {
		try {
			switch (fillType) {
			case FILLDATA_GET_NEXT_SET:

				List<Gathering> newOffers = new ArrayList<Gathering>();
				newOffers = ApplicationWebService.Gatherings.fetchRecords(activity, dbCurrentRow);

				if (newOffers != null && newOffers.size() > 0) {
					items.addAll(newOffers);
					dbCurrentRow += DB_ROW_LIMIT;
				}

				// Load adapter to list and Disable loading animation in actionbar
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);
						isLoading = false;
					}
				});

				break;

			case FILLDATA_REQUERY:
				activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);

				new Thread(new Runnable() {

					@Override
					public void run() {
						// Webservice calls are done here
						items = ApplicationWebService.Gatherings.fetchRecords(activity, dbCurrentRow);
						dbCurrentRow += DB_ROW_LIMIT;

						if (items == null || items.size() == 0) {
							// No items retrieved due to some reason. hence, we use cached data
							try {
								retrieveCache();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						adapter = new MyCustomAdapter(activity.getApplicationContext(), R.layout.activity_activities_list_row, items);

						// Load adapter to list and Disable loading animation in actionbar
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setListAdapter(adapter);
								activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);
								isLoading = false;
							}
						});
					}

				}).start();

				break;

			case FILLDATA_USE_UNFILTERED_ITEMS:

				adapter = new MyCustomAdapter(activity.getApplicationContext(), R.layout.activity_activities_list_row, items);
				setListAdapter(adapter);
				activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);

				break;

			case FILLDATA_USE_FILTERED_ITEMS:

				adapter = new MyCustomAdapter(activity.getApplicationContext(), R.layout.activity_activities_list_row, filtered_items);
				setListAdapter(adapter);
				activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);

				break;

			default:
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public class MyCustomAdapter extends ArrayAdapter<Gathering> {

		List<Gathering> objects;

		public MyCustomAdapter(Context context, int textViewResourceId, List<Gathering> objects) {
			super(context, textViewResourceId, objects);
			this.objects = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			LayoutInflater inflater = LayoutInflater.from(activity);

			final Gathering item = objects.get(position);

			convertView = inflater.inflate(R.layout.fragment_gatherings_list_row, parent, false);

			final Button overflow = (Button) convertView.findViewById(R.id.overflow);
			final ImageView media = (ImageView) convertView.findViewById(R.id.media);
			final TextView txtTitle = (TextView) convertView.findViewById(R.id.title);
			final TextView txtSubtitle = (TextView) convertView.findViewById(R.id.subtitle);
			final TextView txtOrganizer = (TextView) convertView.findViewById(R.id.organizer);
			final TextView txtContent = (TextView) convertView.findViewById(R.id.content);
			final TextView txtAddress = (TextView) convertView.findViewById(R.id.address);
			final TextView txtExpiration = (TextView) convertView.findViewById(R.id.expiration);

			txtTitle.setText(item.getTitle());
			txtSubtitle.setText(item.getSubtitle());
			txtOrganizer.setText("Organized By: " + item.getOrganizer());
			txtContent.setText(item.getDescription());
			txtAddress.setText(item.getLoc_text());

			txtExpiration.setText("Will start on: " + new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.ENGLISH).format(item.getDatefrom()));

			if (currentUser.getId().equals(item.getEdited_by())) {
				//Show overflow
				overflow.setVisibility(View.VISIBLE);
			} else {
				//Hide overflow
				overflow.setVisibility(View.GONE);
			}

			// Load image
			if (item.getImagesList() != null && item.getImagesList().size() > 0) {

				Bitmap b = item.getCurrentImageFromList();
				media.setImageBitmap(b);

				final int delaySecondsMin = 10;
				final int delaySecondsMax = 20;

				final Runnable timedAnimation = new Runnable() {

					@Override
					public void run() {
						final Animation animationFadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
						final Animation animationFadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out);

						AnimationListener animListener = new AnimationListener() {
							Bitmap bitmap;

							@Override
							public void onAnimationStart(Animation animation) {
								bitmap = item.getNextImageFromList(); // Was set here so that when the list is scrolled. we still have the latest image to display
							}

							@Override
							public void onAnimationRepeat(Animation animation) {
							}

							@Override
							public void onAnimationEnd(Animation animation) {
								media.setImageBitmap(bitmap);
								media.startAnimation(animationFadeIn);
							}
						};
						animationFadeOut.setAnimationListener(animListener);
						media.startAnimation(animationFadeOut);

						media.postDelayed(this, (new Random().nextInt((delaySecondsMax - delaySecondsMin) + 1) + delaySecondsMin) * 1000);
					}
				};

				//Enable image animation for 2 or more images
				if (item.getImagesList().size() > 1) {
					media.postDelayed(timedAnimation, (new Random().nextInt((delaySecondsMax - delaySecondsMin) + 1) + delaySecondsMin) * 1000);
				}
			} else {
				media.setVisibility(View.GONE);
			}
			// End Load image

			// Click handler
			convertView.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Intent intent = new Intent(activity, GatheringViewActivity.class);
					intent.putExtra("item", item);
					startActivity(intent);
				}
			});

			final PopupMenu popup = new PopupMenu(activity, overflow);
			//set overflow options
			popup.getMenuInflater().inflate(R.menu.activity_gathering_overflow_menu, popup.getMenu());
			popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(android.view.MenuItem menu) {
					switch (menu.getItemId()) {

					case R.id.menu_edit:
						editGathering(item);

						break;

					case R.id.menu_share:
						boolean withImage = false;
						File myFile = null;

						if (item.getImagesList() != null && item.getImagesList().size() > 0) {
							withImage = true;
							myFile = new File(item.getImagesList().get(0).getLocalFilePath());
						}

						final StringBuilder msg = new StringBuilder();
						msg.append("Hey, I've come across this gathering. Check it out!\n");
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

						ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(activity.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("label", msg.toString());
						clipboard.setPrimaryClip(clip);

						intent.putExtra(Intent.EXTRA_TEXT, msg.toString());
						intent.putExtra(Intent.EXTRA_SUBJECT, "A cool offer via lokal.ph");
						intent.putExtra(Intent.EXTRA_TITLE, msg.toString());

						try {
							AlertDialog.Builder alert = new AlertDialog.Builder(activity);
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

	private final String CACHED_OFFERS = "offercache.dat";

	public void retrieveCache() throws IOException {
		Context context = activity;
		String data = FileOperationsHelper.readFromFile(context, CACHED_OFFERS);
		Gson gsonDetail = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
		InputStream stream = new ByteArrayInputStream(data.getBytes("UTF-8"));
		JsonReader readerDetail = new JsonReader(new InputStreamReader(stream));

		readerDetail.beginArray();
		while (readerDetail.hasNext()) {
			Gathering item = gsonDetail.fromJson(readerDetail, Gathering.class);

			item.setContext(context);
			item.processImagesList();
			items.add(item);
		}
		readerDetail.endArray();
		readerDetail.close();

	}

	public void createCache() {
		Gson gson = new Gson();
		String json = gson.toJson(items);
		FileOperationsHelper.writeToFile(activity, json, CACHED_OFFERS);

	}

	private void editGathering(Gathering item) {
		Intent intent = new Intent(activity, GatheringWriteActivity.class);
		intent.putExtra("item", item);
		intent.putExtra("lat", currentLatLng.latitude);
		intent.putExtra("lng", currentLatLng.longitude);
		intent.putExtra("loc_text", currentLocationText);
		startActivityForResult(intent, GatheringWriteActivity.NEW_OFFER_REQUEST_CODE);
	}

	protected void draftsButtonClicked() {
		dbCurrentRow = 0; //reset row counter
		activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
		isLoading = true;

		// Perform sync then refresh
		new Thread(new Runnable() {

			public void run() {
				fillData(FillDataTypeEnum.FILLDATA_REQUERY);
			}

		}).start();
	}

	protected void publishedButtonClicked() {
		dbCurrentRow = 0; //reset row counter
		activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
		isLoading = true;

		// Perform sync then refresh
		new Thread(new Runnable() {

			public void run() {
				fillData(FillDataTypeEnum.FILLDATA_REQUERY);
			}

		}).start();
	}

	EditText editsearch;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		// Get the options menu view from menu.xml in menu folder
		inflater.inflate(R.menu.fragment_events_list, menu);

		// Locate the EditText in menu.xml
		editsearch = (EditText) menu.findItem(R.id.menu_search_item).getActionView();

		// Capture Text in EditText
		editsearch.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {

			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence s, int arg1, int arg2, int arg3) {
				String text = s.toString().toLowerCase(Locale.getDefault());

				if (isSearchEnabled) {
					activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
					filtered_items.clear();
					for (Gathering offer : items) {
						if (offer.toString().toLowerCase(Locale.ENGLISH).contains(text)) {
							filtered_items.add(offer);
						}
					}

					fillData(FillDataTypeEnum.FILLDATA_USE_FILTERED_ITEMS);
				} else {
					fillData(FillDataTypeEnum.FILLDATA_USE_UNFILTERED_ITEMS);
				}

			}

		});

		// Show the search menu item in menu.xml
		MenuItem menuSearch = menu.findItem(R.id.menu_search_item);
		menuSearch.setOnActionExpandListener(new OnActionExpandListener() {

			// Menu Action Collapse
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				// Empty EditText to remove text filtering
				isSearchEnabled = false;
				editsearch.setText("");
				editsearch.clearFocus();

				return true;
			}

			// Menu Action Expand
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				isSearchEnabled = true;
				// Focus on EditText
				editsearch.requestFocus();

				// Force the keyboard to show on EditText focus
				InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

				return true;
			}
		});

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add:

			Intent intent = new Intent(activity, GatheringWriteActivity.class);
			intent.putExtra("lat", currentLatLng.latitude);
			intent.putExtra("lng", currentLatLng.longitude);
			intent.putExtra("loc_text", currentLocationText);
			startActivityForResult(intent, GatheringWriteActivity.NEW_OFFER_REQUEST_CODE);
			//			startActivity(ActivityIntent);
			return true;
		}
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == GatheringWriteActivity.NEW_OFFER_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {

				activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
				dbCurrentRow = 0;
				isLoading = true;
				items.clear();
				adapter.notifyDataSetChanged();

				// Perform sync then refresh
				new Thread(new Runnable() {

					public void run() {
						appMain.requestSync();
						fillData(FillDataTypeEnum.FILLDATA_REQUERY);
					}

				}).start();
			}
		}

	}

	@Override
	public void onDetach() {
		Log.d(TAG, "Fragment Detaching");
		adapter.clear();
		super.onDetach();
	}

}
