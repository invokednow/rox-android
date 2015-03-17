package com.grayfox.android.app.fragment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import com.getbase.floatingactionbutton.FloatingActionButton;

import com.grayfox.android.app.R;
import com.grayfox.android.app.location.LocationGeocoder;
import com.grayfox.android.app.location.LocationRequester;
import com.grayfox.android.client.model.Location;
import com.grayfox.android.client.model.Recommendation;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseExploreFragment extends RoboFragment implements LocationRequester.LocationCallback {

    @InjectView(R.id.searching_layout) private LinearLayout searchingLayout;
    @InjectView(R.id.searching_text)   private TextView searchingTextView;
    @InjectView(R.id.refresh_button)   private FloatingActionButton refreshButton;
    @InjectView(R.id.pager_strip)      private PagerSlidingTabStrip pagerStrip;
    @InjectView(R.id.view_pager)       private ViewPager viewPager;

    private SwipeRouteDetailFragmentsAdapter swipeRouteDetailFragmentsAdapter;
    private LocationRequester locationRequester;
    private Location lastLocation;
    private String lastAddress;
    private Recommendation[] lastRecommendations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRouteDetailFragmentsAdapter = new SwipeRouteDetailFragmentsAdapter();
        viewPager.setAdapter(swipeRouteDetailFragmentsAdapter);
        pagerStrip.setViewPager(viewPager);
        refreshButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                onPreLocateUser();
                onLocateUser();
            }
        });
        if (savedInstanceState == null) {
            viewPager.setVisibility(View.GONE);
            pagerStrip.setVisibility(View.GONE);
            refreshButton.setVisibility(View.VISIBLE);
            searchingLayout.setVisibility(View.GONE);
        } else onRestoreInstanceState();
    }

    private void onRestoreInstanceState() {
        if (getLocationRequester() != null && !getLocationRequester().isStopped()) onPreLocateUser();
        else onChildRestoreInstanceState();
    }

    protected abstract void onChildRestoreInstanceState();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            locationRequester = new LocationRequester(getActivity().getApplicationContext());
            onPreLocateUser();
            onLocateUser();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationRequester.stopRequestingLocation();
    }

    protected void onPreLocateUser() {
        viewPager.setVisibility(View.GONE);
        pagerStrip.setVisibility(View.GONE);
        refreshButton.setVisibility(View.GONE);
        searchingLayout.setVisibility(View.VISIBLE);
        searchingTextView.setText(R.string.waiting_location_update);
    }

    protected void onLocateUser() {
        if (isConnected()) locationRequester.requestSingle(this);
        else onNetworkDisabled();
    }

    @Override
    public void onLocationAcquired(android.location.Location location) {
        Location myLocation = new Location();
        myLocation.setLatitude(location.getLatitude());
        myLocation.setLongitude(location.getLongitude());
        lastLocation = myLocation;
        lastAddress = LocationGeocoder.getAddress(getActivity().getApplicationContext(), location);
    }

    @Override
    public void onLocationRequestTimeout() {
        viewPager.setVisibility(View.GONE);
        pagerStrip.setVisibility(View.GONE);
        refreshButton.setVisibility(View.VISIBLE);
        searchingLayout.setVisibility(View.GONE);
        Toast.makeText(getActivity().getApplicationContext(),
                R.string.location_update_timeout, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationProvidersDisabled() {
        viewPager.setVisibility(View.GONE);
        pagerStrip.setVisibility(View.GONE);
        refreshButton.setVisibility(View.VISIBLE);
        searchingLayout.setVisibility(View.GONE);
        Toast.makeText(getActivity().getApplicationContext(),
                R.string.enable_location_updates, Toast.LENGTH_SHORT).show();
    }

    private void onNetworkDisabled() {
        viewPager.setVisibility(View.GONE);
        pagerStrip.setVisibility(View.GONE);
        refreshButton.setVisibility(View.VISIBLE);
        searchingLayout.setVisibility(View.GONE);
        Toast.makeText(getActivity().getApplicationContext(),
                R.string.network_unavailable, Toast.LENGTH_LONG).show();
    }

    protected void onPreSearchRecommendations() {
        viewPager.setVisibility(View.GONE);
        pagerStrip.setVisibility(View.GONE);
        refreshButton.setVisibility(View.GONE);
        searchingLayout.setVisibility(View.VISIBLE);
        searchingTextView.setText(R.string.search_in_progress);
    }

    protected void onRecommendationsAcquired(Recommendation[] recommendations) {
        this.lastRecommendations = recommendations;
        viewPager.setVisibility(View.VISIBLE);
        pagerStrip.setVisibility(View.VISIBLE);
        if (recommendations != null) {
            swipeRouteDetailFragmentsAdapter.clearFragments();
            for (Recommendation recommendation : recommendations) {
                RouteDetailFragment fragment = RouteDetailFragment.newInstance(lastLocation, lastAddress, recommendation);
                swipeRouteDetailFragmentsAdapter.addFragment(fragment);
            }
            viewPager.setCurrentItem(0);
        }
    }

    protected void onSearchRecommendationsFinally() {
        searchingLayout.setVisibility(View.GONE);
        refreshButton.setVisibility(View.VISIBLE);
    }

    protected void onPrepareForSearch() {
        viewPager.setVisibility(View.GONE);
        pagerStrip.setVisibility(View.GONE);
        searchingLayout.setVisibility(View.GONE);
        refreshButton.setVisibility(View.VISIBLE);
    }

    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    protected Location getLastLocation() {
        return lastLocation;
    }

    protected String getLastAddress() {
        return lastAddress;
    }

    protected Recommendation[] getLastRecommendations() {
        return lastRecommendations;
    }

    protected LocationRequester getLocationRequester() {
        return locationRequester;
    }

    private class SwipeRouteDetailFragmentsAdapter extends FragmentPagerAdapter {

        private List<RouteDetailFragment> fragments;

        private SwipeRouteDetailFragmentsAdapter() {
            super(getChildFragmentManager());
            fragments = new ArrayList<>();
        }

        private void clearFragments() {
            fragments.clear();
            notifyDataSetChanged();
        }

        private void addFragment(RouteDetailFragment fragment) {
            fragments.add(fragment);
            notifyDataSetChanged();
        }

        @Override
        public RouteDetailFragment getItem(int position) {
            if (!fragments.isEmpty() && position >= 0) return fragments.get(position);
            else return null;
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (!fragments.isEmpty() && position >= 0)
                return getString(R.string.route_format, position + 1);
            else return null;
        }
    }
}