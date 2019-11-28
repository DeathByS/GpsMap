package com.example.gpsmap

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    lateinit var marker : Marker

    // 1

    // PolyLine 옵션
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: MyLocationCallBack

    // PolyLine 옵션 1
    private val polylineOptions = PolylineOptions().width(5f).color(Color.BLUE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //화면이 꺼지지 않게 하기
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //세모 모드로 화면 고정
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationInit() // 2
    }

    //위치 정보를 얻기 위한 각종 초기화 /3
    private fun locationInit() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        locationCallback = MyLocationCallBack()

        locationRequest = LocationRequest()
        //GPS우선
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        //업데이트 인터벌
        //위치 정보가 없을땐 업데이트 x
        //상황에 따라 짧아질 수 있음,정확성 x
        //타 앱에서 짧은 인터벌로 위치 정보를 요청하면 짧아질수도 있음
        locationRequest.interval = 10000
        //정확함. 이것보다 짧은 업데이트는 안함
        locationRequest.fastestInterval = 5000
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap // 2

        //서울에 마커 추가하고 카메라 이동
        val seoul = LatLng(37.56, 126.97)
        this.marker =  mMap.addMarker(MarkerOptions().position(seoul).title("현재 위치 : 서울"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(seoul))

/*
        //시드니에 마커 추가하고 카메라 이동
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
*/

    }

    override fun onResume() {
        super.onResume()
        addlocationListener() // 4

        //권한 요청 9
        permissionCheck(cancel = {
            //위치 정보가 필요한 이유 다이얼로그 표시 /10
            showPermissionInfoDialog()
        }, ok = {
            //현재 위치를 주기적으로 요청 (권한이 필요한 부분) /11
            addlocationListener()
        })
    }

    // 5
    private fun addlocationListener() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    //실행 중 권한 요청 메서드 작성

    private val REQUEST_ACCESS_FINE_LOCATION = 1000

    private fun permissionCheck(cancel: () -> Unit, ok: () -> Unit) {    // 1
        //위치 권한이 있는지 검사
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //권한이 허용되지 않음
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                //이전에 권한을 한 번 거부한 적이 있는 경우 실행될 함수
                cancel()    //2
            } else {
                //권한요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ACCESS_FINE_LOCATION
                )
            }
        } else {
            //권한을 수락했을 시 실행될 함수
            ok() // 3
        }
    }

    //권한이 필요한 이유를 설명하는 다이얼로그 표시 메서드 추가
    private fun showPermissionInfoDialog() {
        alert("현 위치 정보를 얻으려면 위치 권한이 필요하다", "권한이 필요한 이유") {
            //4
            yesButton {
                // 5
                //권한 요청
                ActivityCompat.requestPermissions(
                    this@MapsActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ACCESS_FINE_LOCATION
                )
            }
            noButton { }   // 7
        }.show()    // 8
    }

    //권한 요청 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ACCESS_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty()
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    //권한 허용됨. 1
                    addlocationListener()
                } else {
                    //권한 거부 2
                    toast("권한 거부")
                }
                return
            }
        }
    }

    //위치 정보 요청 삭제
    override fun onPause() {
        super.onPause()
        // 1
        removeLocationListener()
    }

    private fun removeLocationListener() {
        // 현 위치 요청 삭제 2
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    //위도,경도를 로그에 표시
    inner class MyLocationCallBack : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)

            val location = locationResult?.lastLocation

            var markerOption : MarkerOptions = MarkerOptions()

            var latLng = LatLng(location!!.latitude, location!!.longitude)

            marker.position = latLng

            //markerOption.position()

            location.run {
                // 14 level로 확대하며 전체 위치로 카메라 이동
                val latLng = LatLng(latitude, longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))

                Log.d("MapsActivity", "위도: $latitude, 경도: $longitude")  // 1

                //PolyLine에 좌표 추가 2
                //polylineOptions.add(latLng)

                //선 그리기
                //mMap.addPolyline(polylineOptions)
            }
        }
    }

}