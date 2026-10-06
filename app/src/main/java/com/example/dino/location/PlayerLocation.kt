package com.example.dino.location

import android.Manifest
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dino.data.GeoPoint
import kotlinx.coroutines.delay
import com.example.dino.data.distance

data class PlayerFix(val point: GeoPoint, val accuracy: Float, val elapsedNanos: Long) {
    fun isUsable() = accuracy <= 100f &&
        SystemClock.elapsedRealtimeNanos() - elapsedNanos in 0..60_000_000_000L
}
data class LocationState(val fix: PlayerFix? = null, val message: String = "Aguardando localização…",val walking:Boolean=false)

@Composable
fun rememberPlayerLocation(enabled: Boolean, permissionVersion: Int): LocationState {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var state by remember { mutableStateOf(LocationState()) }
    // Refresh freshness status even when the provider stops sending updates.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(enabled) { while (enabled) { delay(1000); tick++ } }
    DisposableEffect(enabled, permissionVersion, owner) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var listening = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val fix = PlayerFix(GeoPoint(location.latitude, location.longitude), location.accuracy, location.elapsedRealtimeNanos)
                val previous=state.fix
                if(previous!=null && fix.elapsedNanos<=previous.elapsedNanos) return
                // A coarse network update must not replace a recent, precise GPS fix.
                if(previous?.isUsable()==true && fix.accuracy>maxOf(35f,previous.accuracy*2) &&
                    fix.elapsedNanos-previous.elapsedNanos<30_000_000_000L) return
                val walking=fix.isUsable() && if(location.hasSpeed()) location.speed in .35f..4.5f
                    else previous?.isUsable()==true && distance(previous.point,fix.point)>
                        maxOf(2.5f,minOf(previous.accuracy,fix.accuracy)*.35f)
                state = LocationState(fix, if (fix.isUsable()) "GPS ativo · precisão de ${fix.accuracy.toInt()} m" else "Buscando um sinal de GPS mais preciso…",walking)
            }
            override fun onProviderDisabled(provider: String) { state = state.copy(message = "Ative a localização do celular",walking=false) }
        }
        fun stop() { if (listening) manager.removeUpdates(listener); listening = false }
        fun start() {
            if (!enabled || listening) return
            val allowed = ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
            if (!allowed) { state = LocationState(message = "Permita a localização precisa para explorar com GPS"); return }
            try {
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                    .filter { manager.isProviderEnabled(it) }
                if (providers.isEmpty()) { state = LocationState(message = "Ative a localização do celular"); return }
                state = state.copy(message = "Buscando sua localização…")
                providers.forEach { provider ->
                    manager.requestLocationUpdates(provider, 2000L, 0f, listener, Looper.getMainLooper())
                }
                listening = true
                providers.mapNotNull {manager.getLastKnownLocation(it)}.filter {
                    SystemClock.elapsedRealtimeNanos()-it.elapsedRealtimeNanos in 0..30_000_000_000L
                }.minByOrNull {it.accuracy}?.let {listener.onLocationChanged(it)}
            } catch (_: SecurityException) { state = LocationState(message = "Permissão de localização necessária") }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) start()
            if (event == Lifecycle.Event.ON_PAUSE) stop()
        }
        owner.lifecycle.addObserver(observer)
        val providerReceiver=object:BroadcastReceiver() {
            override fun onReceive(context:Context?,intent:Intent?) {
                stop()
                if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
            }
        }
        ContextCompat.registerReceiver(context,providerReceiver,IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),ContextCompat.RECEIVER_NOT_EXPORTED)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose { owner.lifecycle.removeObserver(observer);context.unregisterReceiver(providerReceiver); stop() }
    }
    val freshMotion=tick>=0 && enabled && state.fix?.isUsable()==true &&
        SystemClock.elapsedRealtimeNanos()-state.fix!!.elapsedNanos<4_500_000_000L
    return if(enabled && state.fix!=null && !state.fix!!.isUsable())
        state.copy(message="Buscando um sinal de GPS recente e preciso…",walking=false)
    else state.copy(walking=state.walking && freshMotion)
}
