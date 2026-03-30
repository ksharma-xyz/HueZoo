package xyz.ksharma.huezoo.ui.upgrade

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.huezoo.ui.util.safeLaunch
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.platform.billing.BillingClient
import xyz.ksharma.huezoo.platform.billing.PRODUCT_UNLIMITED
import xyz.ksharma.huezoo.platform.billing.PurchaseResult

data class UpgradeUiState(
    val priceLabel: String = "$2.99",
    val isPurchasing: Boolean = false,
    val isPaid: Boolean = false,
    val error: String? = null,
)

class UpgradeViewModel(
    private val billingClient: BillingClient,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpgradeUiState())
    val uiState: StateFlow<UpgradeUiState> = _uiState.asStateFlow()

    init {
        safeLaunch {
            val isPaid = settingsRepository.isPaid()
            val price = billingClient.queryPrice(PRODUCT_UNLIMITED)
            _uiState.value = _uiState.value.copy(
                isPaid = isPaid,
                priceLabel = price ?: "$2.99",
            )
        }
    }

    fun onPurchase() {
        if (_uiState.value.isPurchasing) return
        safeLaunch {
            _uiState.value = _uiState.value.copy(isPurchasing = true, error = null)
            when (val result = billingClient.purchase(PRODUCT_UNLIMITED)) {
                PurchaseResult.Success -> {
                    settingsRepository.setPaid(true)
                    _uiState.value = _uiState.value.copy(isPurchasing = false, isPaid = true)
                }
                PurchaseResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(isPurchasing = false)
                }
                is PurchaseResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isPurchasing = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun onRestorePurchases() {
        safeLaunch {
            val owned = billingClient.isOwned(PRODUCT_UNLIMITED)
            if (owned) {
                settingsRepository.setPaid(true)
                _uiState.value = _uiState.value.copy(isPaid = true)
            }
        }
    }
}
