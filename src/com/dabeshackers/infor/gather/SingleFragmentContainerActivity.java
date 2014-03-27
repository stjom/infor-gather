package com.dabeshackers.infor.gather;

import android.os.Bundle;
import android.view.Window;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class SingleFragmentContainerActivity extends SherlockFragmentActivity {

	private static final String TAG = SingleFragmentContainerActivity.class.getSimpleName();

	public final String EXTRA_FRAGMENT_TO_BE_LOADED = "FragmentToBeLoaded";
	public final int FRAGMENT_GATHERING_VIEW = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_single_fragment_container);

		// Check that the activity is using the layout version with
		// the fragment_container FrameLayout
		if (findViewById(R.id.fragment_container) != null) {

			// However, if we're being restored from a previous state,
			// then we don't need to do anything and should return or else
			// we could end up with overlapping fragments.
			if (savedInstanceState != null) {
				return;
			}

			if (getIntent() != null && getIntent().getExtras() != null && getIntent().hasExtra(EXTRA_FRAGMENT_TO_BE_LOADED)) {
				//				int fragment = getIntent().getExtras().getInt(EXTRA_FRAGMENT_TO_BE_LOADED, 0);
				//				switch (fragment) {
				//				case FRAGMENT_GATHERING_VIEW:
				//					// Create a new Fragment to be placed in the activity layout
				//					GatheringViewActivity gatheringViewActivity = new GatheringViewActivity();
				//
				//					// In case this activity was started with special instructions from an
				//					// Intent, pass the Intent's extras to the fragment as arguments
				//					//					gatheringViewActivity.setArguments(getIntent().getExtras());
				//
				//					// Add the fragment to the 'fragment_container' FrameLayout
				//					getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, gatheringViewActivity).commit();
				//
				//					break;
				//				default:
				//					break;
				//				}

			}

		}

	}
}
