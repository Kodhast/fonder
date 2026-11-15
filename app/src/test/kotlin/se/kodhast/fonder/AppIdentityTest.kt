package se.kodhast.fonder

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Appens identitet utåt efter bytet av kodpaket till `se.kodhast.fonder` (fonder #253): appens id
 * och startikonens komponent är desamma som före bytet, så att uppdateringen installeras över den
 * gamla appen (med dess data) och hemskärmens genväg ligger kvar.
 */
@RunWith(RobolectricTestRunner::class)
class AppIdentityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `appens id är detsamma som före paketbytet`() {
        assertEquals("se.partee71.fonder", BuildConfig.APPLICATION_ID)
        assertEquals("se.partee71.fonder", context.packageName)
    }

    @Test
    fun `startavsikten löser till aliaset med MainActivitys gamla namn`() {
        val launch = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))

        assertEquals("se.partee71.fonder", launch.component?.packageName)
        assertEquals("se.partee71.fonder.MainActivity", launch.component?.className)
    }

    @Test
    fun `aliaset är den enda startpunkten och öppnar MainActivity`() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)

        val launcher = context.packageManager.queryIntentActivities(query, 0).single().activityInfo

        assertEquals("se.partee71.fonder.MainActivity", launcher.name)
        assertEquals(MainActivity::class.java.name, launcher.targetActivity)
    }
}
