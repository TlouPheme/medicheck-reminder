package com.example.medicheckreminder

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.medicheckreminder.databinding.ActivityMainBinding
import com.example.medicheckreminder.ui.motion.NavMotion

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var statusBarInset = 0
    private var navigationBarInset = 0

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setOnExitAnimationListener { splashScreen ->
            splashScreen.remove()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestNotificationPermission()

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        wireFragmentTransitions(navHostFragment)

        // Setup Bottom Nav
        binding.bottomNav.setupWithNavController(navController)

        // Setup ActionBar with titles and back button
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.medicationsListFragment,
                R.id.healthMeasurementsFragment,
                R.id.historyFragment,
                R.id.settingsFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        applyWindowInsets()

        binding.fabAddMedication.setOnClickListener {
            navController.navigate(R.id.addEditMedicationFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateChrome(destination.id)
        }
        updateChrome(navController.currentDestination?.id)
    }

    /**
     * Keep the toolbar and page content below the camera cutout, and keep the
     * bottom bar above the system navigation area.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val enabled = (application as MediCheckApp).container.settingsRepository
            .snapshot().notificationsEnabled
        if (!enabled) return
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            statusBarInset = bars.top
            navigationBarInset = bars.bottom
            binding.bottomNav.updatePadding(bottom = navigationBarInset)
            updateChrome(navController.currentDestination?.id)
            insets
        }
    }

    private fun updateChrome(destinationId: Int?) {
        val showAppBar = destinationId !in NO_APP_BAR
        val showBottomNav = destinationId !in NO_BOTTOM_NAV
        val showFab = destinationId == R.id.homeFragment ||
            destinationId == R.id.medicationsListFragment

        if (showAppBar) {
            binding.appBar.visibility = View.VISIBLE
            binding.appBar.updatePadding(top = statusBarInset)
            supportActionBar?.show()
        } else {
            supportActionBar?.hide()
            binding.appBar.visibility = View.GONE
        }
        binding.navHostFragment.updatePadding(
            top = if (showAppBar) 0 else statusBarInset,
            bottom = if (showBottomNav) 0 else navigationBarInset
        )
        binding.bottomNav.visibility = if (showBottomNav) View.VISIBLE else View.GONE
        binding.fabAddMedication.visibility = if (showFab) View.VISIBLE else View.GONE

        val fabParams = binding.fabAddMedication.layoutParams as ViewGroup.MarginLayoutParams
        fabParams.bottomMargin = if (showBottomNav) {
            resources.getDimensionPixelSize(R.dimen.home_bottom_inset)
        } else {
            navigationBarInset + resources.getDimensionPixelSize(R.dimen.space_m)
        }
        binding.fabAddMedication.layoutParams = fabParams
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * Set Material enter/return/exit/reenter on every graph fragment before its view
     * is created. [NavController.navigateUp] then plays [Fragment.returnTransition]
     * and the previous destination's [Fragment.reenterTransition] — the reverse of
     * the forward motion — instead of a hard cut.
     */
    private fun wireFragmentTransitions(navHostFragment: NavHostFragment) {
        val childFm = navHostFragment.childFragmentManager
        childFm.fragments.forEach { NavMotion.apply(it) }
        childFm.addFragmentOnAttachListener { _, fragment ->
            NavMotion.apply(fragment)
        }
    }

    private companion object {
        val NO_APP_BAR = setOf(
            R.id.splashFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.forgotPasswordFragment
        )
        val NO_BOTTOM_NAV = NO_APP_BAR + R.id.addEditMedicationFragment
    }
}
