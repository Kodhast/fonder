package se.kodhast.fonder.ui.transaktioner

import androidx.annotation.StringRes
import se.kodhast.fonder.R
import se.kodhast.fonder.domain.model.TransactionType

/** "Köp"/"Sälj" — en gång för formuläret, transaktionslistan och båda importerna (regel 4). */
@StringRes
fun TransactionType.labelRes(): Int = when (this) {
    TransactionType.KOP -> R.string.transaktion_kop
    TransactionType.SALJ -> R.string.transaktion_salj
}
