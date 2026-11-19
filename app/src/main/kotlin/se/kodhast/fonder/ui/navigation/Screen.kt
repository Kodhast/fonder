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
    }
}
