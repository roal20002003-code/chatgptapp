package ir.spaceweather.app

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.spaceweather.app.data.Api
import ir.spaceweather.app.data.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(10 * 60 * 1000L) // هر ۱۰ دقیقه
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val errors = ArrayList<String>()
            val h = async { runCatching { Api.hourly() } }
            val a = async { runCatching { Api.alerts() } }
            val c = async { runCatching { Api.charts() } }
            val hr = h.await(); val ar = a.await(); val cr = c.await()
            if (hr.isFailure) errors += tr("پارامترهای لحظه‌ای: ${hr.exceptionOrNull()?.message}", "Live parameters: ${hr.exceptionOrNull()?.message}")
            if (ar.isFailure) errors += tr("اخطارها: ${ar.exceptionOrNull()?.message}", "Alerts: ${ar.exceptionOrNull()?.message}")
            if (cr.isFailure) errors += tr("نمودارها: ${cr.exceptionOrNull()?.message}", "Charts: ${cr.exceptionOrNull()?.message}")
            _state.update { s ->
                s.copy(
                    loading = false,
                    hourly = hr.getOrNull() ?: s.hourly,
                    alerts = ar.getOrNull() ?: s.alerts,
                    charts = cr.getOrNull() ?: s.charts,
                    errors = errors,
                )
            }
        }
    }
}
