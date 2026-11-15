package se.kodhast.fonder.ui.settings

import androidx.annotation.StringRes
import se.kodhast.fonder.R
import se.kodhast.fonder.domain.model.AccountType

/** "ISK/KF"/"Depå/AF" — en gång för Inställningars val och Besluts rubrikrad (regel 4). */
@StringRes
fun AccountType.labelRes(): Int = when (this) {
    AccountType.ISK_KF -> R.string.account_type_isk_kf
    AccountType.DEPA_AF -> R.string.account_type_depa_af
}
