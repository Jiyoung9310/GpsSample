package com.android.yaho.screen

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import com.android.yaho.BuildConfig
import com.android.yaho.R
import com.android.yaho.base.BindingActivity
import com.android.yaho.databinding.ActivityClimbingPathBinding
import com.android.yaho.dp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay

class ClimbingPathActivity : BindingActivity<ActivityClimbingPathBinding>(ActivityClimbingPathBinding::inflate),
    OnMapReadyCallback {

    companion object {
        const val KEY_PATH_LIST = "KEY_PATH_LIST"
        const val KEY_SECTION_MARK_LIST = "KEY_SECTION_MARK_LIST"
    }

    private var naverMap: NaverMap? = null
    private val pathOverlay: PathOverlay by lazy { PathOverlay() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as MapFragment?
            ?: MapFragment.newInstance(
                NaverMapOptions()
                    .backgroundResource(NaverMap.DEFAULT_BACKGROUND_DRWABLE_DARK)
                    .mapType(NaverMap.MapType.Terrain)
                    .enabledLayerGroups(NaverMap.LAYER_GROUP_MOUNTAIN)
            ).also {
                supportFragmentManager.beginTransaction().add(R.id.mapFragment, it).commit()
            }

        mapFragment.getMapAsync(this)

        initView()
    }

    private fun initView() {
        loadAdmob()
        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.apply {
            isLiteModeEnabled = true
            setBackgroundResource(NaverMap.DEFAULT_BACKGROUND_DRWABLE_DARK)
            mapType = NaverMap.MapType.Terrain
            setLayerGroupEnabled(NaverMap.LAYER_GROUP_MOUNTAIN, true)
            minZoom = 10.0
            uiSettings.apply {
                isZoomControlEnabled = false
                isCompassEnabled = false
                isScaleBarEnabled = false
            }
            setContentPadding(0, 0, 0, 0)
        }

        pathOverlay.also {
            it.width = resources.getDimensionPixelSize(R.dimen.path_overlay_width)
            it.outlineWidth = resources.getDimensionPixelSize(R.dimen.path_overlay_outline_width)
            it.color = Color.BLACK
        }

        intent.extras?.getParcelableArrayList<com.android.yaho.local.db.LatLng>(KEY_PATH_LIST)?.let {
            drawPath(it.map { LatLng(it.latitude, it.longitude) })
        }

        intent.extras?.getParcelableArrayList<com.android.yaho.local.db.LatLng>(KEY_SECTION_MARK_LIST)?.let {
            drawMarker(it.map { LatLng(it.latitude, it.longitude) })
        }
    }

    private fun drawPath(list: List<LatLng>) {
        naverMap?.let {
            if (list.size >= 2) {
                pathOverlay.apply {
                    coords = list
                    map = naverMap
                }
            }
        }
    }

    private fun drawMarker(list: List<LatLng>) {
        naverMap?.let {
            list.forEach {
                Marker().apply {
                    anchor = PointF(0.1f, 0.7f)
                    position = LatLng(it.latitude, it.longitude)
                    icon = OverlayImage.fromResource(R.drawable.img_marker_dot)
                    isForceShowIcon = true
                    map = naverMap
                }
            }

            val cameraUpdate = CameraUpdate.fitBounds(
                LatLngBounds.Builder().include(list).build(), 150
            )
            naverMap?.moveCamera(cameraUpdate)
        }
    }

    private fun loadAdmob() {
        MobileAds.initialize(this) { }

        val adView = AdView(this)
        binding.adContainer.addView(adView)

        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        var adWidthPixels = binding.adContainer.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()

        adView.adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        adView.adUnitId = getString(if(BuildConfig.DEBUG) R.string.admob_banner_unit_id_test else R.string.admob_banner_unit_id)


        // Create an ad request.
        val adRequest = AdRequest.Builder().build()

        // Start loading the ad in the background.
        adView.loadAd(adRequest)
    }
}