package se.kodhast.fonder.ui.navigation

import androidx.annotation.DrawableRes
import se.kodhast.fonder.R

/**
 * Flikarna i den flytande flikraden (NAV-1, NAV-7), i ordning, med sin nyckel ([TabKey]) – roten i
 * flikens egen bakåtstack ([AppBackStack]). Beslut (NAV-9) är en egen flik, och Facit en skärm i
 * stacken under den ([FacitKey]).
 */
enum class Screen(val key: TabKey, @param:DrawableRes val icon: Int, val labelRes: Int) {
    Oversikt(OversiktKey, R.drawable.ic_overview, R.string.nav_oversikt),
    Innehav(InnehavKey, R.drawable.ic_holdings, R.string.nav_innehav),
    Beslut(BeslutKey, R.drawable.ic_decisions, R.string.nav_beslut),
    Handelser(HandelserKey, R.drawable.ic_receipt, R.string.nav_handelser);

    companion object {
        val START = Oversikt

        /** Fliken vars rot [key] är, eller null för en skärm djupare i stacken. */
        fun forKey(key: AppKey?): Screen? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Rubrik för en skärm djupare i stacken (flikarnas rubriker bor i [Screen.labelRes]). Null för en
 * flik — den har sin stora rubrik i stället för en topprad — och för en ramskärm ([FrameKey]), som
 * ritar sin rubrik själv.
 */
fun titleResFor(key: AppKey): Int? = when (key) {
    is TabKey, is FrameKey -> null
    SettingsKey -> R.string.nav_settings
    ImportHoldingsKey -> R.string.import_title
    ImportOrdersKey -> R.string.import_orders_title
    FacitKey -> R.string.facit_title
    is SwitchWatchKey -> R.string.switch_watch_title
}
